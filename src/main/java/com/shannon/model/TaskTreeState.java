package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The task the bot is working on right now, as posted by the backend to {@code /task}.
 *
 * <p>Pure data: every field may be missing, and unknown JSON fields are ignored, so the backend
 * can add fields without breaking older mods.
 */
public class TaskTreeState {
    public String goal;
    public String strategy;
    /** pending, in_progress, completed, error, failed, aborted. */
    public String status;
    public String error;
    /** The first characters of the bot's latest reasoning. */
    public String currentThinking;
    /** {@code awaiting_user} while the bot waits for the player to answer. */
    public String recoveryStatus;
    public String currentSubTaskId;
    /** Short answers the bot offers while it waits for the player, or {@code null}. */
    public List<String> replyChoices;
    public List<SubTask> hierarchicalSubTasks = new ArrayList<>();

    public static class SubTask {
        public String id;
        public String goal;
        public String strategy;
        /** pending, in_progress, completed, error. */
        public String status;
        public String result;
        public String failureReason;
        public int depth;
        public String parentId;
        public List<SubTask> children = new ArrayList<>();
    }
}
