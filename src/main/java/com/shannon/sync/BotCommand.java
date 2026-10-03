package com.shannon.sync;

/** The quick orders a player can give the bot from the command switcher. */
public enum BotCommand {
    /** Stop moving where it stands. */
    STOP,
    /** Follow the player who gave the order. */
    FOLLOW,
    /** Come to where the player stands. */
    COME,
    /** Resume the task that is waiting or paused. */
    RESUME,
    /** Give up the current task. */
    CANCEL;

    /** Parses a wire name, or returns {@code null} for an unknown one. */
    public static BotCommand parse(String name) {
        if (name == null) {
            return null;
        }
        for (BotCommand command : values()) {
            if (command.name().equals(name)) {
                return command;
            }
        }
        return null;
    }
}
