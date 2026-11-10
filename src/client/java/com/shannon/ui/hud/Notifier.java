package com.shannon.ui.hud;

import com.shannon.ui.screen.CommandScreen;
import com.shannon.ui.screen.QuickChatScreen;
import com.shannon.ui.screen.ShannonScreen;
import net.minecraft.client.gui.screen.Screen;

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
 *
 * <p>No toast while the status card is in view: the card already grows to show the question, the trouble or
 * the danger with the same key hints, and a toast repeating it only covers the top-right corner. Nor while the
 * mod's own screen is open, which shows the status itself. The toast is for when the card cannot be seen.
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
                    && now - lastDangerToast > DANGER_GAP_MS && !statusInView(shannon)) {
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
            if (now == before || !shannon.config().showToasts || statusInView(shannon)) {
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

    /** Whether the player can already see the bot's status: the HUD card, or the mod's own screen. */
    static boolean statusInView(ShannonClient shannon) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        if (screen instanceof ShannonScreen) {
            return true;
        }
        boolean hudShown = client.player != null && !client.options.hudHidden
                && !client.getDebugHud().shouldShowDebugHud() && shannon.config().showCard;
        return hudShown && (screen == null || screen instanceof QuickChatScreen || screen instanceof CommandScreen);
    }
}
