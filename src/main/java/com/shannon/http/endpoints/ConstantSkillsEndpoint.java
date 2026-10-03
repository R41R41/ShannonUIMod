package com.shannon.http.endpoints;

import com.google.gson.reflect.TypeToken;
import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.ConstantSkillsState;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;

import java.util.ArrayList;
import java.util.List;

/** {@code POST /constant_skills}: a JSON array of the bot's always-on skills. */
public class ConstantSkillsEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        List<ConstantSkillsState.Skill> skills = SyncJson.GSON.fromJson(body,
                new TypeToken<List<ConstantSkillsState.Skill>>() {
                }.getType());
        ConstantSkillsState state = new ConstantSkillsState();
        state.skills = skills != null ? skills : new ArrayList<>();
        StateManager.getInstance().publish(StateChannels.SKILLS, state);
    }
}
