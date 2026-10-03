package com.shannon.sync;

import com.shannon.ShannonUIMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client to server: one request on one {@link ActionChannel}, as JSON. */
public record ActionPayload(String action, byte[] json) implements CustomPayload {
    public static final CustomPayload.Id<ActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ShannonUIMod.MOD_ID, "action_v2"));

    /** Requests are small; anything larger is refused before decoding. */
    public static final int MAX_BYTES = 32_000;
    private static final int MAX_ACTION_LENGTH = 64;

    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.action, MAX_ACTION_LENGTH);
                buf.writeByteArray(value.json);
            },
            buf -> new ActionPayload(buf.readString(MAX_ACTION_LENGTH), buf.readByteArray(MAX_BYTES)));

    public static <T> ActionPayload of(ActionChannel<T> channel, T request) {
        return new ActionPayload(channel.name(), SyncJson.encode(request));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
