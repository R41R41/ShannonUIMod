package com.shannon.ui;

import com.shannon.config.ModConfig;
import com.shannon.model.BotVitals;
import com.shannon.sync.StateChannels;
import com.shannon.ui.feature.ScreenshotFeature;
import com.shannon.ui.hud.Notifier;
import com.shannon.ui.hud.ShannonHud;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.net.ClientSync;
import com.shannon.ui.state.Attention;
import com.shannon.ui.state.ClientStore;
import com.shannon.ui.state.DangerWatch;
import com.shannon.ui.state.TaskHistory;
import com.shannon.ui.state.VoiceStatus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * The client half of the mod. Owns the shared pieces and wires them to Fabric events.
 *
 * <p>Views reach shared state only through {@link #get()}, so there is one store, one config
 * and one set of key bindings.
 */
public final class ShannonClient {
    private static ShannonClient instance;

    private final ClientStore store = new ClientStore();
    private final ClientConfig config = ClientConfig.load();
    private final KeyBindings keys = new KeyBindings();
    private final DangerWatch danger = new DangerWatch(store);
    private final Attention attention = new Attention(store, danger);
    private final TaskHistory history = new TaskHistory(store, danger);
    private final VoiceStatus voice = new VoiceStatus(store);

    private ShannonClient() {
    }

    public static ShannonClient get() {
        return instance;
    }

    /** Called once from the client entrypoint. */
    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new ShannonClient();
        instance.wire();
    }

    private void wire() {
        ClientSync.register(store);
        keys.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> keys.tick(client, this));
        ShannonHud.register(this);
        Notifier.register(this);
        ScreenshotFeature.register();
    }

    public ClientStore store() {
        return store;
    }

    public ClientConfig config() {
        return config;
    }

    public KeyBindings keys() {
        return keys;
    }

    public DangerWatch danger() {
        return danger;
    }

    public Attention attention() {
        return attention;
    }

    public TaskHistory history() {
        return history;
    }

    public VoiceStatus voice() {
        return voice;
    }

    /** The bot's player name, from the server when known. */
    public String botName() {
        BotVitals vitals = store.get(StateChannels.VITALS);
        return vitals != null && vitals.name != null ? vitals.name : ModConfig.TARGET_PLAYER_NAME;
    }
}
