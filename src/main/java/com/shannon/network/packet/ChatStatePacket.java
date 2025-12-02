package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

public record ChatStatePacket(ChatState state) implements CustomPayload {
    public static final CustomPayload.Id<ChatStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "chat_state"));

    public static final PacketCodec<RegistryByteBuf, ChatStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                if (value.state.messages != null) {
                    buf.writeInt(value.state.messages.size());
                    for (ChatState.ChatMessage msg : value.state.messages) {
                        buf.writeString(msg.sender);
                        buf.writeString(msg.message);
                        buf.writeLong(msg.timestamp);
                    }
                } else {
                    buf.writeInt(-1);
                }
            },
            buf -> {
                ChatState state = new ChatState();
                int messageCount = buf.readInt();
                if (messageCount >= 0) {
                    state.messages = new ArrayList<>();
                    for (int i = 0; i < messageCount; i++) {
                        ChatState.ChatMessage msg = new ChatState.ChatMessage();
                        msg.sender = buf.readString();
                        msg.message = buf.readString();
                        msg.timestamp = buf.readLong();
                        state.messages.add(msg);
                    }
                }
                return new ChatStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
