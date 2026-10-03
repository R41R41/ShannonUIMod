package com.shannon.ui.state;

import com.shannon.model.BotVitals;
import com.shannon.model.ChatState;
import com.shannon.model.InventoryState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A log of the bot's recent tasks, written on the client while the player plays: when each task
 * started and ended, what the bot picked up, what it asked, and when it was in danger.
 *
 * <p>Built only from state the client already receives, so it needs nothing from the backend.
 * It lasts until the player leaves the server.
 */
public final class TaskHistory {
    private static final int MAX_RECORDS = 10;
    private static final int MAX_EVENTS = 60;
    /** Pick-ups of the same item this close together become one line. */
    private static final long MERGE_MS = 30_000;
    /** Something the player said this recently when a task starts is taken as its request. */
    private static final long REQUEST_WINDOW_MS = 120_000;
    /** Danger is logged at most this often. */
    private static final long DANGER_GAP_MS = 15_000;
    /** Longer jumps between two positions are teleports, not walking. */
    private static final double MAX_STEP = 64;

    public enum Outcome {
        RUNNING, DONE, FAILED
    }

    public enum Kind {
        STARTED, GAINED, QUESTION, ANSWER, DANGER, DONE, FAILED
    }

    /** One line of a task's log. {@code item} and {@code count} are set for {@link Kind#GAINED}. */
    public static final class Event {
        public final long at;
        public final Kind kind;
        public final String text;
        public final String item;
        public int count;

        Event(long at, Kind kind, String text, String item, int count) {
            this.at = at;
            this.kind = kind;
            this.text = text;
            this.item = item;
            this.count = count;
        }
    }

    /** One task from start to end. */
    public static final class Record {
        public final String goal;
        /** What the player said to start it, or {@code null} when it is not known. */
        public final String request;
        public final long startedAt;
        public long endedAt;
        public Outcome outcome = Outcome.RUNNING;
        public final List<Event> events = new ArrayList<>();
        /** Item id to count, in the order first picked up. */
        public final Map<String, Integer> gained = new LinkedHashMap<>();
        public double walked;
        public int questions;
        public int dangers;

        Record(String goal, String request, long startedAt) {
            this.goal = goal;
            this.request = request;
            this.startedAt = startedAt;
        }

        /** The words to send to ask for this task again. */
        public String repeatRequest() {
            return request != null ? request : goal;
        }

        public long duration() {
            return (outcome == Outcome.RUNNING ? System.currentTimeMillis() : endedAt) - startedAt;
        }
    }

    private final ClientStore store;
    private final DangerWatch danger;
    private final Deque<Record> records = new ArrayDeque<>();
    private Map<String, Integer> lastInventory;
    private double[] lastPosition;
    private String lastDimension;
    private long lastChatTimestamp = -1;
    private long lastDangerAt;
    private String lastSent;
    private long lastSentAt;
    private BotStatus lastStatus = BotStatus.IDLE;

    public TaskHistory(ClientStore store, DangerWatch danger) {
        this.store = store;
        this.danger = danger;
        store.onClear(this::clear);
        store.listen(channel -> {
            if (channel == StateChannels.TASK_TREE || channel == StateChannels.TASK_LIST) {
                onTask();
            } else if (channel == StateChannels.INVENTORY) {
                onInventory(store.get(StateChannels.INVENTORY));
            } else if (channel == StateChannels.CHAT) {
                onChat(store.get(StateChannels.CHAT));
            } else if (channel == StateChannels.VITALS) {
                onVitals(store.get(StateChannels.VITALS));
            }
        });
    }

    /** Newest first. */
    public List<Record> records() {
        return List.copyOf(records);
    }

    /** Forgets everything; called when leaving a server. */
    public void clear() {
        records.clear();
        lastInventory = null;
        lastPosition = null;
        lastChatTimestamp = -1;
        lastStatus = BotStatus.IDLE;
        lastSent = null;
    }

    /**
     * Notes something this player said to the bot: an answer while it waits, otherwise maybe the
     * request for the next task. Called by whatever sends it, with this machine's clock.
     */
    public void noteSent(String text) {
        long now = System.currentTimeMillis();
        Record current = current();
        if (current != null && store.status() == BotStatus.WAITING) {
            add(current, new Event(now, Kind.ANSWER, text, null, 0));
        } else {
            lastSent = text;
            lastSentAt = now;
        }
    }

    private Record current() {
        Record first = records.peekFirst();
        return first != null && first.outcome == Outcome.RUNNING ? first : null;
    }

    private void onTask() {
        BotStatus status = store.status();
        TaskTreeState tree = store.taskTree();
        String goal = tree != null ? TaskView.blankToNull(tree.goal) : null;
        long now = System.currentTimeMillis();
        Record current = current();
        boolean active = status == BotStatus.WORKING || status == BotStatus.WAITING;
        if (active && goal != null && (current == null || !goal.equals(current.goal))) {
            if (current != null) {
                finish(current, Outcome.DONE, now);
            }
            Record record = new Record(goal, recentRequest(now), now);
            record.events.add(new Event(now, Kind.STARTED, goal, null, 0));
            records.addFirst(record);
            while (records.size() > MAX_RECORDS) {
                records.removeLast();
            }
        } else if (current != null && !active && lastStatus != status) {
            finish(current, status == BotStatus.ERROR ? Outcome.FAILED : Outcome.DONE, now);
        }
        Record running = current();
        if (running != null && status == BotStatus.WAITING && lastStatus != BotStatus.WAITING) {
            // The question usually arrives just before the task says it waits.
            ChatState.Message asked = TaskView.lastBotMessage(store.get(StateChannels.CHAT));
            if (asked != null) {
                addQuestion(running, asked.message, now);
            }
        }
        lastStatus = status;
    }

    private void finish(Record record, Outcome outcome, long now) {
        record.outcome = outcome;
        record.endedAt = now;
        add(record, new Event(now, outcome == Outcome.FAILED ? Kind.FAILED : Kind.DONE, null, null, 0));
    }

    private String recentRequest(long now) {
        return lastSent != null && now - lastSentAt <= REQUEST_WINDOW_MS ? lastSent : null;
    }

    private void onInventory(InventoryState inventory) {
        Map<String, Integer> counts = counts(inventory);
        Record current = current();
        if (current != null && lastInventory != null) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int gained = entry.getValue() - lastInventory.getOrDefault(entry.getKey(), 0);
                if (gained > 0) {
                    current.gained.merge(entry.getKey(), gained, Integer::sum);
                    Event last = current.events.isEmpty() ? null : current.events.get(current.events.size() - 1);
                    if (last != null && last.kind == Kind.GAINED && entry.getKey().equals(last.item)
                            && now - last.at < MERGE_MS) {
                        last.count += gained;
                    } else {
                        add(current, new Event(now, Kind.GAINED, null, entry.getKey(), gained));
                    }
                }
            }
        }
        lastInventory = counts;
    }

    private static Map<String, Integer> counts(InventoryState inventory) {
        Map<String, Integer> counts = new HashMap<>();
        if (inventory == null || inventory.main == null) {
            return counts;
        }
        for (InventoryState.Stack stack : inventory.main) {
            if (stack != null && stack.item != null) {
                counts.merge(stack.item, stack.count, Integer::sum);
            }
        }
        return counts;
    }

    private void onChat(ChatState chat) {
        if (chat == null || chat.messages == null) {
            return;
        }
        long newest = lastChatTimestamp;
        Record current = current();
        for (ChatState.Message message : chat.messages) {
            if (message.timestamp <= lastChatTimestamp) {
                continue;
            }
            newest = Math.max(newest, message.timestamp);
            // History that arrives on join is not news.
            if (lastChatTimestamp >= 0 && current != null && message.fromBot()
                    && store.status() == BotStatus.WAITING) {
                addQuestion(current, message.message, System.currentTimeMillis());
            }
        }
        lastChatTimestamp = Math.max(newest, 0);
    }

    private void onVitals(BotVitals vitals) {
        if (vitals == null || !vitals.online) {
            lastPosition = null;
            return;
        }
        Record current = current();
        double[] position = {vitals.x, vitals.y, vitals.z};
        if (current != null && lastPosition != null && vitals.dimension != null
                && vitals.dimension.equals(lastDimension)) {
            double dx = position[0] - lastPosition[0];
            double dy = position[1] - lastPosition[1];
            double dz = position[2] - lastPosition[2];
            double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (step < MAX_STEP) {
                current.walked += step;
            }
        }
        lastPosition = position;
        lastDimension = vitals.dimension;
        long now = System.currentTimeMillis();
        if (current != null && danger.recentlyHurt() && now - lastDangerAt > DANGER_GAP_MS) {
            lastDangerAt = now;
            current.dangers++;
            add(current, new Event(now, Kind.DANGER, danger.describe().getString(), null, 0));
        }
    }

    private static void addQuestion(Record record, String text, long now) {
        for (int i = record.events.size() - 1; i >= 0; i--) {
            Event event = record.events.get(i);
            if (event.kind == Kind.QUESTION) {
                if (event.text != null && event.text.equals(text)) {
                    return;
                }
                break;
            }
        }
        record.questions++;
        add(record, new Event(now, Kind.QUESTION, text, null, 0));
    }

    private static void add(Record record, Event event) {
        record.events.add(event);
        while (record.events.size() > MAX_EVENTS) {
            record.events.remove(1);
        }
    }
}
