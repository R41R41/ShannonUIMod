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

    public static void renderDebug(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, UIRenderer.UIState state, int scrollOffset) {
        context.getMatrices().push();
        try {
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = (int) ((uiWidth - 8) / SCALE);
            int startY = drawY + yOffset;
            final int LINE_HEIGHT = 9; // スケール考慮前の行の高さ
            int scaledUiHeight = (int) (uiHeight / SCALE);

            // ヘッダー
            String header = "Debug Logs";
            for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                }
                line++;
            }
            line++; // 空行

            // DetailedLogsStateからログを取得
            DetailedLogsState logsState = ShannonUIModClient.getDetailedLogsState();

            if (logsState == null || logsState.logs == null || logsState.logs.isEmpty()) {
                String noLogs = "No debug logs available.";
                for (OrderedText lineText : wrapText(mc, noLogs, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x888888);
                    }
                    line++;
                }
                state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;
                return;
            }

            // 全ログを時系列で表示（ターミナル形式）
            for (DetailedLogsState.LogEntry log : logsState.logs) {
                String timestamp = formatTimestamp(log.timestamp);
                int color = getLogColor(log.level);
                String icon = getPhaseIcon(log.phase);

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
                        String paramLine = "    args: " + truncate(log.metadata.parameters, 80);
                        for (OrderedText lineText : wrapText(mc, paramLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x777777);
                            }
                            line++;
                        }
                    }

                    // 実行時間
                    if (log.metadata.duration != null && log.metadata.duration > 0) {
                        String durationLine = "    time: " + log.metadata.duration + "ms";
                        for (OrderedText lineText : wrapText(mc, durationLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x666666);
                            }
                            line++;
                        }
                    }

                    // エラー
                    if (log.metadata.error != null && !log.metadata.error.isEmpty()) {
                        String errorLine = "    err: " + log.metadata.error;
                        for (OrderedText lineText : wrapText(mc, errorLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5555);
                            }
                            line++;
                        }
                    }
                }
            }

            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.getMatrices().pop();
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
            return 0xAAAAAA;
        switch (level.toLowerCase()) {
            case "success":
                return 0x55FF55;
            case "error":
                return 0xFF5555;
            case "warning":
                return 0xFFAA55;
            case "info":
            default:
                return 0xCCCCCC;
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
                return "[PLAN]";
            case "execution":
                return "[EXEC]";
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
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}
