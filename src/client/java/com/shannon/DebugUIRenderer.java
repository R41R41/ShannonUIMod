package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.DetailedLogsState;

/**
 * デバッグタブ用レンダラー
 * ターミナルログ形式で詳細ログを表示（小さい文字サイズ）
 */
public class DebugUIRenderer {
    private static final float SCALE = 0.7f; // 文字サイズを70%に
    private static final int SEARCH_H = 14; // 検索バーの高さ(px)
    private static final int FILTER_H = 13; // フィルターボタン行の高さ(px)
    private static final int HEADER_H = SEARCH_H + FILTER_H; // 固定ヘッダー合計

    // 検索フィルタ
    private static String searchQuery = "";
    private static boolean isSearchFocused = false;
    private static boolean wasMousePressed = false;

    // Phase フィルター ("all", "tool", "thinking", "error")
    private static String phaseFilter = "all";

    // コピー機能
    private static int hoveredLogIndex = -1;
    private static long copyFeedbackExpiry = 0;
    private static int copiedLogIndex = -1;

    public static boolean isSearchFocused() { return isSearchFocused; }

    public static void handleCharTyped(char chr, int modifiers) {
        if (isSearchFocused && chr >= 32 && chr != 127) searchQuery += chr;
    }

    public static void handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (isSearchFocused) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                isSearchFocused = false;
            }
        }
    }

    public static void renderDebug(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, UIRenderer.UIState state, int scrollOffset) {
        renderDebug(context, mc, x, y, uiWidth, uiHeight, state, scrollOffset, 0, 0, false);
    }

    public static void renderDebug(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, UIRenderer.UIState state, int scrollOffset,
            int mouseX, int mouseY, boolean mouseClicked) {

        // ヘッダー（検索バー・フィルターボタン）は固定領域なのでスクロールオフセットを除き、
        // UIRenderer が加算した -4 オフセットを補正する
        int hMouseX = mouseX + 4;
        int hMouseY = mouseY + 4 - scrollOffset;

        // === 検索バー（固定、スクロール外）===
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        try {
            int sbX = 2, sbY = 2, sbW = uiWidth - 10, sbH = SEARCH_H - 2;
            boolean overSearch = hMouseX >= sbX && hMouseX <= sbX + sbW
                    && hMouseY >= sbY && hMouseY <= sbY + sbH;
            if (mouseClicked && !wasMousePressed) isSearchFocused = overSearch;

            int borderColor = isSearchFocused ? 0xFF5599FF : 0xFF444444;
            context.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0xFF111111);
            context.fill(sbX, sbY, sbX + sbW, sbY + 1, borderColor);
            context.fill(sbX, sbY, sbX + 1, sbY + sbH, borderColor);

            String displayText = searchQuery.isEmpty() && !isSearchFocused ? "検索..." : searchQuery;
            int textColor = searchQuery.isEmpty() ? 0xFF555555 : 0xFFFFFFFF;
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(SCALE, SCALE);
            context.drawText(mc.textRenderer,
                    displayText,
                    (int)(4 / SCALE), (int)((sbY + 3) / SCALE),
                    textColor, false);
            context.getMatrices().popMatrix();
        } finally {
            context.getMatrices().popMatrix();
        }

        // === フィルターボタン行 ===
        int errorLogCount = 0;
        {
            DetailedLogsState errState = ShannonUIModClient.getDetailedLogsState();
            if (errState != null && errState.logs != null) {
                for (DetailedLogsState.LogEntry e : errState.logs) {
                    if ("error".equals(e.level)) errorLogCount++;
                }
            }
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y + SEARCH_H);
        try {
            String[] filterLabels = { "全て", "ツール", "思考", errorLogCount > 0 ? "エラー(" + errorLogCount + ")" : "エラー" };
            String[] filterValues = { "all", "tool", "thinking", "error" };
            int btnW = (uiWidth - 12) / 4;
            int btnH = FILTER_H - 2;
            for (int fi = 0; fi < filterLabels.length; fi++) {
                int bx = 2 + fi * (btnW + 1);
                int by = 1;
                boolean isActive = phaseFilter.equals(filterValues[fi]);
                boolean over = hMouseX >= bx && hMouseX <= bx + btnW
                        && hMouseY - SEARCH_H >= by && hMouseY - SEARCH_H <= by + btnH;
                int bg = isActive ? 0xFF335588 : (over ? 0xFF444444 : 0xFF222222);
                int border = isActive ? 0xFF5599FF : (over ? 0xFF666666 : 0xFF444444);
                context.fill(bx, by, bx + btnW, by + btnH, bg);
                context.fill(bx, by, bx + btnW, by + 1, border);
                context.fill(bx, by, bx + 1, by + btnH, border);
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(SCALE, SCALE);
                int labelColor = isActive ? 0xFF88CCFF : (over ? 0xFFFFFFFF : 0xFFAAAAAA);
                int labelW = mc.textRenderer.getWidth(filterLabels[fi]);
                int scaledBtnW = (int) (btnW / SCALE);
                int labelX = (int) (bx / SCALE) + (scaledBtnW - labelW) / 2;
                int labelY = (int) (by / SCALE) + ((int)(btnH / SCALE) - 8) / 2;
                context.drawText(mc.textRenderer, filterLabels[fi],
                        labelX, labelY, labelColor, false);
                context.getMatrices().popMatrix();
                if (over && mouseClicked && !wasMousePressed) {
                    phaseFilter = filterValues[fi];
                }
            }
        } finally {
            context.getMatrices().popMatrix();
        }

        boolean mouseJustClicked = mouseClicked && !wasMousePressed;
        wasMousePressed = mouseClicked;

        int logY = y + HEADER_H;
        int logH = uiHeight - HEADER_H;

        context.getMatrices().pushMatrix();
        context.enableScissor(x, logY, x + uiWidth, logY + logH);
        try {
            context.getMatrices().translate(x, logY);
            context.getMatrices().scale(SCALE, SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int rightEdge = scaledUiWidth - 16;
            int maxTextWidth = rightEdge - drawX;
            int startY = drawY + yOffset;
            final int LINE_HEIGHT = 9; // スケール考慮前の行の高さ
            int scaledUiHeight = (int) (logH / SCALE);

            // DetailedLogsStateからログを取得
            DetailedLogsState logsState = ShannonUIModClient.getDetailedLogsState();

            if (logsState == null || logsState.logs == null || logsState.logs.isEmpty()) {
                String noLogs = "デバッグログはありません。";
                for (OrderedText lineText : wrapText(mc, noLogs, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF888888);
                    }
                    line++;
                }
                state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;
                return;
            }

            // フィルタ適用（検索 + phaseフィルター）
            java.util.List<DetailedLogsState.LogEntry> filteredLogs = new java.util.ArrayList<>(logsState.logs);
            // phase フィルター
            if (!"all".equals(phaseFilter)) {
                java.util.List<DetailedLogsState.LogEntry> phaseFiltered = new java.util.ArrayList<>();
                for (DetailedLogsState.LogEntry entry : filteredLogs) {
                    boolean match;
                    switch (phaseFilter) {
                        case "tool":
                            match = entry.phase != null && entry.phase.toLowerCase().contains("tool");
                            break;
                        case "thinking":
                            match = entry.phase != null && java.util.Arrays.asList(
                                    "thinking", "planning", "reflection", "understanding", "execution"
                            ).contains(entry.phase.toLowerCase());
                            break;
                        case "error":
                            match = "error".equals(entry.level);
                            break;
                        default:
                            match = true;
                    }
                    if (match) phaseFiltered.add(entry);
                }
                filteredLogs = phaseFiltered;
            }
            if (!searchQuery.isEmpty()) {
                String lower = searchQuery.toLowerCase();
                java.util.List<DetailedLogsState.LogEntry> searched = new java.util.ArrayList<>();
                for (DetailedLogsState.LogEntry entry : filteredLogs) {
                    boolean match = (entry.content != null && entry.content.toLowerCase().contains(lower))
                            || (entry.source != null && entry.source.toLowerCase().contains(lower))
                            || (entry.phase != null && entry.phase.toLowerCase().contains(lower));
                    if (match) searched.add(entry);
                }
                filteredLogs = searched;
            }
            if (filteredLogs.isEmpty()) {
                String noMatch = searchQuery.isEmpty()
                        ? "フィルターに一致するログはありません"
                        : "\"" + searchQuery + "\" に一致するログはありません";
                for (OrderedText lineText : wrapText(mc, noMatch, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF888888);
                    }
                    line++;
                }
                state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;
                return;
            }

            // マウス座標（スケール後座標系）
            // mouseX/mouseY は UIRenderer が計算した相対座標（mouseY には scrollOffset が加算済み）
            // ログ領域は y + HEADER_H から始まるため、HEADER_H 分を差し引く必要がある
            float scaledMouseX = (mouseX + 4) / SCALE;
            float viewportMouseY = (mouseY + 4 - scrollOffset - HEADER_H) / SCALE;
            boolean mouseInLogArea = viewportMouseY >= 0 && viewportMouseY < scaledUiHeight
                    && scaledMouseX >= drawX && scaledMouseX <= drawX + maxTextWidth;

            int newHoveredIndex = -1;

            // 全ログを時系列で表示（ターミナル形式）
            int logIndex = 0;
            for (DetailedLogsState.LogEntry log : filteredLogs) {
                String timestamp = formatTimestamp(log.timestamp);
                int color = getLogColor(log.level);
                String icon = getPhaseIcon(log.phase);

                int entryStartLine = line;

                // ログ行: [HH:MM:SS] [ICON] source: content
                String logLine = String.format("[%s] %s %s: %s",
                        timestamp, icon, log.source, log.content);

                for (OrderedText lineText : wrapText(mc, logLine, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, color);
                    }
                    line++;
                }

                // メタデータがあれば追加表示
                if (log.metadata != null) {
                    // パラメータ
                    if (log.metadata.parameters != null && !log.metadata.parameters.isEmpty()) {
                        String paramLine = "    引数: " + truncate(log.metadata.parameters, 80);
                        for (OrderedText lineText : wrapText(mc, paramLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF777777);
                            }
                            line++;
                        }
                    }

                    // 実行時間
                    if (log.metadata.duration != null && log.metadata.duration > 0) {
                        String durationLine = "    実行時間: " + log.metadata.duration + "ms";
                        for (OrderedText lineText : wrapText(mc, durationLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF666666);
                            }
                            line++;
                        }
                    }

                    // エラー
                    if (log.metadata.error != null && !log.metadata.error.isEmpty()) {
                        String errorLine = "    エラー: " + log.metadata.error;
                        for (OrderedText lineText : wrapText(mc, errorLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFF5555);
                            }
                            line++;
                        }
                    }
                }

                // ホバー判定（viewportMouseY を使用: scrollOffset 補正済み）
                if (mouseInLogArea) {
                    float entryTopY = startY + entryStartLine * LINE_HEIGHT;
                    float entryBottomY = startY + line * LINE_HEIGHT;
                    if (viewportMouseY >= entryTopY && viewportMouseY < entryBottomY) {
                        newHoveredIndex = logIndex;
                    }
                }

                // ホバー中のログにハイライト + コピーラベル
                if (hoveredLogIndex == logIndex) {
                    int highlightTop = startY + entryStartLine * LINE_HEIGHT - 1;
                    int highlightBottom = startY + line * LINE_HEIGHT;
                    if (highlightTop < scaledUiHeight && highlightBottom > 0) {
                        context.fill(drawX - 2, highlightTop, rightEdge, highlightBottom, 0x18FFFFFF);
                        boolean showCopied = copiedLogIndex == logIndex
                                && System.currentTimeMillis() < copyFeedbackExpiry;
                        String copyLabel = showCopied ? "Copied!" : "[Copy]";
                        int copyColor = showCopied ? 0xFF55FF55 : 0xFFAAAAFF;
                        int labelW = mc.textRenderer.getWidth(copyLabel);
                        int labelX = drawX + maxTextWidth - labelW;
                        int labelY = highlightTop;
                        context.fill(labelX - 2, labelY - 1, labelX + labelW + 2, labelY + 9, 0xCC000000);
                        context.drawTextWithShadow(mc.textRenderer, Text.literal(copyLabel),
                                labelX, labelY, copyColor);
                    }
                }

                logIndex++;
            }

            hoveredLogIndex = newHoveredIndex;

            // クリックでコピー
            if (mouseJustClicked && hoveredLogIndex >= 0
                    && hoveredLogIndex < filteredLogs.size()) {
                DetailedLogsState.LogEntry entry = filteredLogs.get(hoveredLogIndex);
                StringBuilder copyText = new StringBuilder();
                copyText.append("[").append(formatTimestamp(entry.timestamp)).append("] ")
                        .append(getPhaseIcon(entry.phase)).append(" ")
                        .append(entry.source).append(": ").append(entry.content);
                if (entry.metadata != null) {
                    if (entry.metadata.parameters != null && !entry.metadata.parameters.isEmpty()) {
                        copyText.append("\n引数: ").append(entry.metadata.parameters);
                    }
                    if (entry.metadata.duration != null && entry.metadata.duration > 0) {
                        copyText.append("\n実行時間: ").append(entry.metadata.duration).append("ms");
                    }
                    if (entry.metadata.error != null && !entry.metadata.error.isEmpty()) {
                        copyText.append("\nエラー: ").append(entry.metadata.error);
                    }
                }
                mc.keyboard.setClipboard(copyText.toString());
                copiedLogIndex = hoveredLogIndex;
                copyFeedbackExpiry = System.currentTimeMillis() + 1500;
            }

            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            if (state.contentHeight <= logH) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.disableScissor();
            context.getMatrices().popMatrix();
        }
    }

    /**
     * ISO 8601タイムスタンプをJST HH:MM:SS形式に変換
     */
    private static String formatTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "??:??:??";
        }
        try {
            // ISO 8601形式をパースしてJSTに変換
            if (isoTimestamp.contains("T") && isoTimestamp.length() >= 19) {
                java.time.ZonedDateTime utcTime = java.time.ZonedDateTime.parse(isoTimestamp.replace("Z", "+00:00"));
                java.time.ZonedDateTime jstTime = utcTime.withZoneSameInstant(java.time.ZoneId.of("Asia/Tokyo"));
                return String.format("%02d:%02d:%02d", jstTime.getHour(), jstTime.getMinute(), jstTime.getSecond());
            }
            return isoTimestamp.length() >= 8 ? isoTimestamp.substring(0, 8) : isoTimestamp;
        } catch (Exception e) {
            // パースに失敗した場合は元の文字列から抽出
            try {
                if (isoTimestamp.contains("T") && isoTimestamp.length() >= 19) {
                    return isoTimestamp.substring(11, 19);
                }
            } catch (Exception e2) {
                // 無視
            }
            return "??:??:??";
        }
    }

    /**
     * ログレベルに応じた色を返す
     */
    private static int getLogColor(String level) {
        if (level == null)
            return 0xFFAAAAAA;
        switch (level.toLowerCase()) {
            case "success":
                return 0xFF55FF55;
            case "error":
                return 0xFFFF5555;
            case "warning":
                return 0xFFFFAA55;
            case "info":
            default:
                return 0xFFCCCCCC;
        }
    }

    /**
     * フェーズに応じたアイコンを返す
     * 2ノード構成: planning + execution のみ
     */
    private static String getPhaseIcon(String phase) {
        if (phase == null)
            return "*";
        switch (phase.toLowerCase()) {
            case "planning":
                return "[計画]";
            case "execution":
                return "[実行]";
            default:
                return "[" + phase.toUpperCase().substring(0, Math.min(4, phase.length())) + "]";
        }
    }

    /**
     * 文字列を切り詰める
     */
    private static String truncate(String str, int maxLen) {
        if (str == null)
            return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    private static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        return RenderUtils.wrapText(mc, text, maxWidth);
    }
}
