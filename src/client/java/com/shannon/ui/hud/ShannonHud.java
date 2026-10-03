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
 * The always-on HUD: the status card, the bot's latest words, what is above the bot's head, its
 * face on the locator bar, and the push-to-talk box.
 *
 * <p>Each part is its own Fabric HUD element, so HUD mods that reorder or hide elements can
 * handle them like any other. All of it hides with F1, while F3 is open, and on servers without
 * this mod.
 */
public final class ShannonHud {
    private static final int MARGIN = 4;
    /** Space vanilla's status effect icons take in the top-right corner. */
    private static final int EFFECTS_HEIGHT = 52;
    private static final long FADE_MS = 600;

    /** Whether the bot's words were drawn above its head this frame. */
    private static boolean spokeOverhead;

    private ShannonHud() {
    }

    public static void register(ShannonClient shannon) {
        HudElementRegistry.addLast(Identifier.of(ShannonUIMod.MOD_ID, "overhead"), (context, tickCounter) -> {
            spokeOverhead = false;
            if (visible(shannon) && shannon.config().showOverhead) {
                boolean speak = shannon.config().showSpeech && shannon.store().status() != BotStatus.WAITING
                        && !(MinecraftClient.getInstance().currentScreen instanceof QuickChatScreen);
                spokeOverhead = Overhead.render(context, shannon, tickCounter.getTickProgress(true), speak);
            }
        });
        HudElementRegistry.addLast(Identifier.of(ShannonUIMod.MOD_ID, "status"),
                (context, tickCounter) -> render(context, shannon));
        HudElementRegistry.addLast(Identifier.of(ShannonUIMod.MOD_ID, "voice"), (context, tickCounter) -> {
            if (visible(shannon)) {
                VoiceIndicator.render(context, shannon);
            }
        });
        // After the held item name, which vanilla draws after the locator bar's dots.
        HudElementRegistry.attachElementAfter(VanillaHudElements.HELD_ITEM_TOOLTIP,
                Identifier.of(ShannonUIMod.MOD_ID, "locator_face"), (context, tickCounter) -> {
                    if (visible(shannon) && shannon.config().showLocatorFace) {
                        LocatorFace.render(context, shannon, tickCounter);
                    }
                });
        // The quick chat screen shows the conversation with the bot where vanilla's chat sits, and
        // vanilla's fading lines would draw through it. Wrapping keeps other mods' changes to chat.
        HudElementRegistry.replaceElement(VanillaHudElements.CHAT, chat -> (context, tickCounter) -> {
            if (!(MinecraftClient.getInstance().currentScreen instanceof QuickChatScreen)) {
                chat.render(context, tickCounter);
            }
        });
    }

    /** Whether the mod's HUD may draw at all this frame. */
    private static boolean visible(ShannonClient shannon) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && !client.options.hudHidden
                && !(client.currentScreen instanceof ShannonScreen)
                && !client.getDebugHud().shouldShowDebugHud()
                && ClientActions.available();
    }

    private static void render(DrawContext context, ShannonClient shannon) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientConfig config = shannon.config();
        if (!visible(shannon) || !config.showCard) {
            return;
        }
        StatusCard.Card card = StatusCard.prepare(shannon);
        int screenWidth = context.getScaledWindowWidth();
        boolean left = config.cardCorner == ClientConfig.Corner.TOP_LEFT;
        int x = left ? MARGIN : screenWidth - card.width() - MARGIN;
        int y = MARGIN;
        if (!left && !client.player.getStatusEffects().isEmpty()) {
            y += EFFECTS_HEIGHT;
        }
        card.draw(context, x, y);
        // While waiting, the card itself shows what the bot asked.
        if (config.showSpeech && !spokeOverhead && !(client.currentScreen instanceof QuickChatScreen)
                && shannon.store().status() != BotStatus.WAITING) {
            int speechX = left ? x : screenWidth - StatusCard.WIDTH - MARGIN;
            renderSpeech(context, shannon, speechX, y + card.height() + 3);
        }
    }

    /** The bot's newest words while they are still fresh, or {@code null}. */
    static ChatState.Message currentSpeech(ShannonClient shannon) {
        ChatState.Message speech = shannon.store().speech();
        if (speech == null) {
            return null;
        }
        long shown = System.currentTimeMillis() - shannon.store().speechAt();
        return shown > shannon.config().speechSeconds * 1000L ? null : speech;
    }

    /** How opaque the words are: fading out over their last moments. */
    static float speechAlpha(ShannonClient shannon) {
        long shown = System.currentTimeMillis() - shannon.store().speechAt();
        long life = shannon.config().speechSeconds * 1000L;
        return shown > life - FADE_MS ? Math.max(0f, (life - shown) / (float) FADE_MS) : 1f;
    }

    private static void renderSpeech(DrawContext context, ShannonClient shannon, int x, int y) {
        ChatState.Message speech = currentSpeech(shannon);
        if (speech == null) {
            return;
        }
        float alpha = speechAlpha(shannon);
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
