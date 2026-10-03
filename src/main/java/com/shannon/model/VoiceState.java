package com.shannon.model;

/**
 * What happened to the player's push-to-talk: sent once per event and never stored, so a player
 * who joins later does not see an old transcript.
 */
public class VoiceState {
    /** The push-to-talk press reached the bot and it is listening. */
    public static final String LISTENING = "listening";
    /** Someone else is talking; the press was refused for now. */
    public static final String BLOCKED = "blocked";
    /** The bot is not in a voice channel, or does not know this player. */
    public static final String UNAVAILABLE = "unavailable";
    /** The bot heard something and wrote it down. */
    public static final String TRANSCRIPT = "transcript";

    /** One of the constants above. */
    public String event;
    /** Who is talking, for {@link #BLOCKED}. */
    public String blockedBy;
    /** What the bot heard, for {@link #TRANSCRIPT}. */
    public String text;
    /** Who spoke, for {@link #TRANSCRIPT}: the receiving player's own name. */
    public String speaker;
    /** {@code minebot} when the voice goes to the bot as an order, {@code chat} when it is conversation. */
    public String mode;

    public VoiceState() {
    }

    public VoiceState(String event) {
        this.event = event;
    }
}
