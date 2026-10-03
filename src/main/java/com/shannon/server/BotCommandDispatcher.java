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
 * Turns a {@link BotCommand} into backend calls.
 *
 * <p>This is the only place that knows how each command reaches the bot. The backend has no
 * dedicated route for stopping or coming over yet, so those two are said to the bot in words; when
 * a route exists, change only the matching case here.
 */
final class BotCommandDispatcher {
    private static final String SAY_STOP = "止まって";
    private static final String SAY_COME = "こっちに来て";

    private BotCommandDispatcher() {
    }

    static void dispatch(MinecraftServer server, ServerPlayerEntity player, BotCommand command) {
        switch (command) {
            case STOP -> {
                setFollow(false);
                ServerActions.sayToBot(server, player, SAY_STOP);
            }
            case FOLLOW -> setFollow(true);
            case COME -> ServerActions.sayToBot(server, player, SAY_COME);
            case RESUME -> {
                String taskId = currentTaskId();
                BackendClient.postJson(ModConfig.ENDPOINT_TASK_CONTINUE,
                        taskId == null ? Map.of() : Map.of("taskId", taskId));
            }
            case CANCEL -> {
                String taskId = currentTaskId();
                if (taskId == null) {
                    player.sendMessage(Text.translatableWithFallback("shannonuimod.command.no_task", "シャノンはいま作業をしていません").formatted(Formatting.GRAY), true);
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
