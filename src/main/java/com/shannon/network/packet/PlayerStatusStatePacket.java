package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerStatusStatePacket(PlayerStatusState state) implements CustomPayload {
    public static final CustomPayload.Id<PlayerStatusStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "player_status"));

    public static final PacketCodec<RegistryByteBuf, PlayerStatusStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeFloat(value.state.health);
                buf.writeFloat(value.state.maxHealth);
                buf.writeInt(value.state.hunger);
            },
            (buf) -> {
                PlayerStatusState state = new PlayerStatusState();
                state.health = buf.readFloat();
                state.maxHealth = buf.readFloat();
                state.hunger = buf.readInt();
                return new PlayerStatusStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}