package com.shannon.network.packet;

import java.util.ArrayList;
import java.util.List;

/**
 * 進捗（アドバンスメント）の達成状況データ
 */
public class AdvancementsState {
    public String playerName = "";
    public List<Category> categories = new ArrayList<>();
    public long updatedAt = 0;

    public static class Category {
        public String categoryId = "";
        public String displayName = "";
        public int completed = 0;
        public int total = 0;
        public List<Advancement> advancements = new ArrayList<>();
    }

    public static class Advancement {
        public String title = "";
        public String description = "";
        public boolean done = false;
        public String progress = ""; // "3/10" or ""
    }
}
