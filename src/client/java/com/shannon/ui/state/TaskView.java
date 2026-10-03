package com.shannon.ui.state;

import com.shannon.model.ChatState;
import com.shannon.model.TaskTreeState;

import java.util.ArrayList;
import java.util.List;

/** Read-only views over the task and the conversation that several screens share. */
public final class TaskView {
    private TaskView() {
    }

    /** One step of the task tree with its nesting depth. */
    public record Step(TaskTreeState.SubTask task, int depth) {
        public boolean done() {
            return "completed".equals(task.status);
        }

        public boolean running() {
            return "in_progress".equals(task.status);
        }

        public boolean failed() {
            return "error".equals(task.status) || "failed".equals(task.status);
        }
    }

    /** Every step, depth first, in the order the bot plans them. */
    public static List<Step> steps(TaskTreeState tree) {
        List<Step> steps = new ArrayList<>();
        if (tree != null && tree.hierarchicalSubTasks != null) {
            for (TaskTreeState.SubTask task : tree.hierarchicalSubTasks) {
                collect(task, 0, steps);
            }
        }
        return steps;
    }

    private static void collect(TaskTreeState.SubTask task, int depth, List<Step> out) {
        if (task == null) {
            return;
        }
        out.add(new Step(task, depth));
        if (task.children != null) {
            for (TaskTreeState.SubTask child : task.children) {
                collect(child, depth + 1, out);
            }
        }
    }

    /** {done, total} over all steps; total is 0 when the task has no steps. */
    public static int[] progress(TaskTreeState tree) {
        int done = 0;
        int total = 0;
        for (Step step : steps(tree)) {
            total++;
            if (step.done()) {
                done++;
            }
        }
        return new int[]{done, total};
    }

    /** What the bot is doing at this moment, in a few words, or {@code null}. */
    public static String currentAction(TaskTreeState tree) {
        if (tree == null) {
            return null;
        }
        List<Step> steps = steps(tree);
        if (tree.currentSubTaskId != null) {
            for (Step step : steps) {
                if (tree.currentSubTaskId.equals(step.task().id)) {
                    return step.task().goal;
                }
            }
        }
        for (Step step : steps) {
            if (step.running()) {
                return step.task().goal;
            }
        }
        return blankToNull(tree.strategy);
    }

    /** The newest thing the bot said, or {@code null}. */
    public static ChatState.Message lastBotMessage(ChatState chat) {
        if (chat == null || chat.messages == null) {
            return null;
        }
        for (int i = chat.messages.size() - 1; i >= 0; i--) {
            ChatState.Message message = chat.messages.get(i);
            if (message.fromBot()) {
                return message;
            }
        }
        return null;
    }

    public static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
