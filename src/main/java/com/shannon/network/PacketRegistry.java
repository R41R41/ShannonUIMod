package com.shannon.network;

import com.shannon.network.packet.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * パケットタイプの登録を一元管理
 * S2C（Server to Client）とC2S（Client to Server）パケットの登録
 */
public class PacketRegistry {
        private static final Logger LOGGER = LoggerFactory.getLogger(PacketRegistry.class);

        /**
         * 全てのパケットタイプを登録
         */
        public static void registerAll() {
                LOGGER.info("📦 Registering packet types...");
                registerS2CPackets();
                registerC2SPackets();
                LOGGER.info("✅ All packet types registered");
        }

        /**
         * S2C（Server to Client）パケットを登録
         */
        private static void registerS2CPackets() {
                PayloadTypeRegistry.playS2C().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(TaskListStatePacket.PACKET_ID, TaskListStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(InventoryStatePacket.PACKET_ID,
                                InventoryStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(ConstantSkillsStatePacket.PACKET_ID,
                                ConstantSkillsStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(PlayerStatusStatePacket.PACKET_ID,
                                PlayerStatusStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(ChatStatePacket.PACKET_ID, ChatStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(DetailedLogsStatePacket.PACKET_ID,
                                DetailedLogsStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(ReactionSettingsStatePacket.PACKET_ID,
                                ReactionSettingsStatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playS2C().register(ScreenshotRequestPacket.PACKET_ID,
                                ScreenshotRequestPacket.PACKET_CODEC);

                LOGGER.debug("  ✓ S2C packets registered (10 types)");
        }

        /**
         * C2S（Client to Server）パケットを登録
         */
        private static void registerC2SPackets() {
                PayloadTypeRegistry.playC2S().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(InventoryItemClickPacket.PACKET_ID,
                                InventoryItemClickPacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(ConstantSkillClickPacket.PACKET_ID,
                                ConstantSkillClickPacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(ChatMessageSendPacket.PACKET_ID,
                                ChatMessageSendPacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(ReactionSettingUpdatePacket.PACKET_ID,
                                ReactionSettingUpdatePacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(ReactionSettingsResetPacket.PACKET_ID,
                                ReactionSettingsResetPacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(ScreenshotResultPacket.PACKET_ID,
                                ScreenshotResultPacket.PACKET_CODEC);
                PayloadTypeRegistry.playC2S().register(TaskActionPacket.PACKET_ID,
                                TaskActionPacket.PACKET_CODEC);

                LOGGER.debug("  ✓ C2S packets registered (8 types)");
        }

        private PacketRegistry() {
                // ユーティリティクラスなのでインスタンス化を防ぐ
        }
}
