package com.shannon.ui.state;

import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;

/**
 * Whether the bot has something to show the player right now: a question, a failure, danger,
 * or a change in what it does. The compact status card grows while this is true.
 */
public final class Attention {
    /** How long a change keeps the card large. */
    private static final long CHANGE_MS = 8_000;

    private final ClientStore store;
    private final DangerWatch danger;
    /** Empty until the first state arrives, so what the bot is doing on joining shows for a moment. */
    private String lastSignature = "";
    private long until;

    public Attention(ClientStore store, DangerWatch danger) {
        this.store = store;
        this.danger = danger;
        store.onClear(this::clear);
        store.listen(channel -> {
            if (channel == StateChannels.TASK_TREE || channel == StateChannels.TASK_LIST) {
                noteChange();
            }
        });
    }

    private void noteChange() {
        TaskTreeState tree = store.taskTree();
        String signature = store.status() + "|" + (tree == null ? "" : tree.goal) + "|" + TaskView.currentAction(tree);
        if (!signature.equals(lastSignature)) {
            until = System.currentTimeMillis() + CHANGE_MS;
        }
        lastSignature = signature;
    }

    /** Forgets everything; called when leaving a server. */
    public void clear() {
        lastSignature = "";
        until = 0;
    }

    /** Something just changed. */
    public boolean changed() {
        return System.currentTimeMillis() < until;
    }

    /** The player is needed: a question, a failure or danger. */
    public boolean needed() {
        BotStatus status = store.status();
        return status == BotStatus.WAITING || status == BotStatus.ERROR || danger.inDanger();
    }
}
