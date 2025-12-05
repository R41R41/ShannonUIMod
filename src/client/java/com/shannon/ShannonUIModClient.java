package com.shannon;

import com.shannon.network.packet.DetailedLogsState;
import com.shannon.network.packet.DetailedLogsStatePacket;

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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.shannon.network.packet.InventoryStatePacket;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.ConstantSkillsStatePacket;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.PlayerStatusStatePacket;
import com.shannon.network.packet.PlayerStatusState;
import com.shannon.network.packet.ChatStatePacket;
import com.shannon.network.packet.ChatState;
import com.shannon.network.packet.ReactionSettingsState;
import com.shannon.network.packet.ReactionSettingsStatePacket;

public class ShannonUIModClient implements ClientModInitializer {

    private static KeyBinding toggleDisplayUIKey;
    private static KeyBinding toggleHUDAndScreenUIKey;
    private static KeyBinding tabSwitchNextKey;
    private TaskTreeState taskTreeState;
    private InventoryState inventoryState;
    private ConstantSkillsState constantSkillsState;
    private PlayerStatusState playerStatusState;
    private ChatState chatState;
    private DetailedLogsState detailedLogsState;
    private ReactionSettingsState reactionSettingsState;
    private com.shannon.network.packet.LogToggleState logToggleState = new com.shannon.network.packet.LogToggleState();
    private UIRenderer.UIState uiState = new UIRenderer.UIState();
    private static ShannonUIModClient INSTANCE;
    private int[] tabScrollOffsets = new int[6]; // 6タブ分
    private int selectedTab = 0;

    public enum UIMode {
        HIDDEN,
        HUD,
        SCREEN
    }

    private static UIMode uiMode = UIMode.HIDDEN;

    public ShannonUIModClient() {
        INSTANCE = this;
    }

    @Override
    public void onInitializeClient() {
        System.out.println("ShannonUIModClient onInitializeClient");

        ClientPlayNetworking.registerGlobalReceiver(TaskTreeStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                TaskTreeState state = payload.state();
                taskTreeState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(InventoryStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                InventoryState state = payload.state();
                inventoryState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ConstantSkillsStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ConstantSkillsState state = payload.state();
                constantSkillsState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ReactionSettingsStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ReactionSettingsState state = payload.state();
                reactionSettingsState = state;
                System.out.println("[ShannonUI] Received ReactionSettings via packet: " +
                        (state.reactions != null ? state.reactions.size() : 0) + " reactions");
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerStatusStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                PlayerStatusState state = payload.state();
                playerStatusState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ChatStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                ChatState state = payload.state();
                chatState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DetailedLogsStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                DetailedLogsState state = payload.state();
                detailedLogsState = state;
                System.out.println("DetailedLogsState received: " + state);
            });
        });

        // キーバインドの登録
        toggleDisplayUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.toggleDisplayUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.shannonuimod"));
        toggleHUDAndScreenUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.toggleHUDAndScreenUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.shannonuimod"));
        tabSwitchNextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.tabSwitchNext",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.shannonuimod"));

        MinecraftClient.getInstance().execute(() -> {
            // キーイベントの監視
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                // Uキー: HUD型UIの表示/非表示 or Screen→HUD
                if (toggleDisplayUIKey.wasPressed()) {
                    updateUIMode(true, client);
                }
                // Iキー: Screen型UIの表示/非表示 or HUD→Screen
                if (toggleHUDAndScreenUIKey.wasPressed()) {
                    updateUIMode(false, client);
                }
            });
        });

        HudRenderCallback.EVENT.register((DrawContext context, RenderTickCounter tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null)
                return;

            // UI操作（タブ切り替えなど）
            UIRenderer.handleInput(mc, uiState, uiState.lastUiHeight, tabSwitchNextKey);

            int textureSize = 6;
            int windowWidth = (13 * textureSize + 2);
            int windowHeight = textureSize * 2 + 2;

            if (uiMode == UIMode.HUD) {
                UIRenderer.updatePanelLayout(mc, uiState, windowWidth, windowHeight, textureSize);
                UIRenderer.renderUI(context, mc, uiState.lastPanelX, uiState.lastPanelY, uiState.lastUiWidth,
                        uiState.lastUiHeight, uiState, taskTreeState, inventoryState, constantSkillsState,
                        chatState, uiState.lastUiHeight, uiMode);
            }
            PlayerStatusRenderer.renderPlayerStatus(context, mc, windowWidth, windowHeight, textureSize,
                    playerStatusState);
        });
    }

    public static void updateUIMode(boolean isDisplayUIKeyPressed, MinecraftClient client) {
        if (isDisplayUIKeyPressed) {
            switch (uiMode) {
                case HIDDEN:
                    uiMode = UIMode.HUD;
                    if (client.currentScreen instanceof ShannonUIScreen) {
                        client.setScreen(null);
                    }
                    // HUD型UIのスクロール位置を復元
                    if (INSTANCE != null) {
                        INSTANCE.uiState.scrollOffset = getTabScrollOffset(INSTANCE.uiState.selectedTab);
                    }
                    break;
                case HUD:
                    uiMode = UIMode.HIDDEN;
                    break;
                case SCREEN:
                    if (client.currentScreen instanceof ShannonUIScreen) {
                        client.setScreen(null);
                    }
                    uiMode = UIMode.HIDDEN;
                    break;
            }
        } else {
            switch (uiMode) {
                case HIDDEN:
                    break;
                case HUD:
                    uiMode = UIMode.SCREEN;
                    client.setScreen(new ShannonUIScreen());
                    break;
                case SCREEN:
                    if (client.currentScreen instanceof ShannonUIScreen) {
                        client.setScreen(null);
                    }
                    uiMode = UIMode.HUD;
                    if (INSTANCE != null) {
                        INSTANCE.uiState.scrollOffset = getTabScrollOffset(INSTANCE.uiState.selectedTab);
                    }
                    break;
            }
        }
    }

    public static TaskTreeState getTaskTreeState() {
        return INSTANCE != null ? INSTANCE.taskTreeState : null;
    }

    public static InventoryState getInventoryState() {
        return INSTANCE != null ? INSTANCE.inventoryState : null;
    }

    public static ConstantSkillsState getConstantSkillsState() {
        return INSTANCE != null ? INSTANCE.constantSkillsState : null;
    }

    public static int getTabScrollOffset(int tab) {
        if (INSTANCE == null || tab < 0 || tab >= INSTANCE.tabScrollOffsets.length) {
            return 0;
        }
        return INSTANCE.tabScrollOffsets[tab];
    }

    public static void setTabScrollOffset(int tab, int offset) {
        if (INSTANCE != null && tab >= 0 && tab < INSTANCE.tabScrollOffsets.length) {
            INSTANCE.tabScrollOffsets[tab] = offset;
        }
    }

    public static KeyBinding getToggleDisplayUIKey() {
        return toggleDisplayUIKey;
    }

    public static KeyBinding getToggleHUDAndScreenUIKey() {
        return toggleHUDAndScreenUIKey;
    }

    public static KeyBinding getTabSwitchNextKey() {
        return tabSwitchNextKey;
    }

    public static int getSelectedTab() {
        return INSTANCE != null ? INSTANCE.selectedTab : 0;
    }

    public static void setSelectedTab(int tab) {
        if (INSTANCE != null)
            INSTANCE.selectedTab = tab;
    }

    public static ShannonUIModClient getInstance() {
        return INSTANCE;
    }

    public static PlayerStatusState getPlayerStatusState() {
        return INSTANCE != null ? INSTANCE.playerStatusState : null;
    }

    public static ChatState getChatState() {
        return INSTANCE != null ? INSTANCE.chatState : null;
    }

    public static DetailedLogsState getDetailedLogsState() {
        return INSTANCE != null ? INSTANCE.detailedLogsState : null;
    }

    public static com.shannon.network.packet.LogToggleState getLogToggleState() {
        return INSTANCE != null ? INSTANCE.logToggleState : null;
    }

    public static ReactionSettingsState getReactionSettingsState() {
        // パケット経由で受信したデータを返す
        return INSTANCE != null ? INSTANCE.reactionSettingsState : null;
    }
}