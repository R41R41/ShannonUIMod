package com.shannon.network.packet;

import java.util.List;

/**
 * 反応設定の状態
 */
public class ReactionSettingsState {
    public List<ReactionConfig> reactions;
    public List<ConstantSkillConfig> constantSkills;

    /**
     * 反応イベントの設定
     */
    public static class ReactionConfig {
        public String eventType;
        public boolean enabled;
        public int probability;
        public boolean idleOnly;
        public String reactionType;

        public ReactionConfig() {
        }

        public ReactionConfig(String eventType, boolean enabled, int probability, boolean idleOnly,
                String reactionType) {
            this.eventType = eventType;
            this.enabled = enabled;
            this.probability = probability;
            this.idleOnly = idleOnly;
            this.reactionType = reactionType;
        }
    }

    /**
     * 常時スキルの設定
     */
    public static class ConstantSkillConfig {
        public String skillName;
        public boolean enabled;
        public String description;

        public ConstantSkillConfig() {
        }

        public ConstantSkillConfig(String skillName, boolean enabled, String description) {
            this.skillName = skillName;
            this.enabled = enabled;
            this.description = description;
        }
    }
}
