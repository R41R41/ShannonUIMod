package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.TaskTreeState;
import com.shannon.state.StateManager;
import com.shannon.sync.SyncJson;

/** {@code POST /task}: the task the bot is working on. */
public class TaskEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        TaskTreeState state = SyncJson.GSON.fromJson(body, TaskTreeState.class);
        if (state == null) {
            throw new IllegalArgumentException("empty task");
        }
        StateManager.getInstance().updateTaskTree(state);
    }
}
