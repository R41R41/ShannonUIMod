package com.shannon.network.packet;

import java.util.List;

public class TaskTreeState {
    public String goal;
    public String strategy;
    public String status;
    public String error;

    // 階層的サブタスク（新形式）
    public List<HierarchicalSubTask> hierarchicalSubTasks;
    public String currentSubTaskId;

    // 旧形式（後方互換性）
    public List<SubTask> subTasks;

    // 階層的サブタスク構造
    public static class HierarchicalSubTask {
        public String id;
        public String goal;
        public String strategy;
        public String status; // pending, in_progress, completed, error
        public String result;
        public String failureReason;
        public int depth;
        public String parentId;
        public List<HierarchicalSubTask> children;
        public boolean needsDecomposition;

        // ステータスアイコンを取得
        public String getStatusIcon() {
            if (status == null)
                return "[ ]";
            switch (status.toLowerCase()) {
                case "completed":
                    return "[+]";
                case "in_progress":
                    return "[>]";
                case "error":
                    return "[x]";
                default:
                    return "[ ]";
            }
        }

        // ステータスカラーを取得
        public int getStatusColor() {
            if (status == null)
                return 0xAAAAAA;
            switch (status.toLowerCase()) {
                case "completed":
                    return 0x00FF00; // 緑
                case "in_progress":
                    return 0xFFFF00; // 黄
                case "error":
                    return 0xFF5555; // 赤
                default:
                    return 0xAAAAAA; // グレー
            }
        }
    }

    // 旧形式のサブタスク（後方互換性）
    public static class SubTask {
        public String subTaskGoal;
        public String subTaskStrategy;
        public String subTaskStatus;
        public String subTaskResult;
    }

    @Override
    public String toString() {
        return "TaskTreeState{" +
                "goal='" + goal + '\'' +
                ", strategy='" + strategy + '\'' +
                ", status='" + status + '\'' +
                ", error='" + error + '\'' +
                ", hierarchicalSubTasks=" + (hierarchicalSubTasks != null ? hierarchicalSubTasks.size() : 0) +
                ", subTasks=" + (subTasks != null ? subTasks.size() : 0) +
                '}';
    }
}