package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/** The bot's always-on skills, as posted by the backend to {@code /constant_skills}. */
public class ConstantSkillsState {
    public List<Skill> skills = new ArrayList<>();

    public static class Skill {
        /** Backend id such as {@code auto-eat}. */
        public String skillName;
        public String description;
        public boolean status;
    }
}
