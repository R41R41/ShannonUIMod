package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: クライアントからサーバーに進捗データをリクエストするパケット
 */
public record RequestAdvancementsPacket(String playerName) implements CustomPayload {
    public static final CustomPayload.Id<RequestAdvancementsPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "request_advancements"));

    public static final PacketCodec<RegistryByteBuf, RequestAdvancementsPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.playerName != null ? value.playerName : ""),
            buf -> new RequestAdvancementsPacket(buf.readString()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
