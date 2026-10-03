package com.shannon;

import com.shannon.command.ShannonCommand;
import com.shannon.config.ModConfig;
import com.shannon.http.HttpServerManager;
import com.shannon.model.AdvancementsState;
import com.shannon.network.packet.ScreenshotResultPacket;
import com.shannon.http.endpoints.ServerScreenshotEndpoint;
import com.shannon.server.BotObserver;
import com.shannon.server.ServerActions;
import com.shannon.server.ServerSync;
import com.shannon.state.StateManager;
import com.shannon.sync.ShannonNetworking;
import com.shannon.sync.StateChannels;
import com.shannon.util.AdvancementCollector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Wires the server half of the mod: the push server the bot backend talks to,
 * the sync of its state to clients, and the handlers for what clients ask.
 *
 * <p>The mod uses no mixins; everything goes through Fabric API events, so it does not conflict
 * with other mods that change vanilla classes.
 */
public class ShannonUIMod implements ModInitializer {
    public static final String MOD_ID = "shannonuimod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final StateManager STATES = StateManager.getInstance();
    private static final BotObserver OBSERVER = new BotObserver(STATES);

    @Override
    public void onInitialize() {
        ModConfig.logConfiguration();
        ShannonNetworking.registerPayloadTypes();
        ServerActions.register();
        ServerPlayNetworking.registerGlobalReceiver(ScreenshotResultPacket.PACKET_ID,
                (payload, context) -> ServerScreenshotEndpoint.handleResult(payload));

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ShannonCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            STATES.setServer(server);
            OBSERVER.reset();
            HttpServerManager.startServer();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            HttpServerManager.stopServer();
            STATES.setServer(null);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            STATES.tick();
            OBSERVER.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Runs after the join completes, when the client can receive mod payloads.
            server.execute(() -> {
                STATES.sendAllTo(player);
                OBSERVER.reset();
                if (player.getName().getString().equals(ModConfig.TARGET_PLAYER_NAME)) {
                    AdvancementsState advancements = AdvancementCollector.collect(server, ModConfig.TARGET_PLAYER_NAME);
                    ServerSync.broadcast(server, StateChannels.ADVANCEMENTS, advancements);
                }
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerActions.forget(handler.getPlayer()));

        LOGGER.info("ShannonUIMod initialized");
    }

    /** Kept for the endpoints that look up the running server. */
    public static StateManager getStateManager() {
        return STATES;
    }
}
