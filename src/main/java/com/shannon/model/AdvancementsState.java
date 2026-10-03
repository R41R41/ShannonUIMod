package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The bot's advancements, grouped by tab, with the layout vanilla computes for the tree.
 *
 * <p>Titles are sent as a translation key when the client can resolve one, and as plain text
 * otherwise, so the client shows vanilla advancements in the player's own language.
 */
public class AdvancementsState {
    public String playerName = "";
    public long updatedAt;
    public List<Category> categories = new ArrayList<>();

    public static class Category {
        /** Root advancement id such as {@code minecraft:story/root}. */
        public String rootId = "";
        public String title = "";
        public String titleKey;
        public String icon;
        public int completed;
        public int total;
        public List<Advancement> advancements = new ArrayList<>();
    }

    public static class Advancement {
        public String id = "";
        public String parentId;
        public String title = "";
        public String titleKey;
        public String description = "";
        public String descriptionKey;
        /** Registry id of the icon item. */
        public String icon;
        /** task, goal or challenge. */
        public String frame = "task";
        public boolean done;
        /** "3/10" while partly done, otherwise empty. */
        public String progress = "";
        /** Tree position in vanilla grid units. */
        public float x;
        public float y;
    }
}
