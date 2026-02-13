package com.shannon;

import com.shannon.network.packet.AdvancementsState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 進捗（アドバンスメント）タブのUIレンダラー
 * 完了/未完了フィルタ＋ホバーツールチップ対応
 */
public class AdvancementsUIRenderer {

    // 折りたたまれているカテゴリのセット
    private static final Set<String> collapsedCategories = new HashSet<>();

    // 定数
    private static final float SCALE = RenderUtils.SCALE;
    private static final int LINE_HEIGHT = 11;

    // 色定義
    private static final int COLOR_HEADER_BG = 0xFF3A3A3A;
    private static final int COLOR_HEADER_COMPLETE_BG = 0xFF2A4A2A;
    private static final int COLOR_CATEGORY_TEXT = 0xFFFFFF00;
    private static final int COLOR_DONE = 0xFF55FF55;
    private static final int COLOR_NOT_DONE = 0xFFAAAAAA;
    private static final int COLOR_PROGRESS = 0xFF55AAFF;
    private static final int COLOR_DESCRIPTION = 0xFF888888;
    private static final int COLOR_LOADING = 0xFFCCCCCC;
    private static final int COLOR_REFRESH = 0xFF88CCFF;

    private static boolean wasMousePressed = false;

    // フィルタモード
    public enum FilterMode {
        ALL, INCOMPLETE, COMPLETE
    }

    private static FilterMode filterMode = FilterMode.ALL;

    // ホバーツールチップ用
    private static String hoveredAdvTitle = null;
    private static String hoveredAdvDesc = null;
    private static int hoveredAdvX = 0;
    private static int hoveredAdvY = 0;

    public static void renderAdvancements(DrawContext context, MinecraftClient mc,
            int x, int y, int uiWidth, int uiHeight,
            UIRenderer.UIState state, int scrollOffset,
            AdvancementsState advState,
            int mouseX, int mouseY, boolean mouseClicked) {

        context.getMatrices().push();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int scaledMX = (int) ((mouseX + 4) / SCALE);
            int scaledMY = (int) ((mouseY + 4) / SCALE);
            int scrollComp = (int) (scrollOffset / SCALE);
            int clickMY = scaledMY - scrollComp;
            int scaledWidth = (int) (uiWidth / SCALE);

            int drawX = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxWidth = scaledWidth - 8;
            int drawY = (int) (4 / SCALE) + yOffset;

            boolean justClicked = mouseClicked && !wasMousePressed;

            // ツールチップリセット
            hoveredAdvTitle = null;
            hoveredAdvDesc = null;

            // データ未取得
            if (advState == null) {
                if (!ShannonUIModClient.isAdvancementsRequested()) {
                    ShannonUIModClient.requestAdvancements();
                }
                context.drawTextWithShadow(mc.textRenderer, "読み込み中...", drawX, drawY, COLOR_LOADING);
                state.contentHeight = 30;
                wasMousePressed = mouseClicked;
                return;
            }

            // === ヘッダー: プレイヤー名 ===
            String headerText = "* " + advState.playerName;
            context.drawTextWithShadow(mc.textRenderer, headerText, drawX, drawY, 0xFFFFFFFF);

            // Refreshボタン
            String refreshText = "[更新]";
            int refreshWidth = mc.textRenderer.getWidth(refreshText);
            int refreshX = drawX + maxWidth - refreshWidth;
            context.drawTextWithShadow(mc.textRenderer, refreshText, refreshX, drawY, COLOR_REFRESH);

            if (justClicked && scaledMX >= refreshX && scaledMX <= refreshX + refreshWidth
                    && clickMY >= drawY && clickMY <= drawY + LINE_HEIGHT) {
                ShannonUIModClient.requestAdvancements();
            }

            drawY += LINE_HEIGHT + 2;

            // === 全体統計 ===
            int totalCompleted = 0, totalAll = 0;
            if (advState.categories != null) {
                for (AdvancementsState.Category cat : advState.categories) {
                    totalCompleted += cat.completed;
                    totalAll += cat.total;
                }
            }

            RenderUtils.drawProgressBar(context, drawX, drawY, maxWidth, 8, totalCompleted, totalAll);
            drawY += 11;
            String stats = totalCompleted + "/" + totalAll + " 達成済み";
            context.drawTextWithShadow(mc.textRenderer, stats, drawX, drawY, COLOR_PROGRESS);
            drawY += LINE_HEIGHT + 4;

            // === フィルタボタン ===
            int btnW = 55;
            int btnH = 12;
            int btnGap = 4;
            int btnY = drawY;

            // 全て ボタン
            boolean allClicked = drawFilterButton(context, mc, "全て", drawX, btnY, btnW, btnH,
                    filterMode == FilterMode.ALL, scaledMX, clickMY, justClicked);
            if (allClicked)
                filterMode = FilterMode.ALL;

            // 未達成 ボタン
            boolean incClicked = drawFilterButton(context, mc, "未達成", drawX + btnW + btnGap, btnY, btnW, btnH,
                    filterMode == FilterMode.INCOMPLETE, scaledMX, clickMY, justClicked);
            if (incClicked)
                filterMode = FilterMode.INCOMPLETE;

            // 達成済 ボタン
            boolean compClicked = drawFilterButton(context, mc, "達成済", drawX + (btnW + btnGap) * 2, btnY, btnW, btnH,
                    filterMode == FilterMode.COMPLETE, scaledMX, clickMY, justClicked);
            if (compClicked)
                filterMode = FilterMode.COMPLETE;

            drawY += btnH + 6;

            // === カテゴリ別表示 ===
            if (advState.categories != null) {
                for (AdvancementsState.Category cat : advState.categories) {
                    // フィルタによるカテゴリ表示判定
                    List<AdvancementsState.Advancement> filteredAdvs = filterAdvancements(cat.advancements);
                    if (filteredAdvs.isEmpty() && filterMode != FilterMode.ALL)
                        continue;

                    boolean isCollapsed = collapsedCategories.contains(cat.categoryId);
                    boolean allDone = cat.completed == cat.total && cat.total > 0;

                    // カテゴリヘッダー背景
                    int headerBg = allDone ? COLOR_HEADER_COMPLETE_BG : COLOR_HEADER_BG;
                    context.fill(drawX - 2, drawY - 2, drawX + maxWidth, drawY + LINE_HEIGHT + 2, headerBg);

                    // 折りたたみアイコン + カテゴリ名
                    String collapseIcon = isCollapsed ? "> " : "v ";
                    String catHeader = collapseIcon + cat.displayName + " (" + cat.completed + "/" + cat.total + ")";
                    int catColor = allDone ? COLOR_DONE : COLOR_CATEGORY_TEXT;
                    context.drawTextWithShadow(mc.textRenderer, catHeader, drawX, drawY, catColor);

                    // カテゴリヘッダークリック
                    if (justClicked && scaledMX >= drawX - 2 && scaledMX <= drawX + maxWidth
                            && clickMY >= drawY - 2 && clickMY <= drawY + LINE_HEIGHT + 2) {
                        if (isCollapsed) {
                            collapsedCategories.remove(cat.categoryId);
                        } else {
                            collapsedCategories.add(cat.categoryId);
                        }
                    }

                    drawY += LINE_HEIGHT + 4;

                    // ミニプログレスバー
                    RenderUtils.drawProgressBar(context, drawX, drawY, maxWidth, 4, cat.completed, cat.total);
                    drawY += 7;

                    // 展開時: 個別の進捗を表示（フィルタ適用）
                    if (!isCollapsed) {
                        for (AdvancementsState.Advancement adv : filteredAdvs) {
                            String icon = adv.done ? "[v] " : "[ ] ";
                            int textColor = adv.done ? COLOR_DONE : COLOR_NOT_DONE;

                            Text titleText = parseTextJson(mc, adv.title);
                            String titleStr = titleText != null ? titleText.getString() : adv.title;

                            String advText = icon + titleStr;
                            if (!adv.progress.isEmpty()) {
                                advText += " (" + adv.progress + ")";
                            }

                            int advY = drawY;
                            context.drawTextWithShadow(mc.textRenderer, advText, drawX + 6, drawY, textColor);

                            // ホバー判定（全進捗に対して）
                            int advTextWidth = mc.textRenderer.getWidth(advText);
                            if (scaledMX >= drawX + 6 && scaledMX <= drawX + 6 + advTextWidth
                                    && clickMY >= drawY && clickMY <= drawY + LINE_HEIGHT) {
                                // ホバー中 - ツールチップ情報を保存
                                Text descText = parseTextJson(mc, adv.description);
                                String descStr = descText != null ? descText.getString() : adv.description;
                                if (descStr != null && !descStr.isEmpty()) {
                                    hoveredAdvTitle = titleStr;
                                    hoveredAdvDesc = descStr;
                                    hoveredAdvX = scaledMX;
                                    hoveredAdvY = drawY + LINE_HEIGHT + 2;
                                }

                                // ホバーハイライト
                                context.fill(drawX + 4, drawY - 1, drawX + 6 + advTextWidth + 2, drawY + LINE_HEIGHT,
                                        0x22FFFFFF);
                            }

                            drawY += LINE_HEIGHT;

                            // 未完了の説明文（インラインヒント）- フィルタALLの場合のみ
                            if (!adv.done && !adv.description.isEmpty() && filterMode == FilterMode.ALL) {
                                Text descText = parseTextJson(mc, adv.description);
                                String descStr = descText != null ? descText.getString() : adv.description;
                                String desc = "  " + descStr;

                                if (mc.textRenderer.getWidth(desc) > maxWidth - 12) {
                                    while (mc.textRenderer.getWidth(desc + "...") > maxWidth - 12
                                            && desc.length() > 10) {
                                        desc = desc.substring(0, desc.length() - 1);
                                    }
                                    desc += "...";
                                }
                                context.drawTextWithShadow(mc.textRenderer, desc, drawX + 6, drawY, COLOR_DESCRIPTION);
                                drawY += LINE_HEIGHT;
                            }
                        }
                    }

                    // セパレーター
                    drawY += 3;
                    context.fill(drawX, drawY, drawX + maxWidth, drawY + 1, RenderUtils.COLOR_SEPARATOR);
                    drawY += 5;
                }
            }

            // コンテンツ高さ
            int totalDrawnHeight = drawY - ((int) (4 / SCALE) + yOffset);
            state.contentHeight = (int) (totalDrawnHeight * SCALE) + 16;

            // === ツールチップを最前面に描画 ===
            if (hoveredAdvTitle != null && hoveredAdvDesc != null) {
                List<String> tooltipLines = new ArrayList<>();
                tooltipLines.add(hoveredAdvTitle);
                tooltipLines.add(hoveredAdvDesc);
                RenderUtils.drawTooltip(context, mc, tooltipLines, hoveredAdvX, hoveredAdvY, maxWidth - 20);
            }

            wasMousePressed = mouseClicked;
        } finally {
            context.disableScissor();
            context.getMatrices().pop();
        }
    }

    /**
     * フィルタモードに基づいて進捗をフィルタリング
     */
    private static List<AdvancementsState.Advancement> filterAdvancements(
            List<AdvancementsState.Advancement> advancements) {
        if (advancements == null)
            return new ArrayList<>();
        if (filterMode == FilterMode.ALL)
            return advancements;

        List<AdvancementsState.Advancement> filtered = new ArrayList<>();
        for (AdvancementsState.Advancement adv : advancements) {
            if (filterMode == FilterMode.COMPLETE && adv.done) {
                filtered.add(adv);
            } else if (filterMode == FilterMode.INCOMPLETE && !adv.done) {
                filtered.add(adv);
            }
        }
        return filtered;
    }

    /**
     * フィルタボタンを描画。選択中はハイライト。
     */
    private static boolean drawFilterButton(DrawContext context, MinecraftClient mc,
            String text, int x, int y, int w, int h, boolean selected,
            int mouseX, int mouseY, boolean justClicked) {

        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = selected ? 0xFF336699 : (hover ? RenderUtils.BG_BUTTON_HOVER : RenderUtils.BG_BUTTON);
        context.fill(x, y, x + w, y + h, bg);

        // 枠線
        int topColor = selected ? 0xFF5599CC : (hover ? 0xFF888888 : 0xFF666666);
        context.fill(x, y, x + w, y + 1, topColor);
        context.fill(x, y, x + 1, y + h, topColor);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF222222);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF222222);

        // テキスト
        int tw = mc.textRenderer.getWidth(text);
        int textColor = selected ? 0xFFFFFF : (hover ? 0xFFFFFF : 0xCCCCCC);
        context.drawTextWithShadow(mc.textRenderer, Text.literal(text),
                x + (w - tw) / 2, y + (h - 8) / 2, textColor);

        return justClicked && hover;
    }

    /**
     * JSON形式のTextをクライアントの言語で解決する
     */
    private static Text parseTextJson(MinecraftClient mc, String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty())
            return null;

        if (!jsonStr.startsWith("{") && !jsonStr.startsWith("[") && !jsonStr.startsWith("\"")) {
            return Text.literal(jsonStr);
        }

        try {
            if (mc.world != null) {
                MutableText text = Text.Serialization.fromJson(jsonStr, mc.world.getRegistryManager());
                if (text != null) {
                    return text;
                }
            }
        } catch (Exception e) {
            // JSONパース失敗
        }
        return Text.literal(jsonStr);
    }
}
