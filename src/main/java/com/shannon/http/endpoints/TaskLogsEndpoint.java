package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.DetailedLogsState;
import com.shannon.state.StateManager;
import com.shannon.sync.SyncJson;

/** {@code POST /task_logs}: developer log lines, appended to what the server already holds. */
public class TaskLogsEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        DetailedLogsState state = SyncJson.GSON.fromJson(body, DetailedLogsState.class);
        if (state != null) {
            StateManager.getInstance().appendLogs(state.logs);
        }
    }
}
