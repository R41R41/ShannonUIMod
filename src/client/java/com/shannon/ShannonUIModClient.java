package com.shannon;

import com.shannon.http.ClientHttpServerManager;
import com.shannon.network.packet.DetailedLogsState;
import com.shannon.network.packet.DetailedLogsStatePacket;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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
import com.shannon.network.packet.ScreenshotRequestPacket;
import com.shannon.network.packet.ScreenshotResultPacket;
import com.shannon.network.packet.TaskListStatePacket;
import com.shannon.network.packet.AdvancementsState;
import com.shannon.network.packet.AdvancementsStatePacket;
import com.shannon.network.packet.RequestAdvancementsPacket;
import com.shannon.network.packet.VoiceModeTogglePacket;
import com.shannon.network.packet.VoicePttPacket;
import com.shannon.util.ScreenshotUtil;
import com.shannon.http.endpoints.ScreenshotEndpoint;
import com.shannon.state.StateManager;

public class ShannonUIModClient implements ClientModInitializer {

    private static KeyBinding toggleDisplayUIKey;
    private static KeyBinding toggleHUDAndScreenUIKey;
    private static KeyBinding tabSwitchNextKey;
    private static KeyBinding voiceModeToggleKey;
    private static KeyBinding voicePttKey;
    private static long lastVoiceModeToggleMs = 0;
    private static final long KEY_COOLDOWN_MS = 1000;
    private static boolean pttActive = false;
    private TaskTreeState taskTreeState;
    private TaskListStatePacket.TaskListState taskListState;
    private InventoryState inventoryState;
    private ConstantSkillsState constantSkillsState;
    private PlayerStatusState playerStatusState;
    private ChatState chatState;
    private DetailedLogsState detailedLogsState;
    private ReactionSettingsState reactionSettingsState;
    private AdvancementsState advancementsState;
    private boolean advancementsRequested = false; // タブ表示時のリクエスト制御
    private com.shannon.network.packet.LogToggleState logToggleState = new com.shannon.network.packet.LogToggleState();
    private UIRenderer.UIState uiState = new UIRenderer.UIState();
    private static ShannonUIModClient INSTANCE;
    private int[] tabScrollOffsets = new int[7]; // 7タブ分
    private int selectedTab = 0;
    private static String selectedTaskId = null;
    // 通知バッジ
    private static volatile int unreadChatCount = 0;
    private static volatile boolean hasNewErrorLog = false;

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

        // クライアントサイドHTTPサーバーを起動（スクリーンショット等）
        ClientHttpServerManager.startServer();

        ClientPlayNetworking.registerGlobalReceiver(TaskTreeStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                TaskTreeState state = payload.state();
                taskTreeState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(TaskListStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                TaskListStatePacket.TaskListState state = payload.state();
                taskListState = state;
                System.out.println("📥 Client received TaskListState: " +
                        (state != null && state.tasks != null ? state.tasks.size() : 0) + " tasks, emergencyTask=" +
                        (state != null && state.emergencyTask != null ? state.emergencyTask.goal : "null"));
                // 最初のタスクを選択（選択がない場合）
                if (selectedTaskId == null && state != null && state.tasks != null && !state.tasks.isEmpty()) {
                    selectedTaskId = state.tasks.get(0).id;
                }
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
                // 未読カウント更新（チャットタブを表示中でない場合のみ）
                if (state != null && state.messages != null) {
                    int newCount = state.messages.size();
                    int oldCount = (chatState != null && chatState.messages != null) ? chatState.messages.size() : 0;
                    if (newCount > oldCount && INSTANCE != null && INSTANCE.selectedTab != 3) {
                        unreadChatCount += newCount - oldCount;
                    }
                }
                chatState = state;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DetailedLogsStatePacket.PACKET_ID, (payload, context) -> {
            try {
                DetailedLogsState state = payload.state();
                context.client().execute(() -> {
                    try {
                        // エラーログ検出（デバッグタブ表示中でない場合のみ）
                        if (state != null && state.logs != null && (INSTANCE == null || INSTANCE.selectedTab != 4)) {
                            for (DetailedLogsState.LogEntry log : state.logs) {
                                if ("error".equals(log.level)) {
                                    hasNewErrorLog = true;
                                    break;
                                }
                            }
                        }
                        detailedLogsState = state;
                    } catch (Exception e) {
                        com.shannon.ShannonUIMod.LOGGER.warn("[DetailedLogs] Apply state failed: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                com.shannon.ShannonUIMod.LOGGER.warn("[DetailedLogs] Decode or handle failed: {}", e.getMessage());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(AdvancementsStatePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                AdvancementsState state = payload.state();
                advancementsState = state;
                System.out.println("[ShannonUI] AdvancementsState received: " +
                        state.playerName + " (" +
                        (state.categories != null ? state.categories.size() : 0) + " categories)");
            });
        });

        // スクリーンショットリクエストパケットハンドラ（ボット視点で撮影）
        ClientPlayNetworking.registerGlobalReceiver(ScreenshotRequestPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                System.out.println("[ShannonUI] Screenshot request received: " + payload.requestId() + " for bot: "
                        + payload.botName());

                // スクリーンショットオプションを設定
                ScreenshotEndpoint.ScreenshotOptions options = new ScreenshotEndpoint.ScreenshotOptions();
                options.width = payload.width();
                options.height = payload.height();

                // ボットの視点からスクリーンショットを撮影（フレーム待機あり）
                ScreenshotUtil.captureFromBotViewDelayed(
                        options,
                        payload.botName(),
                        payload.botX(),
                        payload.botY(),
                        payload.botZ(),
                        payload.botYaw(),
                        payload.botPitch(),
                        (result) -> {
                            // 結果をパケットで送信
                            ScreenshotResultPacket resultPacket = new ScreenshotResultPacket(
                                    payload.requestId(),
                                    result.success,
                                    result.base64Image != null ? result.base64Image : "",
                                    result.width,
                                    result.height,
                                    result.playerPosition != null ? result.playerPosition.x : 0,
                                    result.playerPosition != null ? result.playerPosition.y : 0,
                                    result.playerPosition != null ? result.playerPosition.z : 0,
                                    result.playerRotation != null ? result.playerRotation.yaw : 0,
                                    result.playerRotation != null ? result.playerRotation.pitch : 0,
                                    result.error != null ? result.error : "");

                            ClientPlayNetworking.send(resultPacket);
                            System.out.println("[ShannonUI] Screenshot result sent: " + result.success);
                        });
            });
        });

        // キーバインドの登録
        KeyBinding.Category shannonCategory = KeyBinding.Category.create(
                net.minecraft.util.Identifier.of("shannonuimod", "category"));
        toggleDisplayUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.toggleDisplayUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                shannonCategory));
        toggleHUDAndScreenUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.toggleHUDAndScreenUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                shannonCategory));
        tabSwitchNextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.tabSwitchNext",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                shannonCategory));
        voiceModeToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.voiceModeToggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                shannonCategory));
        voicePttKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shannonuimod.voicePtt",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                shannonCategory));

        // キーイベントの監視（execute外で登録）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Uキー: HUD型UIの表示/非表示 or Screen→HUD
            if (toggleDisplayUIKey.wasPressed()) {
                updateUIMode(true, client);
            }
            // Iキー: Screen型UIの表示/非表示 or HUD→Screen
            if (toggleHUDAndScreenUIKey.wasPressed()) {
                updateUIMode(false, client);
            }
            // Vキー: voice_mode トグル（Chat ↔ Minebot）
            if (voiceModeToggleKey.wasPressed()) {
                long now = System.currentTimeMillis();
                if (now - lastVoiceModeToggleMs >= KEY_COOLDOWN_MS) {
                    lastVoiceModeToggleMs = now;
                    ClientPlayNetworking.send(new VoiceModeTogglePacket());
                }
            }
            // Pキー: Push-to-Talk（押してる間ON、離したらOFF）
            boolean pttPressed = voicePttKey.isPressed();
            if (pttPressed && !pttActive) {
                pttActive = true;
                ClientPlayNetworking.send(new VoicePttPacket(true));
            } else if (!pttPressed && pttActive) {
                pttActive = false;
                ClientPlayNetworking.send(new VoicePttPacket(false));
            }

            // スクリーンショット処理（フレーム待機後の撮影）
            ScreenshotUtil.tick();
        });

        HudElementRegistry.addLast(net.minecraft.util.Identifier.of("shannonuimod", "hud"), (context, tickCounter) -> {
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
                    // 共有のタブ・スクロール位置をHUD用uiStateに同期
                    if (INSTANCE != null) {
                        INSTANCE.uiState.selectedTab = INSTANCE.selectedTab;
                        INSTANCE.uiState.scrollOffset = getTabScrollOffset(INSTANCE.selectedTab);
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
                    // 共有のタブ・スクロール位置をHUD用uiStateに同期
                    if (INSTANCE != null) {
                        INSTANCE.uiState.selectedTab = INSTANCE.selectedTab;
                        INSTANCE.uiState.scrollOffset = getTabScrollOffset(INSTANCE.selectedTab);
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

    public static TaskListStatePacket.TaskListState getTaskListState() {
        return INSTANCE != null ? INSTANCE.taskListState : null;
    }

    public static String getSelectedTaskId() {
        return selectedTaskId;
    }

    public static void setSelectedTaskId(String taskId) {
        selectedTaskId = taskId;
    }

    public static AdvancementsState getAdvancementsState() {
        return INSTANCE != null ? INSTANCE.advancementsState : null;
    }

    // === 通知バッジ ===
    public static int getUnreadChatCount() { return unreadChatCount; }
    public static void markChatRead() { unreadChatCount = 0; }
    public static boolean hasNewErrorLog() { return hasNewErrorLog; }
    public static void markErrorLogRead() { hasNewErrorLog = false; }

    /**
     * 進捗データをサーバーにリクエスト（タブ切り替え時に呼ぶ）
     */
    public static void requestAdvancements() {
        if (INSTANCE == null)
            return;
        try {
            ClientPlayNetworking.send(new RequestAdvancementsPacket(""));
            INSTANCE.advancementsRequested = true;
        } catch (Exception e) {
            System.err.println("[ShannonUI] Failed to request advancements: " + e.getMessage());
        }
    }

    /**
     * 進捗がリクエスト済みかどうか
     */
    public static boolean isAdvancementsRequested() {
        return INSTANCE != null && INSTANCE.advancementsRequested;
    }
}