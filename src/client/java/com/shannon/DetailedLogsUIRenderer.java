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
        context.getMatrices().pushMatrix();
        try {
            if (logsState == null || logsState.logs == null || logsState.logs.isEmpty()) {
                // ログがない場合
                state.contentHeight = uiHeight;

                int drawX = 4;
                int drawY = 4;
                context.getMatrices().translate(x, y);
                context.drawTextWithShadow(mc.textRenderer, Text.literal("No detailed logs available."),
                        drawX, drawY, 0xFFAAAAAA);

                return;
            }

            int line = 0;
            float scale = 1.0f;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 8; // 8pxマージン×2
            int startY = drawY + yOffset;

            context.getMatrices().translate(x, y);
            context.getMatrices().scale(scale, scale);

            // ログを古い順に表示（最新のログは一番下）
            for (LogEntry log : logsState.logs) {
                // Planning結果は専用表示
                if ("planning".equals(log.phase) && "success".equals(log.level)
                        && log.metadata != null && log.metadata.goal != null) {
                    line = renderPlanningResult(context, mc, log, startY, drawX, line, maxTextWidth, uiHeight);
                    line++; // 空行
                    continue;
                }

                // Planning開始（ローディング）は専用表示
                if ("planning".equals(log.phase) && "info".equals(log.level)
                        && log.metadata != null && "loading".equals(log.metadata.status)) {
                    line = renderPlanningLoading(context, mc, log, startY, drawX, line, maxTextWidth, uiHeight);
                    line++; // 空行
                    continue;
                }

                // Execution開始（ローディング）は専用表示
                if ("execution".equals(log.phase) && "info".equals(log.level)
                        && log.metadata != null && "loading".equals(log.metadata.status)) {
                    line = renderExecutionLoading(context, mc, log, startY, drawX, line, maxTextWidth, uiHeight);
                    line++; // 空行
                    continue;
                }

                // Execution成功は専用表示
                if ("execution".equals(log.phase) && "success".equals(log.level)
                        && log.content != null && log.content.contains("actions completed")) {
                    line = renderExecutionSuccess(context, mc, log, startY, drawX, line, maxTextWidth, uiHeight);
                    line++; // 空行
                    continue;
                }

                // 通常のログ表示
                int color = getLogColor(log.level);
                String icon = getLogIcon(log.phase);
                String timestamp = formatTimestamp(log.timestamp);

                // ヘッダー: [timestamp] icon phase - source
                String header = String.format("[%s] %s %s - %s",
                        timestamp, icon, capitalizeFirst(log.phase), log.source);

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
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFCCCCCC);
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
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF888888);
                            }
                            line++;
                        }
                    }

                    if (log.metadata.error != null && !log.metadata.error.isEmpty()) {
                        String errorStr = "    ❌ Error: " + log.metadata.error;
                        for (OrderedText lineText : wrapText(mc, errorStr, maxTextWidth)) {
                            int textY = startY + 10 * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFF8888);
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
            context.getMatrices().popMatrix();
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

    /**
     * Planning結果を専用表示（Cursor Agent風）
     */
    private static int renderPlanningResult(DrawContext context, MinecraftClient mc, LogEntry log,
            int startY, int drawX, int line, int maxTextWidth, int uiHeight) {
        String timestamp = formatTimestamp(log.timestamp);

        // ヘッダー: [timestamp] 🧠 Planning - planning_node
        String header = String.format("[%s] 🧠 Planning - %s", timestamp, log.source);
        for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5599FF);
            }
            line++;
        }

        // ✅ Plan created: goal
        String content = "  ✅ " + log.content;
        for (OrderedText lineText : wrapText(mc, content, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF55FF55);
            }
            line++;
        }

        // 空行
        line++;

        // 📋 Strategy:
        if (log.metadata.strategy != null && !log.metadata.strategy.isEmpty()) {
            String strategyText = "  📋 Strategy: " + log.metadata.strategy;
            for (OrderedText lineText : wrapText(mc, strategyText, maxTextWidth)) {
                int textY = startY + 10 * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFFF55);
                }
                line++;
            }
            line++; // 空行
        }

        // ⚡ Actions:
        if (log.metadata.actionCount != null && log.metadata.actionCount > 0) {
            String actionsHeader = String.format("  ⚡ Actions (%d):", log.metadata.actionCount);
            for (OrderedText lineText : wrapText(mc, actionsHeader, maxTextWidth)) {
                int textY = startY + 10 * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFAA55);
                }
                line++;
            }

            // actionSequence を表示
            if (log.metadata.actionSequence != null) {
                try {
                    String actionsJson = log.metadata.actionSequence.toString();
                    // 簡易JSONパース（List<Map>形式を想定）
                    if (log.metadata.actionSequence instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> actions = (java.util.List<Object>) log.metadata.actionSequence;
                        int actionIndex = 1;
                        for (Object actionObj : actions) {
                            if (actionObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> action = (java.util.Map<String, Object>) actionObj;
                                String toolName = action.get("toolName") != null ? action.get("toolName").toString()
                                        : "unknown";
                                String expectedResult = action.get("expectedResult") != null
                                        ? action.get("expectedResult").toString()
                                        : "";

                                String actionLine = String.format("   %d. %s → %s", actionIndex, toolName,
                                        expectedResult);
                                for (OrderedText lineText : wrapText(mc, actionLine, maxTextWidth)) {
                                    int textY = startY + 10 * line;
                                    if (textY >= 0 && textY + 10 <= uiHeight) {
                                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFCCCCCC);
                                    }
                                    line++;
                                }
                                actionIndex++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // フォールバック: 数のみ表示
                    String actionsSummary = String.format("   %d actions planned", log.metadata.actionCount);
                    for (OrderedText lineText : wrapText(mc, actionsSummary, maxTextWidth)) {
                        int textY = startY + 10 * line;
                        if (textY >= 0 && textY + 10 <= uiHeight) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFCCCCCC);
                        }
                        line++;
                    }
                }
            }
            line++; // 空行
        }

        // 📌 SubTasks:
        if (log.metadata.subTaskCount != null && log.metadata.subTaskCount > 0) {
            String subTasksHeader = String.format("  📌 SubTasks (%d):", log.metadata.subTaskCount);
            for (OrderedText lineText : wrapText(mc, subTasksHeader, maxTextWidth)) {
                int textY = startY + 10 * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF55AAFF);
                }
                line++;
            }

            // subTasks を表示
            if (log.metadata.subTasks != null) {
                try {
                    if (log.metadata.subTasks instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> subTasks = (java.util.List<Object>) log.metadata.subTasks;
                        int taskIndex = 1;
                        for (Object taskObj : subTasks) {
                            if (taskObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> task = (java.util.Map<String, Object>) taskObj;
                                String status = task.get("subTaskStatus") != null ? task.get("subTaskStatus").toString()
                                        : "pending";
                                String goal = task.get("subTaskGoal") != null ? task.get("subTaskGoal").toString()
                                        : "unknown";

                                String icon = getSubTaskIcon(status);
                                String taskLine = String.format("   %s [%s] %s", icon, status, goal);

                                // 状態に応じた色
                                int taskColor = status.equals("completed") ? 0x55FF55
                                        : status.equals("error") ? 0xFF5555
                                                : status.equals("in_progress") ? 0xFFFF55 : 0xCCCCCC;

                                for (OrderedText lineText : wrapText(mc, taskLine, maxTextWidth)) {
                                    int textY = startY + 10 * line;
                                    if (textY >= 0 && textY + 10 <= uiHeight) {
                                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, taskColor);
                                    }
                                    line++;
                                }
                                taskIndex++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // フォールバック: 数のみ表示
                    String subTasksSummary = String.format("   %d subtasks defined", log.metadata.subTaskCount);
                    for (OrderedText lineText : wrapText(mc, subTasksSummary, maxTextWidth)) {
                        int textY = startY + 10 * line;
                        if (textY >= 0 && textY + 10 <= uiHeight) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFCCCCCC);
                        }
                        line++;
                    }
                }
            }
        }

        return line;
    }

    /**
     * Planning開始（ローディング）を専用表示
     */
    private static int renderPlanningLoading(DrawContext context, MinecraftClient mc, LogEntry log,
            int startY, int drawX, int line, int maxTextWidth, int uiHeight) {
        String timestamp = formatTimestamp(log.timestamp);

        // ヘッダー: [timestamp] 🧠 Planning - planning_node
        String header = String.format("[%s] 🧠 Planning - %s", timestamp, log.source);
        for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5599FF);
            }
            line++;
        }

        // 🤔 Thinking... ⟳
        String loadingIcon = getLoadingIcon();
        String content = "  🤔 Thinking... " + loadingIcon;
        for (OrderedText lineText : wrapText(mc, content, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFFF55);
            }
            line++;
        }

        return line;
    }

    /**
     * ローディングアイコンを取得（アニメーション）
     */
    private static String getLoadingIcon() {
        long time = System.currentTimeMillis() / 200; // 200msごとに切り替え
        String[] icons = { "⟳", "◐", "◓", "◑", "◒" };
        return icons[(int) (time % icons.length)];
    }

    /**
     * SubTask状態に応じたアイコンを取得
     */
    private static String getSubTaskIcon(String status) {
        if (status == null)
            return "▶";
        switch (status) {
            case "pending":
                return "▶";
            case "in_progress":
                return getLoadingIcon();
            case "completed":
                return "✓";
            case "error":
                return "✗";
            default:
                return "▶";
        }
    }

    /**
     * Execution開始（ローディング）を専用表示
     */
    private static int renderExecutionLoading(DrawContext context, MinecraftClient mc, LogEntry log,
            int startY, int drawX, int line, int maxTextWidth, int uiHeight) {
        String timestamp = formatTimestamp(log.timestamp);

        // ヘッダー: [timestamp] ⚙️ Execution - custom_tool_node
        String header = String.format("[%s] ⚙️ Execution - %s", timestamp, log.source);
        for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFAA55);
            }
            line++;
        }

        // 🔄 Executing... ⟳
        String loadingIcon = getLoadingIcon();
        int actionCount = log.metadata.actionCount != null ? log.metadata.actionCount : 0;
        String content = String.format("  🔄 Executing %d actions... %s", actionCount, loadingIcon);
        for (OrderedText lineText : wrapText(mc, content, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFFF55);
            }
            line++;
        }

        return line;
    }

    /**
     * Execution成功を専用表示
     */
    private static int renderExecutionSuccess(DrawContext context, MinecraftClient mc, LogEntry log,
            int startY, int drawX, int line, int maxTextWidth, int uiHeight) {
        String timestamp = formatTimestamp(log.timestamp);

        // ヘッダー: [timestamp] ⚙️ Execution - custom_tool_node
        String header = String.format("[%s] ⚙️ Execution - %s", timestamp, log.source);
        for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFFFAA55);
            }
            line++;
        }

        // ✅ All actions completed
        String content = "  " + log.content;
        for (OrderedText lineText : wrapText(mc, content, maxTextWidth)) {
            int textY = startY + 10 * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF55FF55);
            }
            line++;
        }

        return line;
    }
}
