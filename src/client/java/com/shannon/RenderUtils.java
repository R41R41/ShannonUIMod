package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * UI描画の共通ユーティリティ
 * 全レンダラーで重複していたパターンを集約
 */
public final class RenderUtils {

    // === 共通定数 ===
    public static final float SCALE = 0.7f;
    public static final int LINE_HEIGHT = 10;
    public static final int ICON_SIZE = 16; // Minecraftアイテムアイコンサイズ

    // === 共通色 (1.21.11: アルファ 0xFF を明示的に指定する必要がある) ===
    public static final int COLOR_HEADER = 0xFF55AAFF;
    public static final int COLOR_SUCCESS = 0xFF55FF55;
    public static final int COLOR_ERROR = 0xFFFF5555;
    public static final int COLOR_WARNING = 0xFFFFAA55;
    public static final int COLOR_IN_PROGRESS = 0xFFFFFF55;
    public static final int COLOR_TEXT = 0xFFFFFFFF;
    public static final int COLOR_SUBTEXT = 0xFFAAAAAA;
    public static final int COLOR_MUTED = 0xFF888888;
    public static final int COLOR_CATEGORY = 0xFFFFAA55;
    public static final int COLOR_SEPARATOR = 0xFF555555;

    // UI背景色
    public static final int BG_BUTTON = 0xFF444444;
    public static final int BG_BUTTON_HOVER = 0xFF666666;
    public static final int BG_SLOT = 0xFF1A1A1A;
    public static final int BG_SLOT_HOVER = 0xFF333333;

    private RenderUtils() {
    }

    // === テキスト ===

    public static List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        return mc.textRenderer.wrapLines(Text.literal(text), maxWidth);
    }

    // === コンテンツ高さ管理 ===

    public static void updateContentHeight(UIRenderer.UIState state, int totalContentPixels, float scale,
            int uiHeight) {
        state.contentHeight = (int) (totalContentPixels * scale) + 16;
        if (state.contentHeight <= uiHeight) {
            state.scrollOffset = 0;
            ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
        }
    }

    // === ボタン描画 ===

    /**
     * Minecraft風ボタンを描画。クリックされた場合trueを返す。
     */
    public static boolean drawButton(DrawContext ctx, MinecraftClient mc, String text,
            int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown, boolean wasDown) {
        return drawButton(ctx, mc, text, x, y, w, h, mouseX, mouseY, mouseDown, wasDown, false);
    }

    /**
     * Minecraft風ボタンを描画（selected状態対応）。クリックされた場合trueを返す。
     */
    public static boolean drawButton(DrawContext ctx, MinecraftClient mc, String text,
            int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown, boolean wasDown,
            boolean selected) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = selected ? 0xFF335588 : (hover ? BG_BUTTON_HOVER : BG_BUTTON);
        ctx.fill(x, y, x + w, y + h, bg);
        // 立体枠線
        int topColor = selected ? 0xFF5599FF : (hover ? 0xFF888888 : 0xFF666666);
        ctx.fill(x, y, x + w, y + 1, topColor);
        ctx.fill(x, y, x + 1, y + h, topColor);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF222222);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFF222222);
        // 中央テキスト
        int tw = mc.textRenderer.getWidth(text);
        int textColor = selected ? 0xFF88CCFF : (hover ? COLOR_TEXT : 0xFFCCCCCC);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text),
                x + (w - tw) / 2, y + (h - 8) / 2, textColor);
        return mouseDown && !wasDown && hover;
    }

    // === プログレスバー ===

    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h,
            int completed, int total) {
        ctx.fill(x, y, x + w, y + h, 0xFF222222);
        ctx.fill(x, y, x + w, y + 1, 0xFF444444);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF444444);
        if (total > 0 && completed > 0) {
            int fillColor = completed == total ? 0xFF44AA44
                    : completed > total / 2 ? 0xFFAAAA44 : 0xFF4488CC;
            int fillW = Math.max((int) ((float) completed / total * w), 1);
            ctx.fill(x, y + 1, x + fillW, y + h - 1, fillColor);
        }
    }

    // === ツールチップ ===

    /**
     * 背景付きツールチップを描画（最前面用）
     */
    public static void drawTooltip(DrawContext ctx, MinecraftClient mc,
            List<String> lines, int x, int y, int maxWidth) {
        if (lines == null || lines.isEmpty())
            return;

        int pad = 4;
        int lh = 10;
        java.util.List<List<OrderedText>> allWrapped = new java.util.ArrayList<>();
        int totalLines = 0;
        int maxTw = 0;
        for (String line : lines) {
            List<OrderedText> wrapped = wrapText(mc, line, maxWidth);
            allWrapped.add(wrapped);
            totalLines += wrapped.size();
            for (OrderedText ot : wrapped) {
                maxTw = Math.max(maxTw, mc.textRenderer.getWidth(ot));
            }
        }

        int tw = maxTw + pad * 2;
        int th = totalLines * lh + pad * 2;

        // Z軸を前面に移動して他のテキストの上に描画
        ctx.getMatrices().pushMatrix();
        // Note: z-ordering not supported in Matrix3x2fStack (1.21.11+)

        // 背景（完全不透明）
        ctx.fill(x - pad, y - pad, x + tw, y + th - pad, 0xFF100010);
        // 枠線（紫）
        int border = 0xFF5000AA;
        ctx.fill(x - pad, y - pad, x + tw, y - pad + 1, border);
        ctx.fill(x - pad, y + th - pad - 1, x + tw, y + th - pad, border);
        ctx.fill(x - pad, y - pad, x - pad + 1, y + th - pad, border);
        ctx.fill(x + tw - 1, y - pad, x + tw, y + th - pad, border);

        // テキスト
        int drawY = y;
        for (List<OrderedText> wrapped : allWrapped) {
            for (OrderedText ot : wrapped) {
                ctx.drawTextWithShadow(mc.textRenderer, ot, x, drawY, COLOR_TEXT);
                drawY += lh;
            }
        }

        ctx.getMatrices().popMatrix();
    }

    // === アイテムスロット背景 ===

    /**
     * Minecraft風のアイテムスロット枠を描画
     */
    public static void drawSlotBackground(DrawContext ctx, int x, int y, int size, boolean hovered) {
        int bg = hovered ? BG_SLOT_HOVER : BG_SLOT;
        ctx.fill(x, y, x + size, y + size, bg);
        // 内側の溝
        ctx.fill(x, y, x + size, y + 1, 0xFF373737);
        ctx.fill(x, y, x + 1, y + size, 0xFF373737);
        ctx.fill(x, y + size - 1, x + size, y + size, 0xFF8B8B8B);
        ctx.fill(x + size - 1, y, x + size, y + size, 0xFF8B8B8B);
    }

    // === セクションヘッダー ===

    public static final int SECTION_HEADER_HEIGHT = 14;

    /**
     * ベベル付きセクションヘッダーを描画（左アクセントバー付き）
     */
    public static void drawSectionHeader(DrawContext ctx, MinecraftClient mc, String text,
            int x, int y, int width, int accentColor) {
        int h = SECTION_HEADER_HEIGHT;
        ctx.fill(x, y, x + width, y + h, 0xFF2A2A2E);
        ctx.fill(x, y, x + width, y + 1, 0xFF3A3A3E);
        ctx.fill(x, y + h - 1, x + width, y + h, 0xFF1A1A1E);
        ctx.fill(x, y, x + 3, y + h, accentColor);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x + 8, y + 3, COLOR_TEXT);
    }

    // === カード背景 ===

    /**
     * ベベル付きカード背景を描画（左アクセントバー付き）
     */
    public static void drawCard(DrawContext ctx, int x, int y, int w, int h,
            int accentColor, boolean hovered) {
        int bg = hovered ? 0xFF363640 : 0xFF2C2C32;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, hovered ? 0xFF464650 : 0xFF3C3C42);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF1A1A1E);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFF1A1A1E); // 右枠線
        if (accentColor != 0) {
            ctx.fill(x, y, x + 3, y + h, accentColor);
        }
    }

    // === トグルスイッチ ===

    /**
     * ON/OFFトグルスイッチを描画（18x8ピクセル）
     */
    public static void drawToggleSwitch(DrawContext ctx, int x, int y, boolean on, boolean hovered) {
        int w = 20, h = 8;
        int knobW = 8;
        // トラック（明るめの枠線で視認性確保）
        int trackColor = on ? 0xFF2D6A2D : 0xFF383838;
        ctx.fill(x, y, x + w, y + h, trackColor);
        ctx.fill(x, y, x + w, y + 1, on ? 0xFF4A8A4A : 0xFF4A4A4A);
        ctx.fill(x, y + h - 1, x + w, y + h, on ? 0xFF1A3A1A : 0xFF222222);
        ctx.fill(x, y, x + 1, y + h, on ? 0xFF4A8A4A : 0xFF4A4A4A);
        ctx.fill(x + w - 1, y, x + w, y + h, on ? 0xFF1A3A1A : 0xFF222222);
        // ノブ
        int knobX = on ? x + w - knobW : x;
        int knobColor = on ? (hovered ? 0xFF66FF66 : 0xFF44CC44) : (hovered ? 0xFFBBBBBB : 0xFF888888);
        ctx.fill(knobX, y, knobX + knobW, y + h, knobColor);
        ctx.fill(knobX, y, knobX + knobW, y + 1, on ? 0xFF88FF88 : 0xFFAAAAAA);
        ctx.fill(knobX, y + h - 1, knobX + knobW, y + h, on ? 0xFF228822 : 0xFF555555);
    }

    // === ミニプログレスバー ===

    /**
     * 3px高のミニプログレスバーを描画
     */
    public static void drawMiniProgressBar(DrawContext ctx, int x, int y, int w,
            int current, int total, int fillColor) {
        ctx.fill(x, y, x + w, y + 3, 0xFF1A1A1A);
        if (total > 0 && current > 0) {
            int fillW = Math.max((int) ((float) current / total * w), 1);
            ctx.fill(x, y, x + fillW, y + 3, fillColor);
        }
    }

    // === スケーリングコンテキスト ===

    /**
     * translate+scale後の座標系で使う共通計算結果
     */
    public static class ScaledContext {
        public final int drawX;
        public final int drawY;
        public final int yOffset;
        public final int scaledWidth;
        public final int scaledHeight;
        public final int maxWidth;
        public final int startY;
        public final int mouseX;
        public final int mouseY;

        /**
         * @param uiWidth      パネル幅（スケール前）
         * @param uiHeight     パネル高さ（スケール前）
         * @param scrollOffset スクロールオフセット
         * @param relMouseX    パネル相対マウスX
         * @param relMouseY    パネル相対マウスY（scrollOffset含む）
         */
        public ScaledContext(int uiWidth, int uiHeight, int scrollOffset, int relMouseX, int relMouseY) {
            this.drawX = (int) (4 / SCALE);
            this.drawY = (int) (4 / SCALE);
            this.yOffset = (int) (-scrollOffset / SCALE);
            this.scaledWidth = (int) (uiWidth / SCALE);
            this.scaledHeight = (int) (uiHeight / SCALE);
            this.maxWidth = scaledWidth - 8;
            this.startY = drawY + yOffset;
            this.mouseX = (int) ((relMouseX + 4) / SCALE);
            this.mouseY = (int) ((relMouseY + 4) / SCALE);
        }
    }
}
