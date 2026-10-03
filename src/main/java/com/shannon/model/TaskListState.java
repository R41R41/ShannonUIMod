package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/** The bot's task queue, as posted by the backend to {@code /task_list}. */
public class TaskListState {
    public List<TaskInfo> tasks = new ArrayList<>();
    public EmergencyTask emergencyTask;
    public String currentTaskId;
    /** {@code awaiting_user} while the current task waits for the player. */
    public String currentRecoveryStatus;
    /** The runtime's view of the current task, including how it ended. */
    public TaskTreeState currentTaskTree;

    public static class TaskInfo {
        public String id;
        public String goal;
        /** pending, executing, paused, awaiting_user, failed_terminal. */
        public String status;
        public long createdAt;
        public String recoveryStatus;
        public String lastFailureType;
    }

    public static class EmergencyTask {
        public String id;
        public String goal;
        public long createdAt;
    }
}
