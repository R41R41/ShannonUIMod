package com.shannon.ui.state;

import com.shannon.model.ChatState;
import com.shannon.model.TaskListState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The client's copy of everything the server syncs, plus what the UI derives from it.
 *
 * <p>Used only on the client thread. Views read from here every frame; they never keep their own
 * copies, so there is one source of truth.
 */
public final class ClientStore {
    private final Map<SyncChannel<?>, Object> states = new HashMap<>();
    private final List<Consumer<SyncChannel<?>>> listeners = new ArrayList<>();
    private BotStatus status = BotStatus.IDLE;

    /** The newest bot message the player has not seen yet in the speech bubble. */
    private ChatState.Message speech;
    private long speechAt;
    private long lastSeenTimestamp;
    private int unread;

    public <T> T get(SyncChannel<T> channel) {
        return channel.type().cast(states.get(channel));
    }

    public BotStatus status() {
        return status;
    }

    public ChatState.Message speech() {
        return speech;
    }

    public long speechAt() {
        return speechAt;
    }

    public int unread() {
        return unread;
    }

    public void markRead() {
        unread = 0;
    }

    /** Calls {@code listener} after every update, with the channel that changed. */
    public void listen(Consumer<SyncChannel<?>> listener) {
        listeners.add(listener);
    }

    public <T> void put(SyncChannel<T> channel, T state) {
        ChatState previousChat = channel == StateChannels.CHAT ? get(StateChannels.CHAT) : null;
        states.put(channel, state);
        if (channel == StateChannels.TASK_TREE || channel == StateChannels.TASK_LIST) {
            status = BotStatus.of(get(StateChannels.TASK_TREE), get(StateChannels.TASK_LIST));
        }
        if (channel == StateChannels.CHAT) {
            trackNewMessages(previousChat == null, (ChatState) state);
        }
        for (Consumer<SyncChannel<?>> listener : List.copyOf(listeners)) {
            listener.accept(channel);
        }
    }

    /** Forgets everything; called when leaving a server. */
    public void clear() {
        states.clear();
        status = BotStatus.IDLE;
        speech = null;
        unread = 0;
        lastSeenTimestamp = 0;
    }

    private void trackNewMessages(boolean firstSync, ChatState chat) {
        if (chat == null || chat.messages == null) {
            return;
        }
        long newest = lastSeenTimestamp;
        for (ChatState.Message message : chat.messages) {
            if (message.timestamp <= lastSeenTimestamp) {
                continue;
            }
            newest = Math.max(newest, message.timestamp);
            // History that arrives on join is not news.
            if (!firstSync && message.fromBot()) {
                speech = message;
                speechAt = System.currentTimeMillis();
                unread++;
            }
        }
        lastSeenTimestamp = newest;
    }

    public TaskTreeState taskTree() {
        return get(StateChannels.TASK_TREE);
    }

    public TaskListState taskList() {
        return get(StateChannels.TASK_LIST);
    }
}
