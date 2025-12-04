package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.TaskTreeState;
import com.shannon.network.packet.DetailedLogsState;
import net.minecraft.text.Style;

public class TaskTreeUIRenderer {
    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state,
            DetailedLogsState logsState, com.shannon.network.packet.LogToggleState logToggleState) {
        context.getMatrices().push();
        try {
            if (taskTreeState == null) {
                state.contentHeight = uiHeight;
                return;
            }
            int line = 0;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 8; // 8pxマージン×2
            int startY = drawY + yOffset;

            context.getMatrices().translate(x, y, 0);

            // 行間定数
            final int LOG_LINE_HEIGHT = 7; // Activity Logs用
            final int TASK_LINE_HEIGHT = 8; // TaskTree用

            // === Activity Logs セクションを先に表示 ===
            if (logsState != null && logsState.logs != null && !logsState.logs.isEmpty() && logToggleState != null) {

                // セパレーターにトグルボタンを追加
                boolean logsExpanded = logToggleState.logsExpanded;
                String toggleIcon = logsExpanded ? "▼" : "▶";
                String separator = String.format("%s Activity Logs (click to %s)",
                        toggleIcon, logsExpanded ? "collapse" : "expand");

                int separatorStartLine = line;
                int separatorStartY = startY + LOG_LINE_HEIGHT * line;
                for (OrderedText lineText : wrapText(mc, separator, maxTextWidth)) {
                    int textY = startY + LOG_LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        // クリック可能な見た目
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                    }
                    line++;
                }

                // セパレーターのY座標を記録（クリック判定用）
                state.logsSeparatorLine = separatorStartLine;
                state.logsSeparatorLineY = separatorStartY;

                // ログが展開されている場合のみ表示
                if (logsExpanded) {
                    line++; // 空行

                    // 最新10件のログを表示
                    int maxLogs = 10;
                    int startIndex = Math.max(0, logsState.logs.size() - maxLogs);

                    for (int i = startIndex; i < logsState.logs.size(); i++) {
                        DetailedLogsState.LogEntry log = logsState.logs.get(i);

                        int logColor = DetailedLogsUIRenderer.getLogColorStatic(log.level);
                        String icon = DetailedLogsUIRenderer.getLogIconStatic(log.phase);
                        String timestamp = DetailedLogsUIRenderer.formatTimestampStatic(log.timestamp);

                        // 個別ログの展開状態
                        boolean logExpanded = logToggleState.isLogExpanded(i);
                        String logToggleIcon = logExpanded ? "▼" : "▶";

                        // ログヘッダー（クリック可能）
                        String logHeader = String.format("%s [%s] %s %s",
                                logToggleIcon, timestamp, icon, log.source);

                        int headerStartLine = line;
                        int headerStartY = startY + LOG_LINE_HEIGHT * line;
                        int headerLineCount = 0;
                        for (OrderedText lineText : wrapText(mc, logHeader, maxTextWidth)) {
                            int textY = startY + LOG_LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, logColor);
                            }
                            line++;
                            headerLineCount++;
                        }
                        int headerEndY = headerStartY + LOG_LINE_HEIGHT * headerLineCount;

                        // クリック可能な行のY座標範囲を記録
                        if (state.logClickableLines == null) {
                            state.logClickableLines = new java.util.HashMap<>();
                        }
                        if (state.logClickableLineYStart == null) {
                            state.logClickableLineYStart = new java.util.HashMap<>();
                        }
                        if (state.logClickableLineYEnd == null) {
                            state.logClickableLineYEnd = new java.util.HashMap<>();
                        }
                        state.logClickableLines.put(headerStartLine, i);
                        state.logClickableLineYStart.put(i, headerStartY);
                        state.logClickableLineYEnd.put(i, headerEndY);

                        // 展開されている場合のみ詳細を表示
                        if (logExpanded) {
                            // コンテンツ（全文表示、インデント）
                            String indentedContent = "  " + log.content;
                            for (OrderedText lineText : wrapText(mc, indentedContent, maxTextWidth)) {
                                int textY = startY + LOG_LINE_HEIGHT * line;
                                if (textY >= 0 && textY + 10 <= uiHeight) {
                                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xCCCCCC);
                                }
                                line++;
                            }

                            // メタデータ
                            if (log.metadata != null) {
                                if (log.metadata.duration != null && log.metadata.duration > 0) {
                                    String durationStr = "    ⏱ " + log.metadata.duration + "ms";
                                    for (OrderedText lineText : wrapText(mc, durationStr, maxTextWidth)) {
                                        int textY = startY + LOG_LINE_HEIGHT * line;
                                        if (textY >= 0 && textY + 10 <= uiHeight) {
                                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY,
                                                    0x888888);
                                        }
                                        line++;
                                    }
                                }

                                if (log.metadata.error != null && !log.metadata.error.isEmpty()) {
                                    String errorStr = "    ❌ " + log.metadata.error;
                                    for (OrderedText lineText : wrapText(mc, errorStr, maxTextWidth)) {
                                        int textY = startY + LOG_LINE_HEIGHT * line;
                                        if (textY >= 0 && textY + 10 <= uiHeight) {
                                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY,
                                                    0xFF8888);
                                        }
                                        line++;
                                    }
                                }
                            }
                        } else {
                            // 折りたたみ時は1行のプレビュー
                            String shortContent = log.content.length() > 60 ? log.content.substring(0, 60) + "..."
                                    : log.content;
                            String indentedContent = "  " + shortContent;
                            for (OrderedText lineText : wrapText(mc, indentedContent, maxTextWidth)) {
                                int textY = startY + LOG_LINE_HEIGHT * line;
                                if (textY >= 0 && textY + 10 <= uiHeight) {
                                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xAAAAAA);
                                }
                                line++;
                                break; // 1行のみ
                            }
                        }

                        line++; // ログ間の空行
                    }
                }
            }

            // === TaskTree セクション（ActivityLogsの後に表示） ===
            line += 2; // 空行

            // TaskTree: scale 0.85でコンパクト表示
            context.getMatrices().push();
            context.getMatrices().scale(0.85f, 0.85f, 1.0f);

            // スケール適用後の座標計算
            int scaledDrawX = (int) (drawX / 0.85f);
            int scaledMaxTextWidth = (int) (maxTextWidth / 0.85f);

            String status = taskTreeState.status == null ? "" : taskTreeState.status.toLowerCase();
            int statusColor = 0xAAAAAA;
            switch (status) {
                case "pending":
                    statusColor = 0xAAAAAA;
                    break;
                case "in_progress":
                    statusColor = 0xFFFF00;
                    break;
                case "completed":
                    statusColor = 0x00FF00;
                    break;
                case "error":
                    statusColor = 0xFF0000;
                    break;
            }

            // セパレーター
            String taskSeparator = "▼ Current Task";
            for (OrderedText lineText : wrapText(mc, taskSeparator, scaledMaxTextWidth)) {
                int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, 0x55AAFF);
                }
                line++;
            }
            line++; // 空行

            // ゴール
            for (OrderedText lineText : wrapText(mc, taskTreeState.goal, scaledMaxTextWidth)) {
                int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, statusColor);
                }
                line++;
            }

            // ストラテジー
            if (taskTreeState.strategy != null && !taskTreeState.strategy.isEmpty()) {
                for (OrderedText lineText : wrapText(mc, taskTreeState.strategy, scaledMaxTextWidth)) {
                    int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                    if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, statusColor);
                    }
                    line++;
                }
            }

            // エラー
            if (taskTreeState.error != null && !taskTreeState.error.isEmpty()) {
                for (OrderedText lineText : wrapText(mc, "Error: " + taskTreeState.error, scaledMaxTextWidth)) {
                    int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                    if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, 0xFF8888);
                    }
                    line++;
                }
            }

            // 階層的サブタスク（新形式）
            if (taskTreeState.hierarchicalSubTasks != null && !taskTreeState.hierarchicalSubTasks.isEmpty()) {
                line++; // 空行

                // タスクセクションヘッダー
                String tasksHeader = "▼ Tasks";
                for (OrderedText lineText : wrapText(mc, tasksHeader, scaledMaxTextWidth)) {
                    int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                    if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, 0x55AAFF);
                    }
                    line++;
                }
                line++; // 空行

                for (TaskTreeState.HierarchicalSubTask sub : taskTreeState.hierarchicalSubTasks) {
                    line = renderHierarchicalSubTask(context, mc, sub, line, scaledDrawX, startY,
                            TASK_LINE_HEIGHT, uiHeight, scaledMaxTextWidth, 0);
                }
            }
            // 旧形式のサブタスク（後方互換性）
            else if (taskTreeState.subTasks != null && !taskTreeState.subTasks.isEmpty()) {
                line++; // 空行

                // タスクセクションヘッダー
                String tasksHeader = "▼ Tasks";
                for (OrderedText lineText : wrapText(mc, tasksHeader, scaledMaxTextWidth)) {
                    int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                    if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, 0x55AAFF);
                    }
                    line++;
                }
                line++; // 空行

                for (TaskTreeState.SubTask sub : taskTreeState.subTasks) {
                    int subColor = 0xAAAAAA;
                    String icon = "□";
                    String subStatus = sub.subTaskStatus == null ? "" : sub.subTaskStatus.toLowerCase();
                    switch (subStatus) {
                        case "pending":
                            subColor = 0xAAAAAA;
                            icon = "□";
                            break;
                        case "in_progress":
                            subColor = 0xFFFF00;
                            icon = "↻";
                            break;
                        case "completed":
                            subColor = 0x00FF00;
                            icon = "✓";
                            break;
                        case "error":
                            subColor = 0xFF5555;
                            icon = "✗";
                            break;
                    }

                    // サブタスクゴール（アイコン付き）
                    String subTaskLine = "  " + icon + " " + sub.subTaskGoal;
                    for (OrderedText lineText : wrapText(mc, subTaskLine, scaledMaxTextWidth)) {
                        int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                        if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, subColor);
                        }
                        line++;
                    }

                    // サブタスク結果（簡潔化）
                    if (sub.subTaskResult != null && !sub.subTaskResult.isEmpty()) {
                        for (OrderedText lineText : wrapText(mc, "    => " + sub.subTaskResult, scaledMaxTextWidth)) {
                            int textY = (int) ((startY + TASK_LINE_HEIGHT * line) / 0.85f);
                            if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, scaledDrawX, textY, 0x8888FF);
                            }
                            line++;
                        }
                    }
                }
            }

            context.getMatrices().pop();

            state.contentHeight = (line + 1) * TASK_LINE_HEIGHT + 8; // 余白

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
     * 階層的サブタスクを再帰的にレンダリング
     */
    private static int renderHierarchicalSubTask(DrawContext context, MinecraftClient mc,
            TaskTreeState.HierarchicalSubTask sub, int line, int drawX, int startY,
            int lineHeight, int uiHeight, int maxTextWidth, int depth) {

        // インデント
        String indent = "  ".repeat(depth + 1);

        // ステータスアイコンとカラー
        String icon = sub.getStatusIcon();
        int color = sub.getStatusColor();

        // サブタスク行
        String subTaskLine = indent + icon + " " + sub.goal;
        for (OrderedText lineText : wrapText(mc, subTaskLine, maxTextWidth)) {
            int textY = (int) ((startY + lineHeight * line) / 0.85f);
            if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, color);
            }
            line++;
        }

        // 失敗理由（エラーの場合）
        if (sub.failureReason != null && !sub.failureReason.isEmpty()) {
            String failureIndent = indent + "  ";
            String failureLine = failureIndent + "✗ " + sub.failureReason;
            for (OrderedText lineText : wrapText(mc, failureLine, maxTextWidth)) {
                int textY = (int) ((startY + lineHeight * line) / 0.85f);
                if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5555);
                }
                line++;
            }
        }

        // 結果（完了の場合）
        if (sub.result != null && !sub.result.isEmpty()) {
            String resultIndent = indent + "  ";
            String resultLine = resultIndent + "=> " + sub.result;
            for (OrderedText lineText : wrapText(mc, resultLine, maxTextWidth)) {
                int textY = (int) ((startY + lineHeight * line) / 0.85f);
                if (textY >= 0 && textY + 10 <= (int) (uiHeight / 0.85f)) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x88FF88);
                }
                line++;
            }
        }

        // 子タスクを再帰的にレンダリング
        if (sub.children != null && !sub.children.isEmpty()) {
            for (TaskTreeState.HierarchicalSubTask child : sub.children) {
                line = renderHierarchicalSubTask(context, mc, child, line, drawX, startY,
                        lineHeight, uiHeight, maxTextWidth, depth + 1);
            }
        }

        return line;
    }

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}