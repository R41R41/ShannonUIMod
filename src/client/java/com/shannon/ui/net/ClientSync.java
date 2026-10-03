package com.shannon.ui.net;

import com.shannon.ShannonUIMod;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncChannel;
import com.shannon.sync.SyncJson;
import com.shannon.sync.SyncPayload;
import com.shannon.ui.state.ClientStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;

/** Receives synced state from the server and puts it into the {@link ClientStore}. */
public final class ClientSync {
    private static final Map<String, SyncChannel<?>> CHANNELS = new HashMap<>();

    static {
        for (SyncChannel<?> channel : StateChannels.ALL) {
            CHANNELS.put(channel.name(), channel);
        }
    }

    private ClientSync() {
    }

    public static void register(ClientStore store) {
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) ->
                context.client().execute(() -> apply(store, payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(store::clear));
    }

    private static void apply(ClientStore store, SyncPayload payload) {
        SyncChannel<?> channel = CHANNELS.get(payload.channel());
        if (channel == null) {
            return; // A newer server; ignore what this client does not know.
        }
        applyTyped(store, channel, payload.json());
    }

    private static <T> void applyTyped(ClientStore store, SyncChannel<T> channel, byte[] json) {
        T state = SyncJson.decode(json, channel.type());
        if (state == null) {
            ShannonUIMod.LOGGER.warn("Ignoring malformed {} state", channel.name());
            return;
        }
        store.put(channel, state);
    }
}
