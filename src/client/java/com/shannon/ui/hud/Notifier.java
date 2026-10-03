package com.shannon.ui.hud;

import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.state.BotStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * Shows a vanilla toast when the bot starts waiting for the player or runs into trouble.
 *
 * <p>Uses vanilla's toast queue, so it stacks with advancement toasts and other mods' toasts.
 */
public final class Notifier {
    private Notifier() {
    }

    public static void register(ShannonClient shannon) {
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
