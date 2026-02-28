package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VoiceModeTogglePacket() implements CustomPayload {
    public static final CustomPayload.Id<VoiceModeTogglePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "voice_mode_toggle"));

    public static final PacketCodec<RegistryByteBuf, VoiceModeTogglePacket> PACKET_CODEC = PacketCodec.unit(
            new VoiceModeTogglePacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
