package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 反応設定更新パケット (C2S)
 */
public record ReactionSettingUpdatePacket(String eventType, boolean enabled, int probability) implements CustomPayload {
    public static final CustomPayload.Id<ReactionSettingUpdatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "reaction_setting_update"));

    public static final PacketCodec<RegistryByteBuf, ReactionSettingUpdatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.eventType);
                buf.writeBoolean(value.enabled);
                buf.writeInt(value.probability);
            },
            buf -> new ReactionSettingUpdatePacket(
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readInt()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
