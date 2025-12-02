package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.DetailedLogsState;
import com.shannon.network.packet.DetailedLogsState.LogEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DetailedLogsUIRenderer {
    public static void renderDetailedLogsUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, DetailedLogsState logsState, int scrollOffset, UIRenderer.UIState state) {
        context.getMatrices().push();
        try {
            if (logsState == null || logsState.logs == null || logsState.logs.isEmpty()) {
                // ログがない場合
                state.contentHeight = uiHeight;

                int drawX = 4;
                int drawY = 4;
                context.getMatrices().translate(x, y, 0);
                context.drawTextWithShadow(mc.textRenderer, Text.literal("No detailed logs available."),
                        drawX, drawY, 0xAAAAAA);

                return;
            }

            int line = 0;
            float scale = 1.0f;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 8; // 8pxマージン×2
            int startY = drawY + yOffset;

            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);

            // ログを古い順に表示（最新のログは一番下）
            for (LogEntry log : logsState.logs) {
                // フェーズに応じた色とアイコン
                int color = getLogColor(log.level);
                String icon = getLogIcon(log.phase);

                // タイムスタンプをフォーマット
                String timestamp = formatTimestamp(log.timestamp);

                // ログのヘッダー: [timestamp] icon phase - source
                String header = String.format("[%s] %s %s - %s",
                        timestamp, icon, capitalizeFirst(log.phase), log.source);

                // ヘッダーを描画
                for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
                    int textY = startY + 10 * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, color);
                    }
                    line++;
                }

                // コンテンツを描画（インデント）
                String content = "  " + log.content;
                for (OrderedText lineText : wrapText(mc, content, maxTextWidth)) {
                    int textY = startY + 10 * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xCCCCCC);
                    }
                    line++;
                }

                // メタデータがあれば表示（さらにインデント）
                if (log.metadata != null) {
                    if (log.metadata.duration != null && log.metadata.duration > 0) {
                        String durationStr = "    ⏱ " + log.metadata.duration + "ms";
                        for (OrderedText lineText : wrapText(mc, durationStr, maxTextWidth)) {
                            int textY = startY + 10 * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x888888);
                            }
                            line++;
                        }
                    }

                    if (log.metadata.error != null && !log.metadata.error.isEmpty()) {
                        String errorStr = "    ❌ Error: " + log.metadata.error;
                        for (OrderedText lineText : wrapText(mc, errorStr, maxTextWidth)) {
                            int textY = startY + 10 * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF8888);
                            }
                            line++;
                        }
                    }
                }

                // ログ間に空行
                line++;
            }

            state.contentHeight = (line + 1) * 10 + 8; // 16px余白

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.getMatrices().pop();
        }
    }

    /**
     * レベルに応じた色を返す（外部から呼べるようにpublic static版も追加）
     */
    public static int getLogColorStatic(String level) {
        return getLogColor(level);
    }

    /**
     * レベルに応じた色を返す
     */
    private static int getLogColor(String level) {
        if (level == null)
            return 0xAAAAAA;

        switch (level.toLowerCase()) {
            case "success":
                return 0x00FF00; // 緑
            case "error":
                return 0xFF0000; // 赤
            case "warning":
                return 0xFFAA00; // オレンジ
            case "info":
            default:
                return 0x5555FF; // 青
        }
    }

    /**
     * フェーズに応じたアイコンを返す（外部から呼べるようにpublic static版も追加）
     */
    public static String getLogIconStatic(String phase) {
        return getLogIcon(phase);
    }

    /**
     * フェーズに応じたアイコンを返す
     */
    private static String getLogIcon(String phase) {
        if (phase == null)
            return "•";

        switch (phase.toLowerCase()) {
            case "thinking":
                return "🤔";
            case "tool_call":
                return "🔧";
            case "tool_result":
                return "✓";
            case "reflection":
                return "💭";
            case "planning":
                return "📋";
            case "understanding":
                return "🧠";
            default:
                return "•";
        }
    }

    /**
     * ISO 8601タイムスタンプをHH:MM:SS形式に変換（外部から呼べるようにpublic static版も追加）
     */
    public static String formatTimestampStatic(String isoTimestamp) {
        return formatTimestamp(isoTimestamp);
    }

    /**
     * ISO 8601タイムスタンプをHH:MM:SS形式に変換
     */
    private static String formatTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "??:??:??";
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoTimestamp);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            return timeFormat.format(date);
        } catch (Exception e) {
            // パースに失敗したら最初の8文字を返す
            return isoTimestamp.length() >= 8 ? isoTimestamp.substring(0, 8) : isoTimestamp;
        }
    }

    /**
     * 文字列の最初の文字を大文字にする
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * テキストを折り返す
     */
    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}
