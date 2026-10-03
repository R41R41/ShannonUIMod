package com.shannon.ui.state;

import com.shannon.model.TaskListState;
import com.shannon.model.TaskTreeState;

/** The four states the status card shows, worked out from the task the bot reports. */
public enum BotStatus {
    /** Nothing to do; waiting for an order. */
    IDLE("idle"),
    /** Working on a task. */
    WORKING("working"),
    /** Asked the player something and waits for the answer. */
    WAITING("waiting"),
    /** The task failed or was stopped. */
    ERROR("error");

    private final String key;

    BotStatus(String key) {
        this.key = key;
    }

    /** Translation key of the status label. */
    public String labelKey() {
        return "shannonuimod.status." + key;
    }

    public static BotStatus of(TaskTreeState tree, TaskListState list) {
        if (isAwaitingUser(tree, list)) {
            return WAITING;
        }
        String status = tree != null && tree.status != null ? tree.status : "";
        boolean listBusy = list != null && list.currentTaskId != null;
        switch (status) {
            case "error", "failed", "aborted":
                return ERROR;
            case "in_progress", "pending", "executing":
                return WORKING;
            case "completed":
                return listBusy ? WORKING : IDLE;
            default:
                return listBusy ? WORKING : IDLE;
        }
    }

    private static boolean isAwaitingUser(TaskTreeState tree, TaskListState list) {
        if (tree != null && "awaiting_user".equals(tree.recoveryStatus)) {
            return true;
        }
        if (list == null) {
            return false;
        }
        if ("awaiting_user".equals(list.currentRecoveryStatus)) {
            return true;
        }
        if (list.currentTaskId != null && list.tasks != null) {
            for (TaskListState.TaskInfo task : list.tasks) {
                if (list.currentTaskId.equals(task.id)
                        && ("awaiting_user".equals(task.status) || "awaiting_user".equals(task.recoveryStatus))) {
                    return true;
                }
            }
        }
        return false;
    }
}
