package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.ReactionSettingsState;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;

/** {@code POST /reaction_settings}: how often the bot speaks up, and its hostile detection distances. */
public class ReactionSettingsEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        ReactionSettingsState state = SyncJson.GSON.fromJson(body, ReactionSettingsState.class);
        if (state == null) {
            throw new IllegalArgumentException("empty settings");
        }
        StateManager.getInstance().publish(StateChannels.REACTIONS, state);
    }
}
