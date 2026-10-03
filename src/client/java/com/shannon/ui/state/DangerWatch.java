package com.shannon.ui.state;

import com.shannon.model.BotVitals;
import com.shannon.sync.StateChannels;
import net.minecraft.text.Text;

/**
 * Decides when the bot is in danger: just hurt, or low on health.
 *
 * <p>The server counts hurts; this notes the local time each time the count grows, so the
 * server's and the player's clocks never need to agree.
 */
public final class DangerWatch {
    /** How long after a hurt the bot still counts as under attack. */
    private static final long HURT_MS = 6_000;
    /** Health at or below this (three hearts) is low. */
    private static final float LOW_HEALTH = 6f;

    private final ClientStore store;
    private int lastHurtCount = -1;
    private long hurtAt;
    private String hurtBy;
    private String hurtCause;

    public DangerWatch(ClientStore store) {
        this.store = store;
        store.onClear(this::clear);
        store.listen(channel -> {
            if (channel == StateChannels.VITALS) {
                update(store.get(StateChannels.VITALS));
            }
        });
    }

    private void update(BotVitals vitals) {
        if (vitals == null || !vitals.online) {
            return;
        }
        if (lastHurtCount >= 0 && vitals.hurtCount > lastHurtCount) {
            hurtAt = System.currentTimeMillis();
            hurtBy = vitals.hurtBy;
            hurtCause = vitals.hurtCause;
        }
        lastHurtCount = vitals.hurtCount;
    }

    /** Forgets everything; called when leaving a server. */
    public void clear() {
        lastHurtCount = -1;
        hurtAt = 0;
    }

    public boolean recentlyHurt() {
        return System.currentTimeMillis() - hurtAt < HURT_MS;
    }

    public boolean lowHealth() {
        BotVitals vitals = store.get(StateChannels.VITALS);
        return vitals != null && vitals.online && vitals.health > 0 && vitals.health <= LOW_HEALTH;
    }

    public boolean inDanger() {
        BotVitals vitals = store.get(StateChannels.VITALS);
        return vitals != null && vitals.online && (recentlyHurt() || lowHealth());
    }

    public long hurtAt() {
        return hurtAt;
    }

    /** What is happening, in a short sentence: who attacks, or what hurts. */
    public Text describe() {
        if (!recentlyHurt()) {
            return Text.translatable("shannonuimod.danger.low_health");
        }
        if (hurtBy != null) {
            return Text.translatable("shannonuimod.danger.attacked", Text.translatable(hurtBy));
        }
        String key = "shannonuimod.danger.cause." + (hurtCause == null ? "generic" : hurtCause);
        return Text.translatableWithFallback(key, Text.translatable("shannonuimod.danger.cause.generic").getString());
    }
}
