package com.shannon.server;

import com.shannon.client.BackendClient;
import com.shannon.client.request.ChatMessageRequest;
import com.shannon.client.request.SkillSwitchRequest;
import com.shannon.client.request.ThrowItemRequest;
import com.shannon.client.request.VoicePttRequest;
import com.shannon.config.ModConfig;
import com.shannon.model.AdvancementsState;
import com.shannon.model.ChatState;
import com.shannon.model.ReactionSettingsState;
import com.shannon.state.StateManager;
import com.shannon.sync.ActionChannel;
import com.shannon.sync.ActionPayload;
import com.shannon.sync.Actions;
import com.shannon.sync.BotCommand;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;
import com.shannon.util.AdvancementCollector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Receives the client's requests and turns them into calls to the bot backend.
 *
 * <p>Each {@link Actions} channel has exactly one handler here. Handlers run on the server thread
 * and only start backend calls, which run on {@link BackendClient}'s own thread.
 */
public final class ServerActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerActions.class);

    private static final int MAX_CHAT_LENGTH = 256;
    private static final int MAX_GIVE_COUNT = 64 * 36;
    /** Requests one player may make per second before the rest are dropped. */
    private static final int MAX_ACTIONS_PER_SECOND = 20;

    @FunctionalInterface
    private interface Handler<T> {
        void handle(MinecraftServer server, ServerPlayerEntity player, T request);
    }

    private record Entry<T>(ActionChannel<T> channel, Handler<T> handler) {
        void dispatch(MinecraftServer server, ServerPlayerEntity player, byte[] json) {
            T request = SyncJson.decode(json, channel.type());
            if (request == null) {
                LOGGER.warn("Ignoring malformed {} from {}", channel.name(), player.getName().getString());
                return;
            }
            handler.handle(server, player, request);
        }
    }

    private static final Map<String, Entry<?>> HANDLERS = new LinkedHashMap<>();
    private static final Map<UUID, long[]> RATE = new HashMap<>();

    private ServerActions() {
    }

    public static void register() {
        on(Actions.GIVE_ITEM, ServerActions::giveItem);
        on(Actions.TOGGLE_SKILL, (server, player, request) -> BackendClient.postJson(
                ModConfig.ENDPOINT_SKILL_SWITCH, new SkillSwitchRequest(request.skillName, request.enabled)));
        on(Actions.CHAT, ServerActions::chat);
        on(Actions.TASK, ServerActions::taskAction);
        on(Actions.COMMAND, (server, player, request) -> {
            BotCommand command = BotCommand.parse(request.command);
            if (command != null) {
                BotCommandDispatcher.dispatch(server, player, command);
            }
        });
        on(Actions.REACTION_UPDATE, ServerActions::reactionUpdate);
        on(Actions.REACTION_RESET, (server, player, request) -> BackendClient.postJson(
                ModConfig.ENDPOINT_REACTION_SETTINGS_RESET, Map.of()));
        on(Actions.REQUEST_ADVANCEMENTS, (server, player, request) -> {
            AdvancementsState state = AdvancementCollector.collect(server, ModConfig.TARGET_PLAYER_NAME);
            ServerSync.send(player, StateChannels.ADVANCEMENTS, state);
        });
        on(Actions.VOICE_MODE, ServerActions::voiceMode);
        on(Actions.VOICE_PTT, (server, player, request) -> BackendClient.post(ModConfig.ENDPOINT_VOICE_PTT,
                        new VoicePttRequest(player.getName().getString(), request.pressed ? "on" : "off"))
                .thenAccept(response -> {
                    if (!response.ok()) {
                        server.execute(() -> notifyFailure(player));
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayerEntity player = context.player();
            server.execute(() -> receive(server, player, payload));
        });
    }

    private static <T> void on(ActionChannel<T> channel, Handler<T> handler) {
        HANDLERS.put(channel.name(), new Entry<>(channel, handler));
    }

    private static void receive(MinecraftServer server, ServerPlayerEntity player, ActionPayload payload) {
        Entry<?> entry = HANDLERS.get(payload.action());
        if (entry == null || !withinRate(player)) {
            return;
        }
        try {
            entry.dispatch(server, player, payload.json());
        } catch (RuntimeException e) {
            LOGGER.error("Action {} from {} failed", payload.action(), player.getName().getString(), e);
        }
    }

    private static boolean withinRate(ServerPlayerEntity player) {
        long second = System.currentTimeMillis() / 1000;
        long[] window = RATE.computeIfAbsent(player.getUuid(), id -> new long[]{second, 0});
        if (window[0] != second) {
            window[0] = second;
            window[1] = 0;
        }
        return ++window[1] <= MAX_ACTIONS_PER_SECOND;
    }

    /** Forgets a player's rate window when they leave. */
    public static void forget(ServerPlayerEntity player) {
        RATE.remove(player.getUuid());
    }

    // ===== Handlers =====

    private static void giveItem(MinecraftServer server, ServerPlayerEntity player, Actions.GiveItem request) {
        if (request.item == null || request.item.isBlank()) {
            return;
        }
        int count = Math.max(1, Math.min(request.count, MAX_GIVE_COUNT));
        BackendClient.post(ModConfig.ENDPOINT_THROW_ITEM, new ThrowItemRequest(request.item, count))
                .thenAccept(response -> {
                    if (!response.ok()) {
                        server.execute(() -> notifyFailure(player));
                    }
                });
    }

    private static void chat(MinecraftServer server, ServerPlayerEntity player, Actions.Chat request) {
        String message = request.message == null ? "" : request.message.strip();
        if (message.isEmpty()) {
            return;
        }
        if (message.length() > MAX_CHAT_LENGTH) {
            message = message.substring(0, MAX_CHAT_LENGTH);
        }
        sayToBot(server, player, message);
    }

    /** Adds a line to the conversation as {@code player}, and sends it to the bot. */
    static void sayToBot(MinecraftServer server, ServerPlayerEntity player, String message) {
        String sender = player.getName().getString();
        StateManager.getInstance().addChat(
                new ChatState.Message(ChatState.Kind.CHAT, sender, message, System.currentTimeMillis()));
        // The backend answers only when the bot finishes; the answer arrives through /bot_chat.
        BackendClient.post(ModConfig.ENDPOINT_CHAT_MESSAGE, new ChatMessageRequest(sender, message))
                .thenAccept(response -> {
                    if (response.status() == -1 || response.status() == 401 || response.status() == 503) {
                        server.execute(() -> notifyFailure(player));
                    }
                });
    }

    private static void taskAction(MinecraftServer server, ServerPlayerEntity player, Actions.TaskAction request) {
        if (request.taskId == null || request.taskId.isBlank()) {
            return;
        }
        String endpoint = switch (request.action == null ? "" : request.action) {
            case Actions.TaskAction.DELETE -> ModConfig.ENDPOINT_TASK_DELETE;
            case Actions.TaskAction.PRIORITIZE -> ModConfig.ENDPOINT_TASK_PRIORITIZE;
            default -> null;
        };
        if (endpoint != null) {
            BackendClient.post(endpoint, Map.of("taskId", request.taskId)).thenAccept(response -> {
                if (!response.ok()) {
                    server.execute(() -> notifyFailure(player));
                }
            });
        }
    }

    private static void reactionUpdate(MinecraftServer server, ServerPlayerEntity player,
                                       Actions.ReactionUpdate request) {
        if (request.eventType == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", request.eventType);
        body.put("enabled", request.enabled);
        body.put("probability", Math.max(0, Math.min(100, request.probability)));
        if (request.criticalDistance != null || request.detectionDistance != null) {
            ReactionSettingsState current = StateManager.getInstance().get(StateChannels.REACTIONS);
            ReactionSettingsState.HostileDetection base = current != null ? current.hostileDetection : null;
            Map<String, Object> hostile = new LinkedHashMap<>();
            hostile.put("criticalDistance", request.criticalDistance != null
                    ? request.criticalDistance : base != null ? base.criticalDistance : 4.0);
            hostile.put("detectionDistance", request.detectionDistance != null
                    ? request.detectionDistance : base != null ? base.detectionDistance : 16.0);
            hostile.put("multiMobCriticalCount", base != null ? base.multiMobCriticalCount : 3);
            body.put("hostileDetection", hostile);
        }
        BackendClient.postJson(ModConfig.ENDPOINT_REACTION_SETTING_UPDATE, body);
    }

    private static void voiceMode(MinecraftServer server, ServerPlayerEntity player, Actions.Empty request) {
        BackendClient.post(ModConfig.ENDPOINT_VOICE_MODE, Map.of()).thenAccept(response -> server.execute(() -> {
            if (!response.ok()) {
                notifyFailure(player);
                return;
            }
            String result = "";
            try {
                var json = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
                result = json.has("result") ? json.get("result").getAsString() : "";
            } catch (RuntimeException ignored) {
                // A body without a result still means the mode changed.
            }
            player.sendMessage(Text.translatableWithFallback("shannonuimod.voice.mode", "音声の宛先: %s", result), true);
        }));
    }

    /** Tells the player, above the hotbar, that the bot could not be reached. */
    static void notifyFailure(ServerPlayerEntity player) {
        player.sendMessage(Text.translatableWithFallback("shannonuimod.error.backend", "シャノンにつながりませんでした").formatted(Formatting.RED), true);
    }
}
