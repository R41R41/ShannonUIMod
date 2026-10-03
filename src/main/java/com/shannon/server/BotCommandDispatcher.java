package com.shannon.server;

import com.shannon.client.BackendClient;
import com.shannon.client.request.SkillSwitchRequest;
import com.shannon.config.ModConfig;
import com.shannon.model.TaskListState;
import com.shannon.state.StateManager;
import com.shannon.sync.BotCommand;
import com.shannon.sync.StateChannels;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * Sends a {@link BotCommand} to the backend's {@code /bot_command} route, which runs it as a fixed
 * action and answers in the bot's own words through {@code /bot_chat}.
 *
 * <p>A backend from before that route answers 404; the command then falls back to the closest
 * older endpoints, so the mod works with both.
 */
final class BotCommandDispatcher {
    private static final String SAY_STOP = "止まって";
    private static final String SAY_COME = "こっちに来て";

    private BotCommandDispatcher() {
    }

    static void dispatch(MinecraftServer server, ServerPlayerEntity player, BotCommand command) {
        String sender = player.getName().getString();
        BackendClient.post(ModConfig.ENDPOINT_BOT_COMMAND, Map.of("command", command.name(), "sender", sender))
                .thenAccept(response -> server.execute(() -> {
                    if (response.status() == 404) {
                        fallback(server, player, command);
                    } else if (!response.ok() && response.status() != 400) {
                        ServerActions.notifyFailure(player);
                    }
                }));
    }

    /** What the command meant before the backend had a route for it. */
    private static void fallback(MinecraftServer server, ServerPlayerEntity player, BotCommand command) {
        switch (command) {
            case STOP -> {
                setFollow(false);
                ServerActions.sayToBot(server, player, SAY_STOP);
            }
            case FOLLOW -> setFollow(true);
            case COME -> ServerActions.sayToBot(server, player, SAY_COME);
            case RESUME -> BackendClient.postJson(ModConfig.ENDPOINT_TASK_CONTINUE, Map.of());
            case CANCEL -> {
                String taskId = currentTaskId();
                if (taskId == null) {
                    player.sendMessage(Text.translatableWithFallback("shannonuimod.command.no_task",
                            "シャノンはいま作業をしていません").formatted(Formatting.GRAY), true);
                    return;
                }
                BackendClient.postJson(ModConfig.ENDPOINT_TASK_DELETE, Map.of("taskId", taskId));
            }
        }
    }

    private static void setFollow(boolean enabled) {
        BackendClient.postJson(ModConfig.ENDPOINT_SKILL_SWITCH,
                new SkillSwitchRequest(ModConfig.FOLLOW_SKILL_NAME, enabled));
    }

    private static String currentTaskId() {
        TaskListState list = StateManager.getInstance().get(StateChannels.TASK_LIST);
        return list != null ? list.currentTaskId : null;
    }
}
