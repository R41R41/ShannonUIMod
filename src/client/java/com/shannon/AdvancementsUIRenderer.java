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

        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledMX = (int) ((mouseX + 4) / SCALE);
            int scaledMY = (int) ((mouseY + 4) / SCALE);
            int scrollComp = (int) (scrollOffset / SCALE);
            int clickMY = scaledMY - scrollComp;
            int scaledWidth = (int) (uiWidth / SCALE);

            int drawX = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int rightEdge = scaledWidth - 16;
            int maxWidth = rightEdge - drawX;
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
            int refreshBtnW = 40;
            int refreshBtnH = 12;
            int refreshX = drawX + maxWidth - refreshBtnW;
            boolean refreshClicked = RenderUtils.drawButton(context, mc, "更新",
                    refreshX, drawY - 2, refreshBtnW, refreshBtnH,
                    scaledMX, clickMY, mouseClicked, wasMousePressed);
            if (refreshClicked) {
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
            drawY += 13;
            String stats = totalCompleted + "/" + totalAll + " 達成済み";
            context.drawTextWithShadow(mc.textRenderer, stats, drawX, drawY, COLOR_PROGRESS);
            drawY += LINE_HEIGHT + 4;

            // === フィルタボタン ===
            int btnW = 55;
            int btnH = 12;
            int btnGap = 4;
            int btnY = drawY;

            // 全て ボタン
            boolean allClicked = RenderUtils.drawButton(context, mc, "全て", drawX, btnY, btnW, btnH,
                    scaledMX, clickMY, mouseClicked, wasMousePressed, filterMode == FilterMode.ALL);
            if (allClicked)
                filterMode = FilterMode.ALL;

            // 未達成 ボタン
            boolean incClicked = RenderUtils.drawButton(context, mc, "未達成", drawX + btnW + btnGap, btnY, btnW, btnH,
                    scaledMX, clickMY, mouseClicked, wasMousePressed, filterMode == FilterMode.INCOMPLETE);
            if (incClicked)
                filterMode = FilterMode.INCOMPLETE;

            // 達成済 ボタン
            boolean compClicked = RenderUtils.drawButton(context, mc, "達成済", drawX + (btnW + btnGap) * 2, btnY, btnW, btnH,
                    scaledMX, clickMY, mouseClicked, wasMousePressed, filterMode == FilterMode.COMPLETE);
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

                    // カテゴリヘッダー（drawSectionHeader使用）
                    String collapseIcon = isCollapsed ? "> " : "v ";
                    String catHeader = collapseIcon + cat.displayName + " (" + cat.completed + "/" + cat.total + ")";
                    int accentColor = allDone ? COLOR_DONE : COLOR_CATEGORY_TEXT;
                    RenderUtils.drawSectionHeader(context, mc, catHeader,
                            drawX - 2, drawY, maxWidth + 2, accentColor);

                    // カテゴリヘッダークリック
                    if (justClicked && scaledMX >= drawX - 2 && scaledMX <= drawX + maxWidth
                            && clickMY >= drawY && clickMY <= drawY + RenderUtils.SECTION_HEADER_HEIGHT) {
                        if (isCollapsed) {
                            collapsedCategories.remove(cat.categoryId);
                        } else {
                            collapsedCategories.add(cat.categoryId);
                        }
                    }

                    drawY += RenderUtils.SECTION_HEADER_HEIGHT + 2;

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
                    drawY += 2;
                    context.fill(drawX, drawY, drawX + maxWidth, drawY + 1, RenderUtils.COLOR_SEPARATOR);
                    drawY += 4;
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
            context.getMatrices().popMatrix();
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
                com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseString(jsonStr);
                var ops = mc.world.getRegistryManager().getOps(com.mojang.serialization.JsonOps.INSTANCE);
                var result = net.minecraft.text.TextCodecs.CODEC.parse(ops, jsonElement);
                if (result.result().isPresent()) {
                    return (MutableText) result.result().get();
                }
            }
        } catch (Exception e) {
            // JSONパース失敗
        }
        return Text.literal(jsonStr);
    }
}
