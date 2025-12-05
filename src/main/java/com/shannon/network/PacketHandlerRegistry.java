package com.shannon.network;

import com.shannon.MyModServer;
import com.shannon.ShannonUIMod;
import com.shannon.client.BackendClient;
import com.shannon.client.request.ChatMessageRequest;
import com.shannon.client.request.SkillSwitchRequest;
import com.shannon.client.request.ThrowItemRequest;
import com.shannon.config.ModConfig;
import com.shannon.error.ModErrorHandler;
import com.shannon.error.exceptions.PacketHandlingException;
import com.shannon.network.packet.*;
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
                                    new Object()); // 空のリクエスト

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

    private PacketHandlerRegistry() {
        // ユーティリティクラスなのでインスタンス化を防ぐ
    }
}
