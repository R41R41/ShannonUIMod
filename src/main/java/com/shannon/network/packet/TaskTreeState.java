package com.shannon.network.packet;

import java.util.List;

public class TaskTreeState {
    public String goal;
    public String strategy;
    public String status;
    public String error;
    public String currentThinking;

    // 階層的サブタスク（新形式）
    public List<HierarchicalSubTask> hierarchicalSubTasks;
    public String currentSubTaskId;

    // 旧形式（後方互換性）
    public List<SubTask> subTasks;

    // メタ認知状態（MetaCognitionLoopから）
    public MetaStateData metaState;

    // 感情状態（EmotionLoopから）
    public EmotionData emotionState;

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

    /**
     * MetaCognitionLoop から送られるメタ認知状態
     * on_track / struggling / stuck / wrong_approach の4段階評価
     */
    public static class MetaStateData {
        /** on_track / struggling / stuck / wrong_approach */
        public String assessment;
        /** メタ認知からの改善提案 */
        public String suggestion;
        /** escalate / deescalate / hold */
        public String modelAction;
        /** 連続成功回数 */
        public int consecutiveSuccesses;
        /** 連続失敗回数 */
        public int consecutiveFailures;

        /**
         * assessmentに応じたUIカラーを返す
         * on_track=緑、struggling=黄、stuck=橙、wrong_approach=赤
         */
        public int getAssessmentColor() {
            if (assessment == null) return 0xFFAAAAAA;
            switch (assessment.toLowerCase()) {
                case "on_track":     return 0xFF55FF55;
                case "struggling":   return 0xFFFFFF55;
                case "stuck":        return 0xFFFFAA33;
                case "wrong_approach": return 0xFFFF5555;
                default:             return 0xFFAAAAAA;
            }
        }

        /**
         * assessmentに応じたアイコンを返す
         */
        public String getAssessmentIcon() {
            if (assessment == null) return "[?]";
            switch (assessment.toLowerCase()) {
                case "on_track":     return "[OK]";
                case "struggling":   return "[??]";
                case "stuck":        return "[!!]";
                case "wrong_approach": return "[NG]";
                default:             return "[--]";
            }
        }
    }

    /**
     * EmotionLoop から送られるPlutchik感情状態（8パラメータ）
     */
    public static class EmotionData {
        /** 感情ラベル（例: "joy", "anticipation"） */
        public String emotion;
        /** Plutchik 8パラメータ */
        public EmotionParameters parameters;

        public static class EmotionParameters {
            public int joy;
            public int trust;
            public int fear;
            public int surprise;
            public int sadness;
            public int disgust;
            public int anger;
            public int anticipation;
        }

        /**
         * 支配的感情に応じたUIカラーヒントを返す（サブタイル・テーマ色）
         * ポジティブ感情=暖かい色、ネガティブ=冷たい色
         */
        public int getEmotionTint() {
            if (emotion == null || parameters == null) return 0xFFCCCCCC;
            switch (emotion.toLowerCase()) {
                case "joy":          return 0xFFFFFF88;
                case "trust":        return 0xFF88FF88;
                case "anticipation": return 0xFFFFCC44;
                case "surprise":     return 0xFF88CCFF;
                case "fear":         return 0xFFAA88FF;
                case "sadness":      return 0xFF8888FF;
                case "disgust":      return 0xFF88AA44;
                case "anger":        return 0xFFFF6644;
                default:             return 0xFFCCCCCC;
            }
        }
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
                ", assessment=" + (metaState != null ? metaState.assessment : "null") +
                ", emotion=" + (emotionState != null ? emotionState.emotion : "null") +
                '}';
    }
}
