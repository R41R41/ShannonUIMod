package com.shannon.http.endpoints;

import com.shannon.http.JsonPostEndpoint;
import com.shannon.model.VoiceState;
import com.shannon.server.ServerSync;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * {@code POST /voice_transcript}: what the bot heard on voice, as
 * {@code {"text": "...", "speaker": "...", "mcUsername": "...", "mode": "minebot"}}.
 *
 * <p>Goes only to the online player named {@code mcUsername}, and is dropped when there is none,
 * so nobody sees what someone else said. Their client shows it only if they were just talking.
 */
public class VoiceTranscriptEndpoint extends JsonPostEndpoint {
    private static final int MAX_TEXT = 512;

    private static final class Request {
        String text;
        String mcUsername;
        String mode;
    }

    @Override
    protected void accept(String body) {
        Request request = SyncJson.GSON.fromJson(body, Request.class);
        if (request == null || request.text == null || request.text.isBlank()
                || request.mcUsername == null || request.mcUsername.isBlank()) {
            throw new IllegalArgumentException("text and mcUsername are required");
        }
        MinecraftServer server = StateManager.getInstance().getServer();
        if (server == null) {
            return;
        }
        VoiceState state = new VoiceState(VoiceState.TRANSCRIPT);
        String text = request.text.strip();
        state.text = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        state.speaker = request.mcUsername;
        state.mode = request.mode;
        server.execute(() -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(request.mcUsername);
            if (player != null) {
                ServerSync.send(player, StateChannels.VOICE, state);
            }
        });
    }
}
