package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/** How often the bot speaks up on its own, as posted by the backend to {@code /reaction_settings}. */
public class ReactionSettingsState {
    public List<Reaction> reactions = new ArrayList<>();
    public HostileDetection hostileDetection;

    public static class Reaction {
        /** player_facing, hostile_approach, item_obtained, damage, weather_change, ... */
        public String eventType;
        public boolean enabled;
        /** 0 to 100. */
        public int probability;
        public boolean idleOnly;
        public String reactionType;
    }

    public static class HostileDetection {
        public double criticalDistance;
        public double detectionDistance;
        public int multiMobCriticalCount;
    }
}
