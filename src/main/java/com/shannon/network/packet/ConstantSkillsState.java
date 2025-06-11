package com.shannon.network.packet;

import java.util.List;

public class ConstantSkillsState {
    public List<ConstantSkill> skills;

    public static class ConstantSkill {
        public String skillName;
        public String description;
        public String status;
    }
}