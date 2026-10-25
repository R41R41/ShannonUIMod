package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The bot's inventory, slot by slot, read from the bot's server player.
 *
 * <p>{@link #main} holds the 36 main slots in vanilla order: 0 to 8 are the hotbar and 9 to 35
 * are the rows above it. An empty slot is {@code null}.
 */
public class InventoryState {
    public List<Stack> main = new ArrayList<>();
    public Stack head;
    public Stack chest;
    public Stack legs;
    public Stack feet;
    public Stack offHand;
    public int selectedSlot;

    /**
     * One stack. {@code item} is a registry id such as {@code minecraft:oak_log}.
     *
     * <p>{@code maxDamage} is 0 for an item that does not wear out; otherwise {@code damage} is how
     * much of it is used up, as vanilla counts it.
     */
    public static class Stack {
        public String item;
        public int count;
        public int damage;
        public int maxDamage;
        public boolean enchanted;
        /** The name given on an anvil, or {@code null}. */
        public String name;

        public Stack() {
        }

        public Stack(String item, int count) {
            this.item = item;
            this.count = count;
        }

        /** What is left of it, from 0 to 1; 1 for an item that does not wear out. */
        public float durability() {
            return maxDamage <= 0 ? 1f : Math.max(0f, (maxDamage - damage) / (float) maxDamage);
        }
    }
}
