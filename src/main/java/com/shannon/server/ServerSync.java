package com.shannon.server;

import com.shannon.sync.SyncChannel;
import com.shannon.sync.SyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends state to clients. Call only on the server thread.
 *
 * <p>Players whose client does not have this mod, or has another protocol version, are skipped,
 * so vanilla clients and other mods' clients can join the same server.
 */
public final class ServerSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSync.class);

    private ServerSync() {
    }

    public static <T> void broadcast(MinecraftServer server, SyncChannel<T> channel, T state) {
        SyncPayload payload = encode(channel, state);
        if (payload == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendPayload(player, payload);
        }
    }

    public static <T> void send(ServerPlayerEntity player, SyncChannel<T> channel, T state) {
        SyncPayload payload = encode(channel, state);
        if (payload != null) {
            sendPayload(player, payload);
        }
    }

    private static <T> SyncPayload encode(SyncChannel<T> channel, T state) {
        if (state == null) {
            return null;
        }
        SyncPayload payload = SyncPayload.of(channel, state);
        if (payload.json().length > SyncPayload.MAX_BYTES) {
            LOGGER.warn("Not syncing {}: {} bytes is over the payload limit", channel.name(), payload.json().length);
            return null;
        }
        return payload;
    }

    private static void sendPayload(ServerPlayerEntity player, SyncPayload payload) {
        if (ServerPlayNetworking.canSend(player, SyncPayload.ID)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
