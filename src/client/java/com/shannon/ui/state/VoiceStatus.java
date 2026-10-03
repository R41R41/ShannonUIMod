package com.shannon.ui.state;

import com.shannon.model.VoiceState;
import com.shannon.sync.StateChannels;

/** The local player's push-to-talk: whether the key is held and what the bot answered. */
public final class VoiceStatus {
    /** How long after letting go a transcript is still taken as this player's. */
    private static final long TRANSCRIPT_WINDOW_MS = 20_000;

    private boolean holding;
    private long pressedAt;
    private long releasedAt;
    private VoiceState event;
    private long eventAt;

    public VoiceStatus(ClientStore store) {
        store.onClear(() -> {
            holding = false;
            event = null;
        });
        store.listen(channel -> {
            if (channel == StateChannels.VOICE) {
                receive(store.get(StateChannels.VOICE));
            }
        });
    }

    private void receive(VoiceState state) {
        if (state == null || state.event == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // Words this player spoke from Discord without the key mean nothing here.
        if (VoiceState.TRANSCRIPT.equals(state.event) && !holding && now - releasedAt > TRANSCRIPT_WINDOW_MS) {
            return;
        }
        event = state;
        eventAt = now;
    }

    public void press() {
        holding = true;
        pressedAt = System.currentTimeMillis();
        event = null;
    }

    public void release() {
        holding = false;
        releasedAt = System.currentTimeMillis();
    }

    public boolean holding() {
        return holding;
    }

    public long pressedAt() {
        return pressedAt;
    }

    public long releasedAt() {
        return releasedAt;
    }

    /** The bot's latest answer since the key was last pressed, or {@code null}. */
    public VoiceState event() {
        return event;
    }

    public long eventAt() {
        return eventAt;
    }
}
