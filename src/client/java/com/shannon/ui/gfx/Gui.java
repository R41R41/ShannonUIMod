package com.shannon.ui.gfx;

import com.shannon.ShannonUIMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Drawing primitives that reproduce vanilla's GUI pieces with plain fills, so they stay crisp at
 * every GUI scale and need no textures of their own.
 *
 * <p>Everything goes through {@link DrawContext}; no render state is changed, so other mods'
 * HUDs and screens drawn before or after are unaffected.
 */
public final class Gui {
    public static final Identifier SHANNON_FACE = Identifier.of(ShannonUIMod.MOD_ID, "textures/shannon.png");

    private static final Identifier HEART_CONTAINER = vanilla("textures/gui/sprites/hud/heart/container.png");
    private static final Identifier HEART_FULL = vanilla("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_HALF = vanilla("textures/gui/sprites/hud/heart/half.png");
    private static final Identifier FOOD_EMPTY = vanilla("textures/gui/sprites/hud/food_empty.png");
    private static final Identifier FOOD_FULL = vanilla("textures/gui/sprites/hud/food_full.png");
    private static final Identifier FOOD_HALF = vanilla("textures/gui/sprites/hud/food_half.png");

    /** Line height of vanilla text, with the one-pixel gap vanilla leaves between lines. */
    public static final int LINE = 10;
    public static final String ELLIPSIS = "…";

    private Gui() {
    }

    private static Identifier vanilla(String path) {
        return Identifier.of("minecraft", path);
    }

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    // ===== Text =====

    /** Text with vanilla's drop shadow, as on the HUD and on buttons. */
    public static void text(DrawContext context, Text text, int x, int y, int color) {
        context.drawTextWithShadow(font(), text, x, y, color);
    }

    public static void text(DrawContext context, OrderedText text, int x, int y, int color) {
        context.drawTextWithShadow(font(), text, x, y, color);
    }

    /** A container title: dark grey, no shadow, as vanilla draws "Inventory". */
    public static void label(DrawContext context, Text text, int x, int y) {
        context.drawText(font(), text, x, y, Palette.LABEL, false);
    }

    public static int width(Text text) {
        return font().getWidth(text);
    }

    public static int width(String text) {
        return font().getWidth(text);
    }

    /** {@code text} cut to {@code maxWidth}, ending in an ellipsis when it was cut. */
    public static Text fit(String text, int maxWidth) {
        if (text == null) {
            return Text.empty();
        }
        TextRenderer font = font();
        if (font.getWidth(text) <= maxWidth) {
            return Text.literal(text);
        }
        String cut = font.trimToWidth(text, Math.max(0, maxWidth - font.getWidth(ELLIPSIS)));
        return Text.literal(cut + ELLIPSIS);
    }

    /** Wraps {@code text} to {@code maxWidth}, keeping Japanese punctuation off the start of a line. */
    public static List<OrderedText> wrap(Text text, int maxWidth) {
        return LineBreaker.wrap(font(), text, maxWidth);
    }

    /** Draws wrapped text and returns the height used. Lines beyond {@code maxLines} are dropped. */
    public static int paragraph(DrawContext context, Text text, int x, int y, int maxWidth, int color, int maxLines) {
        List<OrderedText> lines = wrap(text, maxWidth);
        int count = Math.min(lines.size(), maxLines);
        for (int i = 0; i < count; i++) {
            text(context, lines.get(i), x, y + i * LINE, color);
        }
        return count * LINE;
    }

    public static int paragraphHeight(Text text, int maxWidth, int maxLines) {
        return Math.min(wrap(text, maxWidth).size(), maxLines) * LINE;
    }

    // ===== Boxes =====

    /** A vanilla container background: black outline with cut corners, light top-left, dark bottom-right. */
    public static void panel(DrawContext context, int x, int y, int w, int h) {
        int r = x + w;
        int b = y + h;
        context.fill(x + 1, y, r - 1, y + 1, Palette.OUTLINE);
        context.fill(x + 1, b - 1, r - 1, b, Palette.OUTLINE);
        context.fill(x, y + 1, x + 1, b - 1, Palette.OUTLINE);
        context.fill(r - 1, y + 1, r, b - 1, Palette.OUTLINE);
        context.fill(x + 1, y + 1, r - 1, b - 1, Palette.PANEL);
        context.fill(x + 1, y + 1, r - 3, y + 3, Palette.PANEL_LIGHT);
        context.fill(x + 1, y + 1, x + 3, b - 3, Palette.PANEL_LIGHT);
        context.fill(x + 3, b - 3, r - 1, b - 1, Palette.PANEL_SHADOW);
        context.fill(r - 3, y + 3, r - 1, b - 1, Palette.PANEL_SHADOW);
    }

    /** A creative-inventory style tab on top of a panel. The selected tab merges into the panel. */
    public static void tab(DrawContext context, int x, int y, int w, int h, boolean selected) {
        int r = x + w;
        int b = y + h;
        int face = selected ? Palette.PANEL : Palette.TAB_OFF;
        int light = selected ? Palette.PANEL_LIGHT : Palette.TAB_OFF_LIGHT;
        int shadow = selected ? Palette.PANEL_SHADOW : Palette.TAB_OFF_SHADOW;
        context.fill(x + 1, y, r - 1, y + 1, Palette.OUTLINE);
        context.fill(x, y + 1, x + 1, b, Palette.OUTLINE);
        context.fill(r - 1, y + 1, r, b, Palette.OUTLINE);
        context.fill(x + 1, y + 1, r - 1, b, face);
        context.fill(x + 1, y + 1, r - 3, y + 3, light);
        context.fill(x + 1, y + 1, x + 3, b, light);
        context.fill(r - 3, y + 3, r - 1, b, shadow);
    }

    /** A sunken area, like the slot texture: dark top-left edge, light bottom-right edge. */
    public static void inset(DrawContext context, int x, int y, int w, int h, int face) {
        int r = x + w;
        int b = y + h;
        context.fill(x, y, r, b, face);
        context.fill(x, y, r - 1, y + 1, Palette.SLOT_SHADOW);
        context.fill(x, y, x + 1, b - 1, Palette.SLOT_SHADOW);
        context.fill(x + 1, b - 1, r, b, Palette.SLOT_LIGHT);
        context.fill(r - 1, y + 1, r, b, Palette.SLOT_LIGHT);
    }

    /** One 18 by 18 inventory slot. */
    public static void slot(DrawContext context, int x, int y) {
        inset(context, x, y, 18, 18, Palette.SLOT);
    }

    /** A translucent HUD box like the chat background, with a faint or coloured edge. */
    public static void hudBox(DrawContext context, int x, int y, int w, int h, int edge) {
        context.fill(x, y, x + w, y + h, Palette.HUD_BOX);
        outline(context, x, y, w, h, edge);
    }

    public static void outline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y + 1, x + 1, y + h - 1, color);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** An experience-bar style progress bar. */
    public static void progress(DrawContext context, int x, int y, int w, int h, float fraction) {
        context.fill(x, y, x + w, y + h, Palette.OUTLINE);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, Palette.BAR_BACK);
        int filled = Math.round((w - 2) * Math.max(0f, Math.min(1f, fraction)));
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, Palette.XP);
            context.fill(x + 1, y + h - 2, x + 1 + filled, y + h - 1, Palette.XP_DARK);
        }
    }

    /** A vanilla tooltip, kept inside the screen. */
    public static void tooltip(DrawContext context, List<Text> lines, int mouseX, int mouseY, int screenW, int screenH) {
        if (lines.isEmpty()) {
            return;
        }
        int w = 0;
        for (Text line : lines) {
            w = Math.max(w, width(line));
        }
        int h = lines.size() == 1 ? 8 : 8 + 2 + (lines.size() - 1) * LINE;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w + 4 > screenW) {
            x = Math.max(4, mouseX - 16 - w);
        }
        y = Math.max(4, Math.min(y, screenH - h - 4));
        context.fill(x - 3, y - 4, x + w + 3, y - 3, Palette.TOOLTIP_BACK);
        context.fill(x - 3, y + h + 3, x + w + 3, y + h + 4, Palette.TOOLTIP_BACK);
        context.fill(x - 3, y - 3, x + w + 3, y + h + 3, Palette.TOOLTIP_BACK);
        context.fill(x - 4, y - 3, x - 3, y + h + 3, Palette.TOOLTIP_BACK);
        context.fill(x + w + 3, y - 3, x + w + 4, y + h + 3, Palette.TOOLTIP_BACK);
        context.fillGradient(x - 3, y - 2, x - 2, y + h + 2, Palette.TOOLTIP_EDGE_TOP, Palette.TOOLTIP_EDGE_BOTTOM);
        context.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, Palette.TOOLTIP_EDGE_TOP, Palette.TOOLTIP_EDGE_BOTTOM);
        context.fill(x - 3, y - 3, x + w + 3, y - 2, Palette.TOOLTIP_EDGE_TOP);
        context.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, Palette.TOOLTIP_EDGE_BOTTOM);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = y + (i == 0 ? 0 : 2 + i * LINE);
            text(context, lines.get(i), x, lineY, Palette.WHITE);
        }
    }

    /** A key cap with the key's name; returns its width. */
    public static int keycap(DrawContext context, Text key, int x, int y) {
        int w = Math.max(12, width(key) + 6);
        context.fill(x, y, x + w, y + 12, 0xFF2A2A2A);
        context.fill(x, y, x + w, y + 1, 0xFFA0A0A0);
        context.fill(x, y, x + 1, y + 12, 0xFFA0A0A0);
        context.fill(x + 1, y + 11, x + w, y + 12, 0xFF5A5A5A);
        context.fill(x + w - 1, y + 1, x + w, y + 12, 0xFF5A5A5A);
        text(context, key, x + (w - width(key)) / 2, y + 2, Palette.WHITE);
        return w;
    }

    /** A key cap followed by what the key does; returns the total width. */
    public static int keyHint(DrawContext context, Text key, Text action, int x, int y) {
        int w = keycap(context, key, x, y);
        text(context, action, x + w + 3, y + 2, Palette.GRAY);
        return w + 3 + width(action);
    }

    public static int keyHintWidth(Text key, Text action) {
        return Math.max(12, width(key) + 6) + 3 + width(action);
    }

    // ===== Pictures =====

    public static void face(DrawContext context, int x, int y, int size) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, SHANNON_FACE, x, y, 0, 0, size, size, size, size);
    }

    private static void sprite(DrawContext context, Identifier id, int x, int y) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, 9, 9, 9, 9);
    }

    /** Ten vanilla hearts, 81 pixels wide. */
    public static void hearts(DrawContext context, int x, int y, float health, float maxHealth) {
        int count = Math.max(1, Math.min(10, (int) Math.ceil(maxHealth / 2f)));
        int halves = Math.round(health);
        for (int i = 0; i < count; i++) {
            int hx = x + i * 8;
            sprite(context, HEART_CONTAINER, hx, y);
            if (halves >= i * 2 + 2) {
                sprite(context, HEART_FULL, hx, y);
            } else if (halves == i * 2 + 1) {
                sprite(context, HEART_HALF, hx, y);
            }
        }
    }

    /** Ten vanilla food icons filled from the right, as vanilla does, 81 pixels wide. */
    public static void food(DrawContext context, int x, int y, int food) {
        for (int i = 0; i < 10; i++) {
            int fx = x + (9 - i) * 8;
            sprite(context, FOOD_EMPTY, fx, y);
            if (food >= i * 2 + 2) {
                sprite(context, FOOD_FULL, fx, y);
            } else if (food == i * 2 + 1) {
                sprite(context, FOOD_HALF, fx, y);
            }
        }
    }

    /** The item for a registry id, or an empty stack for an unknown one (such as an item from a mod this client lacks). */
    public static ItemStack stack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return ItemStack.EMPTY;
        }
        Item item = Registries.ITEM.get(id);
        return new ItemStack(item, Math.max(1, Math.min(count, item.getMaxCount())));
    }

    /** An item with its stack count, as in a slot. {@code count} may exceed a stack. */
    public static void item(DrawContext context, String itemId, int count, int x, int y) {
        ItemStack stack = stack(itemId, count);
        if (stack.isEmpty()) {
            context.fill(x + 3, y + 3, x + 13, y + 13, 0xFF6E6E6E);
            text(context, Text.literal("?"), x + 6, y + 4, Palette.WHITE);
        } else {
            context.drawItem(stack, x, y);
        }
        if (count > 1) {
            String label = String.valueOf(count);
            text(context, Text.literal(label), x + 17 - width(label), y + 9, Palette.WHITE);
        }
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
