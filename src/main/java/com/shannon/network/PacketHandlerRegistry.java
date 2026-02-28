package com.shannon.network;

import com.shannon.MyModServer;
import com.shannon.ShannonUIMod;
import com.shannon.client.BackendClient;
import com.shannon.client.request.ChatMessageRequest;
import com.shannon.client.request.SkillSwitchRequest;
import com.shannon.client.request.ThrowItemRequest;
import com.shannon.client.request.VoicePttRequest;
import com.shannon.config.ModConfig;
import com.shannon.error.ModErrorHandler;
import com.shannon.error.exceptions.PacketHandlingException;
import com.shannon.network.packet.*;
import com.shannon.http.endpoints.ServerScreenshotEndpoint;
import com.shannon.state.StateManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C2S（Client to Server）パケットハンドラの登録を一元管理
 * 重複コードを削減し、パケット処理を統一
 */
public class PacketHandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketHandlerRegistry.class);

    /**
     * 全てのC2Sパケットハンドラを登録
     */
    public static void registerC2SHandlers() {
        LOGGER.info("📨 Registering C2S packet handlers...");

        registerMessageHandler();
        registerInventoryClickHandler();
        registerSkillClickHandler();
        registerChatMessageHandler();
        registerReactionSettingUpdateHandler();
        registerReactionSettingsResetHandler();
        registerScreenshotResultHandler();
        registerTaskActionHandler();
        registerRequestAdvancementsHandler();
        registerVoiceModeToggleHandler();
        registerVoicePttHandler();

        LOGGER.info("✅ All C2S packet handlers registered");
    }

    /**
     * メッセージパケットハンドラ
     */
    private static void registerMessageHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                MessagePacket.PACKET_ID,
                (payload, context) -> {
                    try {
                        ServerPlayerEntity player = context.player();
                        MyModServer.sendMessageToClient(player, payload.message());

                        if (ModConfig.LOG_PACKETS) {
                            LOGGER.debug("MessagePacket received: {}", payload.message());
                        }
                    } catch (Exception e) {
                        ModErrorHandler.handle(
                                new PacketHandlingException("MessagePacket", "receive", e));
                    }
                });
    }

    /**
     * インベントリクリックパケットハンドラ
     */
    private static void registerInventoryClickHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                InventoryItemClickPacket.PACKET_ID,
                (payload, context) -> {
                    String itemName = payload.itemName();
                    context.server().execute(() -> {
                        try {
                            BackendClient.postJson(
                                    ModConfig.ENDPOINT_THROW_ITEM,
                                    new ThrowItemRequest(itemName));

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("InventoryItemClickPacket: {}", itemName);
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("InventoryItemClickPacket", e));
                        }
                    });
                });
    }

    /**
     * スキルクリックパケットハンドラ
     */
    private static void registerSkillClickHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ConstantSkillClickPacket.PACKET_ID,
                (payload, context) -> {
                    String skillName = payload.skillName();
                    boolean status = payload.status();
                    context.server().execute(() -> {
                        try {
                            BackendClient.postJson(
                                    ModConfig.ENDPOINT_SKILL_SWITCH,
                                    new SkillSwitchRequest(skillName, status));

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("ConstantSkillClickPacket: {} -> {}", skillName, status);
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("ConstantSkillClickPacket", e));
                        }
                    });
                });
    }

    /**
     * チャットメッセージパケットハンドラ
     */
    private static void registerChatMessageHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ChatMessageSendPacket.PACKET_ID,
                (payload, context) -> {
                    String message = payload.message();
                    ServerPlayerEntity player = context.player();
                    String senderName = player.getName().getString();

                    context.server().execute(() -> {
                        try {
                            // StateManager経由でチャット履歴に追加
                            updateChatState(player, message);

                            // Backend通知
                            BackendClient.postJson(
                                    ModConfig.ENDPOINT_CHAT_MESSAGE,
                                    new ChatMessageRequest(senderName, message));

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("ChatMessageSendPacket: {} - {}", senderName, message);
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("ChatMessageSendPacket", e));
                        }
                    });
                });
    }

    /**
     * チャット状態を更新
     */
    private static void updateChatState(ServerPlayerEntity player, String message) {
        StateManager stateManager = ShannonUIMod.getStateManager();
        ChatState chatState = stateManager.getChatState();

        ChatState.ChatMessage chatMessage = new ChatState.ChatMessage();
        chatMessage.sender = player.getName().getString();
        chatMessage.message = message;
        chatMessage.timestamp = System.currentTimeMillis();
        chatState.messages.add(chatMessage);

        stateManager.updateChatState(chatState);
    }

    /**
     * 反応設定更新パケットハンドラ
     */
    private static void registerReactionSettingUpdateHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ReactionSettingUpdatePacket.PACKET_ID,
                (payload, context) -> {
                    String eventType = payload.eventType();
                    boolean enabled = payload.enabled();
                    int probability = payload.probability();

                    context.server().execute(() -> {
                        try {
                            BackendClient.postJson(
                                    ModConfig.ENDPOINT_REACTION_SETTING_UPDATE,
                                    new com.shannon.client.request.ReactionSettingUpdateRequest(
                                            eventType, enabled, probability));

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("ReactionSettingUpdatePacket: {} -> enabled={}, prob={}",
                                        eventType, enabled, probability);
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("ReactionSettingUpdatePacket", e));
                        }
                    });
                });
    }

    /**
     * 反応設定リセットパケットハンドラ
     */
    private static void registerReactionSettingsResetHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ReactionSettingsResetPacket.PACKET_ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        try {
                            BackendClient.postJson(
                                    ModConfig.ENDPOINT_REACTION_SETTINGS_RESET,
                                    java.util.Collections.emptyMap()); // 空のリクエスト

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("ReactionSettingsResetPacket received");
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("ReactionSettingsResetPacket", e));
                        }
                    });
                });
    }

    /**
     * スクリーンショット結果パケットハンドラ
     */
    private static void registerScreenshotResultHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                ScreenshotResultPacket.PACKET_ID,
                (payload, context) -> {
                    try {
                        // HTTPエンドポイントに結果を渡す
                        ServerScreenshotEndpoint.handleResult(payload);

                        if (ModConfig.LOG_PACKETS) {
                            LOGGER.debug("ScreenshotResultPacket received: {} (success: {})",
                                    payload.requestId(), payload.success());
                        }
                    } catch (Exception e) {
                        ModErrorHandler.handle(
                                new PacketHandlingException("ScreenshotResultPacket", e));
                    }
                });
    }

    /**
     * タスクアクションパケットハンドラ（削除、優先実行）
     */
    private static void registerTaskActionHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                TaskActionPacket.PACKET_ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        try {
                            String action = payload.action();
                            String taskId = payload.taskId();

                            String endpoint = null;
                            if (TaskActionPacket.ACTION_DELETE.equals(action)) {
                                endpoint = ModConfig.ENDPOINT_TASK_DELETE;
                            } else if (TaskActionPacket.ACTION_PRIORITIZE.equals(action)) {
                                endpoint = ModConfig.ENDPOINT_TASK_PRIORITIZE;
                            }

                            if (endpoint != null) {
                                BackendClient.postJson(endpoint, java.util.Map.of("taskId", taskId));
                            }

                            if (ModConfig.LOG_PACKETS) {
                                LOGGER.debug("TaskActionPacket received: action={}, taskId={}", action, taskId);
                            }
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("TaskActionPacket", e));
                        }
                    });
                });
    }

    /**
     * 進捗データリクエストパケットハンドラ
     * クライアントからのリクエストに応じて進捗データを収集・送信
     */
    private static void registerRequestAdvancementsHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                RequestAdvancementsPacket.PACKET_ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    String requestedPlayer = payload.playerName();
                    LOGGER.info("[Advancements] Request received from {} for '{}'",
                            player.getName().getString(), requestedPlayer);

                    // 空の場合はターゲットプレイヤー
                    if (requestedPlayer == null || requestedPlayer.isEmpty()) {
                        requestedPlayer = ModConfig.TARGET_PLAYER_NAME;
                    }
                    final String targetName = requestedPlayer;

                    // プレイヤーリストをログ出力
                    LOGGER.info("[Advancements] Looking for '{}', online players: {}",
                            targetName,
                            context.server().getPlayerManager().getPlayerList().stream()
                                    .map(p -> p.getName().getString())
                                    .reduce((a, b) -> a + ", " + b)
                                    .orElse("none"));

                    context.server().execute(() -> {
                        try {
                            com.shannon.network.packet.AdvancementsState state =
                                com.shannon.util.AdvancementCollector.collect(context.server(), targetName);

                            LOGGER.info("[Advancements] Collected: playerName='{}', categories={}",
                                    state.playerName,
                                    state.categories != null ? state.categories.size() : 0);

                            // リクエストしたプレイヤーにのみ送信
                            if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(
                                    player, AdvancementsStatePacket.PACKET_ID)) {
                                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                    player, new AdvancementsStatePacket(state));
                                LOGGER.info("[Advancements] Sent to {}", player.getName().getString());
                            } else {
                                LOGGER.warn("[Advancements] Cannot send to {} - channel not available",
                                        player.getName().getString());
                            }
                        } catch (Exception e) {
                            LOGGER.error("[Advancements] Error processing request", e);
                            ModErrorHandler.handle(
                                    new PacketHandlingException("RequestAdvancementsPacket", e));
                        }
                    });
                });
    }

    private static void registerVoiceModeToggleHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                VoiceModeTogglePacket.PACKET_ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        try {
                            BackendClient.postWithBody(ModConfig.ENDPOINT_VOICE_MODE, "{}", (responseCode, body) -> {
                                if (ModConfig.LOG_PACKETS) {
                                    LOGGER.debug("VoiceModeToggle response: {} body: {}", responseCode, body);
                                }
                                try {
                                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                                    boolean success = json.has("success") && json.get("success").getAsBoolean();
                                    String result = json.has("result") ? json.get("result").getAsString() : "";
                                    String msg;
                                    if (success) {
                                        msg = "§a🎙️ ボイスモード: " + result;
                                    } else {
                                        msg = "§c🎙️ " + result;
                                    }
                                    context.server().execute(() -> player.sendMessage(
                                            net.minecraft.text.Text.literal(msg), false));
                                } catch (Exception e) {
                                    if (responseCode == -1) {
                                        context.server().execute(() -> player.sendMessage(
                                                net.minecraft.text.Text.literal("§c🎙️ バックエンド接続失敗"), false));
                                    }
                                }
                            });
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("VoiceModeTogglePacket", e));
                            player.sendMessage(
                                    net.minecraft.text.Text.literal("§c🎙️ バックエンド接続失敗"), false);
                        }
                    });
                });
    }

    private static void registerVoicePttHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
                VoicePttPacket.PACKET_ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    String mcUsername = player.getName().getString();
                    String action = payload.pressed() ? "on" : "off";
                    context.server().execute(() -> {
                        try {
                            String json = new com.google.gson.Gson().toJson(new VoicePttRequest(mcUsername, action));
                            BackendClient.postWithBody(
                                    ModConfig.ENDPOINT_VOICE_PTT,
                                    json,
                                    (responseCode, body) -> {
                                        if (ModConfig.LOG_PACKETS) {
                                            LOGGER.debug("VoicePtt response: {} body: {}", responseCode, body);
                                        }
                                        try {
                                            com.google.gson.JsonObject resp = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                                            boolean success = resp.has("success") && resp.get("success").getAsBoolean();
                                            if (!success) {
                                                String result = resp.has("result") ? resp.get("result").getAsString() : "エラー";
                                                context.server().execute(() -> player.sendMessage(
                                                        net.minecraft.text.Text.literal("§c🎙️ " + result), false));
                                            }
                                        } catch (Exception ignored) {
                                            if (responseCode == -1) {
                                                context.server().execute(() -> player.sendMessage(
                                                        net.minecraft.text.Text.literal("§c🎙️ バックエンド接続失敗"), false));
                                            }
                                        }
                                    });
                        } catch (Exception e) {
                            ModErrorHandler.handle(
                                    new PacketHandlingException("VoicePttPacket", e));
                            player.sendMessage(
                                    net.minecraft.text.Text.literal("§c🎙️ バックエンド接続失敗"), false);
                        }
                    });
                });
    }

    private PacketHandlerRegistry() {
        // ユーティリティクラスなのでインスタンス化を防ぐ
    }
}
