package com.shannon.state;

import com.shannon.model.ChatState;
import com.shannon.model.DetailedLogsState;
import com.shannon.model.TaskTreeState;
import com.shannon.server.ServerSync;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's copy of every synced state.
 *
 * <p>The HTTP endpoints publish from their own threads; broadcasting always happens on the server
 * thread. A player who joins receives the latest copy of every state.
 */
public final class StateManager {
    private static final StateManager INSTANCE = new StateManager();

    private static final int MAX_CHAT_MESSAGES = 100;
    private static final int MAX_LOG_ENTRIES = 100;
    /** Log lines in one sync; the newest are kept. */
    private static final int LOGS_PER_SYNC = 25;
    private static final long LOGS_SYNC_INTERVAL_MS = 1_000;

    private final Map<SyncChannel<?>, Object> latest = new ConcurrentHashMap<>();
    private volatile MinecraftServer server;

    private final Object chatLock = new Object();
    private final List<ChatState.Message> chat = new ArrayList<>();

    private final Object logsLock = new Object();
    private final List<DetailedLogsState.LogEntry> logs = new ArrayList<>();
    private boolean logsDirty;
    private long lastLogsSync;

    /** What the last task-tree update looked like, to tell when a task starts or ends. */
    private String lastGoal;
    private String lastOutcome;

    private StateManager() {
    }

    public static StateManager getInstance() {
        return INSTANCE;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    // ===== Generic publish / read =====

    /** Stores {@code state} and sends it to every client. Safe from any thread. */
    public <T> void publish(SyncChannel<T> channel, T state) {
        if (state == null) {
            return;
        }
        latest.put(channel, state);
        MinecraftServer current = server;
        if (current != null) {
            current.execute(() -> ServerSync.broadcast(current, channel, state));
        }
    }

    public <T> T get(SyncChannel<T> channel) {
        return channel.type().cast(latest.get(channel));
    }

    /** Sends every stored state to one player. Call on the server thread. */
    public void sendAllTo(ServerPlayerEntity player) {
        for (SyncChannel<?> channel : StateChannels.ALL) {
            sendStored(player, channel);
        }
    }

    private <T> void sendStored(ServerPlayerEntity player, SyncChannel<T> channel) {
        T state = get(channel);
        if (state != null) {
            ServerSync.send(player, channel, state);
        }
    }

    // ===== Task tree, with task events in the conversation =====

    public void updateTaskTree(TaskTreeState state) {
        if (state == null) {
            return;
        }
        publish(StateChannels.TASK_TREE, state);
        recordTaskEvent(state);
    }

    private synchronized void recordTaskEvent(TaskTreeState state) {
        String goal = state.goal;
        if (goal == null || goal.isBlank()) {
            return;
        }
        if (!goal.equals(lastGoal)) {
            lastGoal = goal;
            lastOutcome = null;
            addChat(new ChatState.Message(ChatState.Kind.TASK_STARTED, null, goal, System.currentTimeMillis()));
        }
        String status = state.status == null ? "" : state.status;
        ChatState.Kind outcome = switch (status) {
            case "completed" -> ChatState.Kind.TASK_DONE;
            case "error", "failed", "aborted" -> ChatState.Kind.TASK_ERROR;
            default -> null;
        };
        if (outcome != null && !Objects.equals(lastOutcome, outcome.name())) {
            lastOutcome = outcome.name();
            String text = outcome == ChatState.Kind.TASK_ERROR && state.error != null && !state.error.isBlank()
                    ? state.error
                    : goal;
            addChat(new ChatState.Message(outcome, null, text, System.currentTimeMillis()));
        }
    }

    // ===== Conversation =====

    public void addChat(ChatState.Message message) {
        ChatState snapshot = new ChatState();
        synchronized (chatLock) {
            chat.add(message);
            while (chat.size() > MAX_CHAT_MESSAGES) {
                chat.remove(0);
            }
            snapshot.messages = new ArrayList<>(chat);
        }
        publish(StateChannels.CHAT, snapshot);
    }

    public void replaceChat(List<ChatState.Message> messages) {
        ChatState snapshot = new ChatState();
        synchronized (chatLock) {
            chat.clear();
            chat.addAll(messages);
            while (chat.size() > MAX_CHAT_MESSAGES) {
                chat.remove(0);
            }
            snapshot.messages = new ArrayList<>(chat);
        }
        publish(StateChannels.CHAT, snapshot);
    }

    // ===== Developer logs, throttled =====

    public void appendLogs(List<DetailedLogsState.LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        synchronized (logsLock) {
            logs.addAll(entries);
            while (logs.size() > MAX_LOG_ENTRIES) {
                logs.remove(0);
            }
            logsDirty = true;
        }
    }

    public void clearLogs() {
        synchronized (logsLock) {
            logs.clear();
            logsDirty = true;
            lastLogsSync = 0;
        }
    }

    /** Called every server tick. Sends the newest log lines at most once a second. */
    public void tick() {
        DetailedLogsState snapshot = null;
        synchronized (logsLock) {
            long now = System.currentTimeMillis();
            if (logsDirty && now - lastLogsSync >= LOGS_SYNC_INTERVAL_MS) {
                logsDirty = false;
                lastLogsSync = now;
                snapshot = new DetailedLogsState();
                int from = Math.max(0, logs.size() - LOGS_PER_SYNC);
                snapshot.logs = new ArrayList<>(logs.subList(from, logs.size()));
            }
        }
        if (snapshot != null) {
            publish(StateChannels.LOGS, snapshot);
        }
    }
}
