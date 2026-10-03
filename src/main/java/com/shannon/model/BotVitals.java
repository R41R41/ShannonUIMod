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
    /**
     * How many times the bot has been hurt since the server started. The client notes the time it
     * sees this grow, so the two machines' clocks never need to agree.
     */
    public int hurtCount;
    /** Translation key of the entity that hurt the bot last, such as {@code entity.minecraft.zombie}. */
    public String hurtBy;
    /** Vanilla's damage message id for the last hurt, such as {@code fall} or {@code lava}. */
    public String hurtCause;
}
