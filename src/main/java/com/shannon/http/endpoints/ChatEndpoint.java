package com.shannon.http.endpoints;

import com.google.gson.reflect.TypeToken;
import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.ChatState;
import com.shannon.state.StateManager;
import com.shannon.sync.SyncJson;

import java.util.List;

/** {@code POST /chat}: replaces the whole conversation with a JSON array of messages. */
public class ChatEndpoint extends JsonPostEndpoint {
    @Override
    protected void accept(String body) {
        List<ChatState.Message> messages = SyncJson.GSON.fromJson(body,
                new TypeToken<List<ChatState.Message>>() {
                }.getType());
        if (messages == null) {
            throw new IllegalArgumentException("empty conversation");
        }
        StateManager.getInstance().replaceChat(messages);
    }
}
