package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.ChatState;
import com.shannon.state.StateManager;
import com.shannon.sync.SyncJson;

/** {@code POST /bot_chat}: something the bot said, as {@code {"message": "..."}}. */
public class BotChatEndpoint extends JsonPostEndpoint {
    private static final class Request {
        String message;
    }

    @Override
    protected void accept(String body) {
        Request request = SyncJson.GSON.fromJson(body, Request.class);
        if (request == null || request.message == null || request.message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        StateManager.getInstance().addChat(new ChatState.Message(
                ChatState.Kind.CHAT, ChatState.BOT_SENDER, request.message, System.currentTimeMillis()));
    }
}
