package com.shannon.ui.hud;

import com.shannon.ShannonUIMod;
import com.shannon.model.ChatState;
import com.shannon.ui.ClientConfig;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.QuickChatScreen;
import com.shannon.ui.screen.ShannonScreen;
import com.shannon.ui.state.BotStatus;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The always-on HUD: the status card and, under it, the bot's latest words.
 *
 * <p>Registered as its own Fabric HUD element, so HUD mods that reorder or hide elements can
 * handle it like any other. It hides with F1, while F3 is open, and on servers without this mod.
 */
public final class ShannonHud {
    private static final int MARGIN = 4;
    /** Space vanilla's status effect icons take in the top-right corner. */
    private static final int EFFECTS_HEIGHT = 52;
    private static final long FADE_MS = 600;

    private ShannonHud() {
    }

    public static void register(ShannonClient shannon) {
        HudElementRegistry.addLast(Identifier.of(ShannonUIMod.MOD_ID, "status"),
                (context, tickCounter) -> render(context, shannon));
        // The quick chat screen shows the conversation with the bot where vanilla's chat sits, and
        // vanilla's fading lines would draw through it. Wrapping keeps other mods' changes to chat.
        HudElementRegistry.replaceElement(VanillaHudElements.CHAT, chat -> (context, tickCounter) -> {
            if (!(MinecraftClient.getInstance().currentScreen instanceof QuickChatScreen)) {
                chat.render(context, tickCounter);
            }
        });
    }

    private static void render(DrawContext context, ShannonClient shannon) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientConfig config = shannon.config();
        if (client.player == null || client.options.hudHidden || !config.showCard
                || client.currentScreen instanceof ShannonScreen
                || client.getDebugHud().shouldShowDebugHud()
                || !ClientActions.available()) {
            return;
        }
        int screenWidth = context.getScaledWindowWidth();
        int x = config.cardCorner == ClientConfig.Corner.TOP_LEFT ? MARGIN : screenWidth - StatusCard.WIDTH - MARGIN;
        int y = MARGIN;
        if (config.cardCorner == ClientConfig.Corner.TOP_RIGHT && !client.player.getStatusEffects().isEmpty()) {
            y += EFFECTS_HEIGHT;
        }
        int height = StatusCard.render(context, shannon, x, y);
        // While waiting, the card itself shows what the bot asked.
        if (config.showSpeech && !(client.currentScreen instanceof QuickChatScreen)
                && shannon.store().status() != BotStatus.WAITING) {
            renderSpeech(context, shannon, x, y + height + 3);
        }
    }

    private static void renderSpeech(DrawContext context, ShannonClient shannon, int x, int y) {
        ChatState.Message speech = shannon.store().speech();
        if (speech == null) {
            return;
        }
        long shown = System.currentTimeMillis() - shannon.store().speechAt();
        long life = shannon.config().speechSeconds * 1000L;
        if (shown > life) {
            return;
        }
        float alpha = shown > life - FADE_MS ? (life - shown) / (float) FADE_MS : 1f;
        Text text = Text.empty()
                .append(Text.translatable("shannonuimod.name").formatted(Formatting.AQUA))
                .append(Text.literal("  "))
                .append(Text.literal(speech.message));
        int inner = StatusCard.WIDTH - 12;
        int height = Gui.paragraphHeight(text, inner, 4) + 7;
        context.fill(x, y, x + StatusCard.WIDTH, y + height, Palette.fade(Palette.HUD_BOX, alpha));
        Gui.paragraph(context, text, x + 6, y + 4, inner, Palette.fade(Palette.WHITE, alpha), 4);
    }
}
