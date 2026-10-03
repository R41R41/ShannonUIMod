package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.TaskListState;
import com.shannon.model.TaskTreeState;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;

/** {@code POST /task_list}: the bot's task queue. */
public class TaskListEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        TaskListState state = SyncJson.GSON.fromJson(body, TaskListState.class);
        if (state == null) {
            throw new IllegalArgumentException("empty task list");
        }
        StateManager states = StateManager.getInstance();
        states.publish(StateChannels.TASK_LIST, state);
        // How a task ended (stopped, failed) reaches only the task list; without it the card would
        // keep showing the task as running.
        TaskTreeState tree = state.currentTaskTree;
        if (tree != null && tree.goal != null && isFinished(tree.status)) {
            TaskTreeState shown = states.get(StateChannels.TASK_TREE);
            if (shown == null || !tree.goal.equals(shown.goal) || !tree.status.equals(shown.status)) {
                states.updateTaskTree(tree);
            }
        }
    }

    private static boolean isFinished(String status) {
        return "completed".equals(status) || "error".equals(status) || "failed".equals(status)
                || "aborted".equals(status);
    }
}
