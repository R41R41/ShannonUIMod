package com.shannon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.shannon.network.packet.TaskTreeStatePacket;
import com.shannon.network.packet.TaskTreeState;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ShannonUIModClient implements ClientModInitializer {

    private static KeyBinding toggleUIKey;
    private static KeyBinding tabSwitchNextKey;
    private static KeyBinding tabSwitchPrevKey;
    private boolean isUIVisible = false;
    private TaskTreeState taskTreeState;
    private int scrollOffset = 0;
    private int selectedTab = 0; // 0:タスクツリー, 1:インベントリ, 2:常時スキル
    private UIRenderer.UIState uiState = new UIRenderer.UIState();
    private GLFWScrollCallbackI originalScrollCallback = null;
    private boolean scrollCallbackSet = false;

    @Override
    public void onInitializeClient() {
        System.out.println("ShannonUIModClient onInitializeClient");

        ClientPlayNetworking.registerGlobalReceiver(TaskTreeStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                TaskTreeState state = payload.state();
                taskTreeState = state;
                System.out.println("Received task tree state: " + state);
            });
        });

        // キーバインドの登録
        toggleUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.toggleUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.shannonuimod"));
        tabSwitchNextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.tabSwitchNext",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.shannonuimod"));
        tabSwitchPrevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.tabSwitchPrev",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.shannonuimod"));

        MinecraftClient.getInstance().execute(() -> {
            long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
            GLFWScrollCallbackI[] originalCallback = new GLFWScrollCallbackI[1];
            boolean[] scrollCallbackSet = { false };

            // キーイベントの監視
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (toggleUIKey.wasPressed()) {
                    isUIVisible = !isUIVisible;
                    if (isUIVisible) {
                        if (!scrollCallbackSet[0]) {
                            originalCallback[0] = GLFW.glfwSetScrollCallback(windowHandle,
                                    (handle, xoffset, yoffset) -> {
                                        System.out.println("[ShannonUIModClient] GLFW scroll callback: xoffset="
                                                + xoffset + ", yoffset=" + yoffset);
                                        if (UIRenderer.isMouseOverPanel(client, isUIVisible, uiState.lastPanelX,
                                                uiState.lastPanelY,
                                                uiState.lastUiWidth, uiState.lastUiHeight)) {
                                            System.out.println("[ShannonUIModClient] isMouseOverPanel: true");
                                            UIRenderer.handleScroll(yoffset, uiState, uiState.lastUiHeight);
                                        } else {
                                            System.out.println("[ShannonUIModClient] isMouseOverPanel: false");
                                            if (originalCallback[0] != null) {
                                                originalCallback[0].invoke(handle, xoffset, yoffset);
                                            }
                                        }
                                    });
                            scrollCallbackSet[0] = true;
                        }
                    } else {
                        if (scrollCallbackSet[0] && originalCallback[0] != null) {
                            GLFW.glfwSetScrollCallback(windowHandle, originalCallback[0]);
                            scrollCallbackSet[0] = false;
                        }
                    }
                }
            });
        });

        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null)
                return;

            // UI操作（タブ切り替えなど）
            UIRenderer.handleInput(mc, uiState, isUIVisible, uiState.lastUiHeight, tabSwitchNextKey, tabSwitchPrevKey);

            // Tabキーでタブ切り替え
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_TAB) && isUIVisible) {
                selectedTab = (selectedTab + 1) % 3;
            }

            int textureSize = 6;
            int windowWidth = (13 * textureSize + 2);
            int windowHeight = textureSize * 2 + 2;

            if (isUIVisible) {
                UIRenderer.updatePanelLayout(mc, uiState, windowWidth, windowHeight, textureSize);
                UIRenderer.renderUI(context, mc, uiState.lastPanelX, uiState.lastPanelY, uiState.lastUiWidth,
                        uiState.lastUiHeight, uiState, taskTreeState, uiState.lastUiHeight);
            }
            PlayerStatusRenderer.renderPlayerStatus(context, mc, windowWidth, windowHeight, textureSize);
        });
    }
}