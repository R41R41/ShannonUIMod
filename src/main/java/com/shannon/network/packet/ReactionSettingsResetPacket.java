package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 反応設定リセットパケット (C2S)
 */
public record ReactionSettingsResetPacket() implements CustomPayload {
    public static final CustomPayload.Id<ReactionSettingsResetPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "reaction_settings_reset"));

    public static final PacketCodec<RegistryByteBuf, ReactionSettingsResetPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // 空のパケット
            },
            buf -> new ReactionSettingsResetPacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
