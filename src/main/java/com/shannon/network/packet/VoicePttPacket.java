package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VoicePttPacket(boolean pressed) implements CustomPayload {
    public static final CustomPayload.Id<VoicePttPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "voice_ptt"));

    public static final PacketCodec<RegistryByteBuf, VoicePttPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, VoicePttPacket::pressed,
            VoicePttPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
