package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.TaskListState;
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
        StateManager.getInstance().publish(StateChannels.TASK_LIST, state);
    }
}
