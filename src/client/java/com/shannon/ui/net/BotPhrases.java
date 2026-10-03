package com.shannon.ui.net;

import com.shannon.ui.input.PointTarget;

/**
 * The words sent to the bot for requests made by pointing or picking, in Japanese because that
 * is the language the bot works in. Orders about a place carry its coordinates and the block's
 * registry id, so the bot need not guess which block is meant.
 */
public final class BotPhrases {
    private BotPhrases() {
    }

    public static String dig(PointTarget target) {
        return "(" + target.coordinates() + ") にある" + target.name().getString() + "（" + target.id() + "）を掘って";
    }

    public static String gather(PointTarget target) {
        return "近くの" + target.name().getString() + "（" + target.id() + "）を集めて";
    }

    public static String comeTo(PointTarget target) {
        return "(" + target.coordinates() + ") まで来て";
    }

    public static String waitAt(PointTarget target) {
        return "(" + target.coordinates() + ") まで来て、そこで待っていて";
    }

    public static String attack(PointTarget target) {
        return "(" + target.coordinates() + ") あたりにいる" + target.name().getString() + "（" + target.id() + "）を倒して";
    }

    /** Asking for items reads naturally without the id; the player sees and saves these words. */
    public static String collect(String itemName, int count) {
        return itemName + "を" + count + "個集めて";
    }
}
