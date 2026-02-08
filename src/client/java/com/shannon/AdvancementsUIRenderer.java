package com.shannon;

import com.shannon.network.packet.AdvancementsState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

/**
 * 進捗（アドバンスメント）タブのUIレンダラー
 * SettingsUIRendererと同じ translate+scale パターンを使用
 */
public class AdvancementsUIRenderer {

    // 折りたたまれているカテゴリのセット
    private static final Set<String> collapsedCategories = new HashSet<>();

    // 定数
    private static final float SCALE = 0.7f;
    private static final int LINE_HEIGHT = 11;

    // 色定義
    private static final int COLOR_HEADER_BG = 0xFF3A3A3A;
    private static final int COLOR_HEADER_COMPLETE_BG = 0xFF2A4A2A;
    private static final int COLOR_CATEGORY_TEXT = 0xFFFFFF00;
    private static final int COLOR_DONE = 0xFF55FF55;
    private static final int COLOR_NOT_DONE = 0xFFAAAAAA;
    private static final int COLOR_PROGRESS = 0xFF55AAFF;
    private static final int COLOR_DESCRIPTION = 0xFF888888;
    private static final int COLOR_SEPARATOR = 0xFF555555;
    private static final int COLOR_LOADING = 0xFFCCCCCC;
    private static final int COLOR_REFRESH = 0xFF88CCFF;

    private static boolean wasMousePressed = false;

    public static void renderAdvancements(DrawContext context, MinecraftClient mc,
            int x, int y, int uiWidth, int uiHeight,
            UIRenderer.UIState state, int scrollOffset,
            AdvancementsState advState,
            int mouseX, int mouseY, boolean mouseClicked) {

        context.getMatrices().push();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            // SettingsUIRendererと同じパターン: translate → scale
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            // マウス座標をスケーリング（+4はパネル内のパディング）
            int scaledMX = (int) ((mouseX + 4) / SCALE);
            // relMouseYにはscrollOffsetが含まれているので、描画座標と合わせるために補正
            // scaledMY = (relMouseY + 4) / SCALE だと drawY より scrollOffset/SCALE だけ大きくなる
            // clickMY = scaledMY - scrollOffset/SCALE で描画座標と一致させる
            int scaledMY = (int) ((mouseY + 4) / SCALE);
            int scrollComp = (int) (scrollOffset / SCALE);
            int clickMY = scaledMY - scrollComp;
            int scaledWidth = (int) (uiWidth / SCALE);

            // 描画開始位置（スクロール考慮）
            int drawX = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxWidth = scaledWidth - 8;
            int drawY = (int) (4 / SCALE) + yOffset;

            boolean justClicked = mouseClicked && !wasMousePressed;

            // まだデータがない場合: リクエスト送信
            if (advState == null) {
                if (!ShannonUIModClient.isAdvancementsRequested()) {
                    ShannonUIModClient.requestAdvancements();
                }
                context.drawTextWithShadow(mc.textRenderer, "Loading...", drawX, drawY, COLOR_LOADING);
                state.contentHeight = 30;
                wasMousePressed = mouseClicked;
                return;
            }

            // === ヘッダー: プレイヤー名 ===
            String header = "* " + advState.playerName;
            context.drawTextWithShadow(mc.textRenderer, header, drawX, drawY, 0xFFFFFFFF);

            // Refreshボタン（ヘッダー右端）
            String refreshText = "[Refresh]";
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

            // プログレスバー（全体）
            drawProgressBar(context, drawX, drawY, maxWidth, 8, totalCompleted, totalAll);
            drawY += 11;
            String stats = totalCompleted + "/" + totalAll + " 達成";
            context.drawTextWithShadow(mc.textRenderer, stats, drawX, drawY, COLOR_PROGRESS);
            drawY += LINE_HEIGHT + 4;

            // === カテゴリ別表示 ===
            if (advState.categories != null) {
                for (AdvancementsState.Category cat : advState.categories) {
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

                    // カテゴリヘッダークリック判定（トグル）
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
                    drawProgressBar(context, drawX, drawY, maxWidth, 4, cat.completed, cat.total);
                    drawY += 7;

                    // 展開時: 個別の進捗を表示
                    if (!isCollapsed && cat.advancements != null) {
                        for (AdvancementsState.Advancement adv : cat.advancements) {
                            String icon = adv.done ? "[v] " : "[ ] ";
                            int textColor = adv.done ? COLOR_DONE : COLOR_NOT_DONE;

                            // TextのJSON→クライアント言語で解決
                            Text titleText = parseTextJson(mc, adv.title);
                            String titleStr = titleText != null ? titleText.getString() : adv.title;

                            String advText = icon + titleStr;
                            if (!adv.progress.isEmpty()) {
                                advText += " (" + adv.progress + ")";
                            }

                            context.drawTextWithShadow(mc.textRenderer, advText, drawX + 6, drawY, textColor);
                            drawY += LINE_HEIGHT;

                            // 未完了の説明文（ヒント）
                            if (!adv.done && !adv.description.isEmpty()) {
                                Text descText = parseTextJson(mc, adv.description);
                                String descStr = descText != null ? descText.getString() : adv.description;
                                String desc = "  " + descStr;

                                // 長すぎる場合は切り詰め
                                if (mc.textRenderer.getWidth(desc) > maxWidth - 12) {
                                    while (mc.textRenderer.getWidth(desc + "...") > maxWidth - 12
                                            && desc.length() > 10) {
                                        desc = desc.substring(0, desc.length() - 1);
                                    }
                                    desc += "...";
                                }
                                context.drawTextWithShadow(mc.textRenderer, desc, drawX + 6, drawY,
                                        COLOR_DESCRIPTION);
                                drawY += LINE_HEIGHT;
                            }
                        }
                    }

                    // セパレーター
                    drawY += 3;
                    context.fill(drawX, drawY, drawX + maxWidth, drawY + 1, COLOR_SEPARATOR);
                    drawY += 5;
                }
            }

            // コンテンツ高さを更新（スクロール用）
            int totalDrawnHeight = drawY - ((int) (4 / SCALE) + yOffset);
            state.contentHeight = (int) (totalDrawnHeight * SCALE) + 16;

            wasMousePressed = mouseClicked;

        } finally {
            context.disableScissor();
            context.getMatrices().pop();
        }
    }

    /**
     * JSON形式のTextをクライアントの言語で解決する
     * JSON解析に失敗したらnullを返す（呼び出し元で元の文字列をフォールバックとして使う）
     */
    private static Text parseTextJson(MinecraftClient mc, String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return null;

        // JSON形式でなければそのまま返す（プレーンテキスト）
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
            // JSONパース失敗 → リテラルテキストとして返す
        }
        return Text.literal(jsonStr);
    }

    /**
     * プログレスバーを描画
     */
    private static void drawProgressBar(DrawContext context, int x, int y, int width, int height,
            int completed, int total) {
        int bgColor = 0xFF222222;
        int borderColor = 0xFF444444;
        int fillColor;

        if (total == 0) {
            fillColor = 0xFF555555;
        } else if (completed == total) {
            fillColor = 0xFF44AA44; // 全達成: 緑
        } else if (completed > total / 2) {
            fillColor = 0xFFAAAA44; // 半分以上: 黄
        } else {
            fillColor = 0xFF4488CC; // それ以外: 青
        }

        // 背景
        context.fill(x, y, x + width, y + height, bgColor);
        // ボーダー
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);

        // 進捗バー
        if (total > 0 && completed > 0) {
            int fillWidth = (int) ((float) completed / total * width);
            fillWidth = Math.max(fillWidth, 1);
            context.fill(x, y + 1, x + fillWidth, y + height - 1, fillColor);
        }
    }
}
