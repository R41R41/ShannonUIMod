package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * How far the bot has got on the way to the Ender Dragon in this world: when it first reached each
 * milestone, and how often it died.
 *
 * <p>Times are the overworld's game time in ticks. Game time stops while the server is down and is
 * not changed by {@code /time set}, so the elapsed times count play only, and the client measures
 * them against its own synced world time without trusting either machine's clock.
 */
public class JourneyState {
    /** The milestones in the order the way usually goes. The first ten are the bot backend's own list. */
    public static final List<Milestone> MILESTONES = List.of(
            new Milestone("wooden_pickaxe", "minecraft:wooden_pickaxe"),
            new Milestone("stone_pickaxe", "minecraft:stone_pickaxe"),
            new Milestone("stone_axe", "minecraft:stone_axe"),
            new Milestone("bed", "minecraft:red_bed"),
            new Milestone("iron_pickaxe", "minecraft:iron_pickaxe"),
            new Milestone("bucket", "minecraft:bucket"),
            new Milestone("iron_armor", "minecraft:iron_chestplate"),
            new Milestone("obsidian", "minecraft:obsidian"),
            new Milestone("nether_portal", "minecraft:netherrack"),
            new Milestone("blaze_rod", "minecraft:blaze_rod"),
            new Milestone("ender_pearl", "minecraft:ender_pearl"),
            new Milestone("ender_eye", "minecraft:ender_eye"),
            new Milestone("the_end", "minecraft:end_stone"),
            new Milestone("dragon", "minecraft:dragon_head"));

    /** Game time when the bot was first seen in this world. */
    public long startTick = -1;
    public List<Mark> marks = new ArrayList<>();
    public int deaths;
    /** Game time of the latest death, or -1. */
    public long lastDeathTick = -1;

    /** One milestone of the closed list; {@code icon} is the item drawn for it. */
    public record Milestone(String id, String icon) {
    }

    /** A milestone reached, and when. */
    public static class Mark {
        public String id;
        public long tick;

        public Mark() {
        }

        public Mark(String id, long tick) {
            this.id = id;
            this.tick = tick;
        }
    }

    /** When {@code id} was first reached, or -1. */
    public long reachedAt(String id) {
        for (Mark mark : marks) {
            if (mark.id.equals(id)) {
                return mark.tick;
            }
        }
        return -1;
    }
}
