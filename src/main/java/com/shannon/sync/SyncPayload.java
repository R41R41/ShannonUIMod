package com.shannon.sync;

import com.shannon.ShannonUIMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server to client: one state update on one {@link SyncChannel}, as JSON.
 *
 * <p>The channel id carries a protocol version. A client and a server with different versions
 * simply do not see each other's channel, instead of failing to decode and disconnecting.
 */
public record SyncPayload(String channel, byte[] json) implements CustomPayload {
    public static final CustomPayload.Id<SyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ShannonUIMod.MOD_ID, "sync_v2"));

    /** Vanilla refuses client-bound custom payloads over 1 MiB. */
    public static final int MAX_BYTES = 1_000_000;
    private static final int MAX_CHANNEL_LENGTH = 64;

    public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.channel, MAX_CHANNEL_LENGTH);
                buf.writeByteArray(value.json);
            },
            buf -> new SyncPayload(buf.readString(MAX_CHANNEL_LENGTH), buf.readByteArray(MAX_BYTES)));

    public static <T> SyncPayload of(SyncChannel<T> channel, T state) {
        return new SyncPayload(channel.name(), SyncJson.encode(state));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
