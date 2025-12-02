package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChatMessageSendPacket(String message) implements CustomPayload {
    public static final CustomPayload.Id<ChatMessageSendPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "chat_message_send"));

    public static final PacketCodec<RegistryByteBuf, ChatMessageSendPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.message),
            buf -> new ChatMessageSendPacket(buf.readString()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
