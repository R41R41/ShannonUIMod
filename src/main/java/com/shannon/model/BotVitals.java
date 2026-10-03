package com.shannon.model;

/**
 * Health, food and position of the bot's player, read on the server.
 *
 * <p>Sent from the server so that the HUD works even when the bot is outside the client's render
 * distance.
 */
public class BotVitals {
    /** The bot's player name, so the client can find its entity. */
    public String name;
    public boolean online;
    public float health;
    public float maxHealth;
    public int food;
    public int air;
    public int maxAir;
    public double x;
    public double y;
    public double z;
    /** Dimension id such as {@code minecraft:overworld}. */
    public String dimension;
    /** Biome id such as {@code minecraft:plains}. */
    public String biome;
    /** Registry id of the item in the bot's main hand, or {@code null}. */
    public String mainHand;
}
