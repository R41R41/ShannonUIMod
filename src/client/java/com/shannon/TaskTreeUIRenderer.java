package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.TaskTreeState;
import com.shannon.network.packet.DetailedLogsState;

public class TaskTreeUIRenderer {
    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state,
            DetailedLogsState logsState, com.shannon.network.packet.LogToggleState logToggleState) {
        context.getMatrices().push();
        try {
            int line = 0;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 8;
            int startY = drawY + yOffset;
            final int LINE_HEIGHT = 9;

            context.getMatrices().translate(x, y, 0);

            // === 現在の状態（1行のみ、最上部に固定表示） ===
            String currentStatus = getCurrentStatus(logsState, taskTreeState);
            int statusColor = getStatusColor(logsState, taskTreeState);
            for (OrderedText lineText : wrapText(mc, currentStatus, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, statusColor);
                }
                line++;
                break; // 1行のみ
            }
            line++; // 空行

            // タスクがない場合
            if (taskTreeState == null || taskTreeState.goal == null || taskTreeState.goal.isEmpty()) {
                String noTask = "No active task";
                for (OrderedText lineText : wrapText(mc, noTask, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x888888);
                    }
                    line++;
                }
                state.contentHeight = (line + 1) * LINE_HEIGHT + 8;
                return;
            }

            // === Current Task セクション ===
            String taskHeader = "▼ Current Task";
            for (OrderedText lineText : wrapText(mc, taskHeader, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                }
                line++;
            }

            // ゴール
            String goalLine = "  Goal: " + taskTreeState.goal;
            int goalColor = getTaskStatusColor(taskTreeState.status);
            for (OrderedText lineText : wrapText(mc, goalLine, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, goalColor);
                }
                line++;
            }

            // ストラテジー
            if (taskTreeState.strategy != null && !taskTreeState.strategy.isEmpty()) {
                String strategyLine = "  " + taskTreeState.strategy;
                for (OrderedText lineText : wrapText(mc, strategyLine, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xAAAAAA);
                    }
                    line++;
                }
            }

            // エラー表示
            if (taskTreeState.error != null && !taskTreeState.error.isEmpty()) {
                line++;
                String errorLine = "  [ERROR] " + taskTreeState.error;
                for (OrderedText lineText : wrapText(mc, errorLine, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5555);
                    }
                    line++;
                }
            }

            // === Tasks セクション（階層的サブタスク） ===
            if (taskTreeState.hierarchicalSubTasks != null && !taskTreeState.hierarchicalSubTasks.isEmpty()) {
                line++; // 空行

                String tasksHeader = "▼ Tasks";
                for (OrderedText lineText : wrapText(mc, tasksHeader, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                    }
                    line++;
                }

                for (TaskTreeState.HierarchicalSubTask sub : taskTreeState.hierarchicalSubTasks) {
                    line = renderHierarchicalSubTask(context, mc, sub, line, drawX, startY,
                            LINE_HEIGHT, uiHeight, maxTextWidth, 0);
                }
            }
            // 旧形式のサブタスク（後方互換性）
            else if (taskTreeState.subTasks != null && !taskTreeState.subTasks.isEmpty()) {
                line++; // 空行

                String tasksHeader = "▼ Tasks";
                for (OrderedText lineText : wrapText(mc, tasksHeader, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                    }
                    line++;
                }

                for (TaskTreeState.SubTask sub : taskTreeState.subTasks) {
                    String icon = getSubTaskIcon(sub.subTaskStatus);
                    int color = getTaskStatusColor(sub.subTaskStatus);

                    String subTaskLine = "  " + icon + " " + sub.subTaskGoal;
                    for (OrderedText lineText : wrapText(mc, subTaskLine, maxTextWidth)) {
                        int textY = startY + LINE_HEIGHT * line;
                        if (textY >= 0 && textY + 10 <= uiHeight) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, color);
                        }
                        line++;
                    }

                    if (sub.subTaskResult != null && !sub.subTaskResult.isEmpty()) {
                        String resultLine = "    => " + sub.subTaskResult;
                        for (OrderedText lineText : wrapText(mc, resultLine, maxTextWidth)) {
                            int textY = startY + LINE_HEIGHT * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x88FF88);
                            }
                            line++;
                        }
                    }
                }
            }

            state.contentHeight = (line + 1) * LINE_HEIGHT + 8;

            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.getMatrices().pop();
        }
    }

    /**
     * 現在の状態を取得（最新のログから）
     */
    private static String getCurrentStatus(DetailedLogsState logsState, TaskTreeState taskTreeState) {
        // ログから最新の状態を取得
        if (logsState != null && logsState.logs != null && !logsState.logs.isEmpty()) {
            DetailedLogsState.LogEntry latestLog = logsState.logs.get(logsState.logs.size() - 1);

            // Planning中
            if ("planning".equals(latestLog.phase) && "info".equals(latestLog.level)) {
                return "[*] Planning...";
            }
            // Planning完了
            if ("planning".equals(latestLog.phase) && "success".equals(latestLog.level)) {
                return "[>] Plan ready";
            }
            // 実行中
            if ("execution".equals(latestLog.phase) && "info".equals(latestLog.level)) {
                if (latestLog.metadata != null && latestLog.metadata.actionCount != null) {
                    return "[>] Executing (" + latestLog.metadata.actionCount + " actions)...";
                }
                return "[>] Executing...";
            }
            // 実行成功
            if ("execution".equals(latestLog.phase) && "success".equals(latestLog.level)) {
                return "[+] Action completed";
            }
            // エラー
            if ("error".equals(latestLog.level)) {
                return "[!] Error occurred";
            }
        }

        // タスクステータスから
        if (taskTreeState != null && taskTreeState.status != null) {
            switch (taskTreeState.status.toLowerCase()) {
                case "completed":
                    return "[+] Task completed";
                case "error":
                    return "[!] Task failed";
                case "in_progress":
                    return "[>] Working...";
            }
        }

        return "[-] Idle";
    }

    /**
     * 現在の状態の色を取得
     */
    private static int getStatusColor(DetailedLogsState logsState, TaskTreeState taskTreeState) {
        if (logsState != null && logsState.logs != null && !logsState.logs.isEmpty()) {
            DetailedLogsState.LogEntry latestLog = logsState.logs.get(logsState.logs.size() - 1);

            if ("planning".equals(latestLog.phase)) {
                return 0x5599FF; // 青
            }
            if ("execution".equals(latestLog.phase) && "info".equals(latestLog.level)) {
                return 0xFFFF55; // 黄色
            }
            if ("success".equals(latestLog.level)) {
                return 0x55FF55; // 緑
            }
            if ("error".equals(latestLog.level)) {
                return 0xFF5555; // 赤
            }
        }
        return 0xAAAAAA; // グレー
    }

    /**
     * タスクステータスに応じた色を返す
     */
    private static int getTaskStatusColor(String status) {
        if (status == null)
            return 0xAAAAAA;
        switch (status.toLowerCase()) {
            case "completed":
                return 0x55FF55;
            case "in_progress":
                return 0xFFFF55;
            case "error":
                return 0xFF5555;
            default:
                return 0xAAAAAA;
        }
    }

    /**
     * サブタスクのアイコンを返す
     */
    private static String getSubTaskIcon(String status) {
        if (status == null)
            return "[ ]";
        switch (status.toLowerCase()) {
            case "completed":
                return "[+]";
            case "in_progress":
                return "[>]";
            case "error":
                return "[x]";
            default:
                return "[ ]";
        }
    }

    /**
     * 階層的サブタスクを再帰的にレンダリング
     */
    private static int renderHierarchicalSubTask(DrawContext context, MinecraftClient mc,
            TaskTreeState.HierarchicalSubTask sub, int line, int drawX, int startY,
            int lineHeight, int uiHeight, int maxTextWidth, int depth) {

        // インデント（深さに応じて）
        String indent = "  " + "  ".repeat(depth);

        // ステータスアイコンとカラー
        String icon = sub.getStatusIcon();
        int color = sub.getStatusColor();

        // サブタスク行
        String subTaskLine = indent + icon + " " + sub.goal;
        for (OrderedText lineText : wrapText(mc, subTaskLine, maxTextWidth)) {
            int textY = startY + lineHeight * line;
            if (textY >= 0 && textY + 10 <= uiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, color);
            }
            line++;
        }

        // 結果（完了の場合、短く表示）
        if (sub.result != null && !sub.result.isEmpty() && "completed".equals(sub.status)) {
            String resultIndent = indent + "  ";
            String shortResult = sub.result.length() > 40 ? sub.result.substring(0, 40) + "..." : sub.result;
            String resultLine = resultIndent + "=> " + shortResult;
            for (OrderedText lineText : wrapText(mc, resultLine, maxTextWidth)) {
                int textY = startY + lineHeight * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x88FF88);
                }
                line++;
            }
        }

        // 失敗理由（エラーの場合）
        if (sub.failureReason != null && !sub.failureReason.isEmpty()) {
            String failureIndent = indent + "  ";
            String failureLine = failureIndent + "[x] " + sub.failureReason;
            for (OrderedText lineText : wrapText(mc, failureLine, maxTextWidth)) {
                int textY = startY + lineHeight * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF5555);
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
