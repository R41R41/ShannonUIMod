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

public class ShannonUIModClient implements ClientModInitializer {

    private static KeyBinding toggleUIKey;
    private boolean isUIVisible = false;
    private TaskTreeState taskTreeState;
    private int scrollOffset = 0;
    private int selectedTab = 0; // 0:タスクツリー, 1:インベントリ, 2:常時スキル
    private UIRenderer.UIState uiState = new UIRenderer.UIState();
    private GLFWScrollCallbackI originalScrollCallback = null;

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

        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null)
                return;

            // UI操作（タブ切り替えなど）
            UIRenderer.handleInput(mc, uiState, isUIVisible, uiState.lastUiHeight);

            // Uキーが押されたらUIの表示状態を切り替える
            if (toggleUIKey.wasPressed()) {
                isUIVisible = !isUIVisible;
                long windowHandle = mc.getWindow().getHandle();
                originalScrollCallback = UIRenderer.handleToggleUIVisible(
                        isUIVisible,
                        originalScrollCallback,
                        windowHandle,
                        uiState,
                        uiState.lastUiHeight,
                        () -> UIRenderer.isMouseOverPanel(mc, isUIVisible, uiState.lastPanelX, uiState.lastPanelY,
                                uiState.lastUiWidth, uiState.lastUiHeight));
            }

            // Tabキーでタブ切り替え
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_TAB) && isUIVisible) {
                selectedTab = (selectedTab + 1) % 3;
            }

            int textureSize = 6;
            int windowWidth = (13 * textureSize + 2);
            int windowHeight = textureSize * 2 + 2;

            if (isUIVisible) {
                UIRenderer.updatePanelLayout(mc, uiState, windowWidth, windowHeight, textureSize);
                context.fill(uiState.lastPanelX, uiState.lastPanelY, uiState.lastPanelX + uiState.lastUiWidth,
                        uiState.lastPanelY + uiState.lastUiHeight, 0x96000000); // 半透明の黒い枠
                UIRenderer.renderTabbedUI(context, mc, uiState.lastPanelX, uiState.lastPanelY, uiState.lastUiWidth,
                        uiState.lastUiHeight, uiState, taskTreeState, uiState.lastUiHeight);
            }
            PlayerStatusRenderer.renderPlayerStatus(context, mc, windowWidth, windowHeight, textureSize);
        });
    }
}