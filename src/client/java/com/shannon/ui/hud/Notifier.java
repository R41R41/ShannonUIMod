package com.shannon.ui.hud;

import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.state.BotStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * Shows a vanilla toast when the bot starts waiting for the player, runs into trouble, or is hurt
 * and low on health. Nothing else makes a toast, so toasts keep meaning "look now".
 *
 * <p>Uses vanilla's toast queue, so it stacks with advancement toasts and other mods' toasts.
 */
public final class Notifier {
    /** Danger toasts come at most this often, however long a fight goes on. */
    private static final long DANGER_GAP_MS = 30_000;

    private static long lastDangerToast;

    private Notifier() {
    }

    public static void register(ShannonClient shannon) {
        shannon.store().listen(channel -> {
            if (channel != StateChannels.VITALS || !shannon.config().showToasts) {
                return;
            }
            long now = System.currentTimeMillis();
            if (shannon.danger().recentlyHurt() && shannon.danger().lowHealth()
                    && now - lastDangerToast > DANGER_GAP_MS) {
                lastDangerToast = now;
                SystemToast.show(MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.translatable("shannonuimod.toast.danger"), shannon.danger().describe());
            }
        });
        BotStatus[] last = {BotStatus.IDLE};
        shannon.store().listen(channel -> {
            if (channel != StateChannels.TASK_TREE && channel != StateChannels.TASK_LIST) {
                return;
            }
            BotStatus now = shannon.store().status();
            BotStatus before = last[0];
            last[0] = now;
            if (now == before || !shannon.config().showToasts) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (now == BotStatus.WAITING) {
                SystemToast.show(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.translatable("shannonuimod.toast.waiting"),
                        Text.translatable("shannonuimod.toast.waiting.hint",
                                KeyBindings.keyName(shannon.keys().talk())));
            } else if (now == BotStatus.ERROR) {
                SystemToast.show(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.translatable("shannonuimod.toast.error"),
                        Text.translatable("shannonuimod.toast.error.hint",
                                KeyBindings.keyName(shannon.keys().commands())));
            }
        });
    }
}
