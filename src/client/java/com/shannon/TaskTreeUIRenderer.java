package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.shannon.network.packet.TaskTreeState;
import com.shannon.network.packet.TaskListStatePacket;
import com.shannon.network.packet.TaskActionPacket;
import com.shannon.network.packet.DetailedLogsState;

import java.util.List;

/**
 * タスク状態UIレンダラー
 * タスクリスト（最大3つ + 緊急1つ）と選択されたタスクの詳細を表示
 */
public class TaskTreeUIRenderer {
    private static final float SCALE = 0.7f;
    private static final int LINE_HEIGHT = 9;
    private static final int TASK_ITEM_HEIGHT = 20;
    private static final int BUTTON_SIZE = 14;

    // ホバー状態
    private static String hoveredTaskId = null;
    private static String hoveredButton = null; // "play" or "delete"

    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state,
            DetailedLogsState logsState, com.shannon.network.packet.LogToggleState logToggleState) {

        context.getMatrices().push();
        try {
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = (int) ((uiWidth - 8) / SCALE);
            int startY = drawY + yOffset;
            int scaledUiHeight = (int) (uiHeight / SCALE);
            int currentY = startY;

            // クライアントからタスクリストを取得
            TaskListStatePacket.TaskListState taskListState = ShannonUIModClient.getTaskListState();
            String selectedTaskId = ShannonUIModClient.getSelectedTaskId();

            // === タスクリストセクション ===
            String header = "== Task List ==";
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal(header), drawX, currentY, 0x55AAFF);
            }
            currentY += LINE_HEIGHT + 4;

            // タスクがない場合
            if (taskListState == null || (taskListState.tasks == null || taskListState.tasks.isEmpty())
                    && taskListState.emergencyTask == null) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("No tasks"), drawX, currentY, 0x888888);
                }
                currentY += LINE_HEIGHT * 2;
            } else {
                // 通常タスク
                if (taskListState.tasks != null) {
                    for (TaskListStatePacket.TaskListState.TaskInfo task : taskListState.tasks) {
                        currentY = renderTaskItem(context, mc, task.id, task.goal, task.status, false,
                                selectedTaskId != null && selectedTaskId.equals(task.id),
                                drawX, currentY, maxTextWidth, scaledUiHeight);
                    }
                }

                // 緊急タスク
                if (taskListState.emergencyTask != null) {
                    currentY = renderTaskItem(context, mc,
                            taskListState.emergencyTask.id,
                            taskListState.emergencyTask.goal,
                            "emergency", true,
                            selectedTaskId != null && selectedTaskId.equals(taskListState.emergencyTask.id),
                            drawX, currentY, maxTextWidth, scaledUiHeight);
                }
            }

            currentY += LINE_HEIGHT;

            // === 選択されたタスクの詳細セクション ===
            String detailHeader = "== Task Details ==";
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal(detailHeader), drawX, currentY, 0x55AAFF);
            }
            currentY += LINE_HEIGHT + 4;

            // 選択されたタスクがあれば詳細を表示
            if (taskTreeState != null && taskTreeState.goal != null && !taskTreeState.goal.isEmpty()) {
                currentY = renderTaskDetails(context, mc, taskTreeState, drawX, currentY, maxTextWidth, scaledUiHeight);
            } else {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("Select a task to see details"), drawX,
                            currentY, 0x888888);
                }
                currentY += LINE_HEIGHT;
            }

            state.contentHeight = (int) ((currentY - startY + 20) * SCALE);

            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.getMatrices().pop();
        }
    }

    /**
     * タスクアイテムをレンダリング
     */
    private static int renderTaskItem(DrawContext context, MinecraftClient mc,
            String taskId, String goal, String status, boolean isEmergency, boolean isSelected,
            int drawX, int currentY, int maxTextWidth, int scaledUiHeight) {

        if (currentY < 0 || currentY + TASK_ITEM_HEIGHT > scaledUiHeight) {
            return currentY + TASK_ITEM_HEIGHT + 2;
        }

        // 背景（選択/ホバー状態で色を変える）
        boolean isHovered = hoveredTaskId != null && hoveredTaskId.equals(taskId);
        int bgColor;
        if (isSelected) {
            bgColor = isHovered ? 0x55AACCFF : 0x4488AAFF; // 選択中（ホバーでさらに明るく）
        } else if (isHovered) {
            bgColor = 0x44FFFFFF; // ホバー中
        } else {
            bgColor = 0x22FFFFFF; // 通常
        }
        context.fill(drawX - 2, currentY - 2, drawX + maxTextWidth, currentY + TASK_ITEM_HEIGHT - 2, bgColor);

        // ホバー中は枠線を追加
        if (isHovered) {
            int borderColor = 0x88FFFFFF;
            context.fill(drawX - 2, currentY - 2, drawX + maxTextWidth, currentY - 1, borderColor); // 上
            context.fill(drawX - 2, currentY + TASK_ITEM_HEIGHT - 3, drawX + maxTextWidth,
                    currentY + TASK_ITEM_HEIGHT - 2, borderColor); // 下
        }

        // ステータスアイコンと色
        String icon;
        int textColor;
        if (isEmergency) {
            icon = "[!]";
            textColor = 0xFF5555;
        } else {
            switch (status) {
                case "executing":
                    icon = "[>]";
                    textColor = 0xFFFF55;
                    break;
                case "paused":
                    icon = "[=]";
                    textColor = 0xFFAA55;
                    break;
                default:
                    icon = "[ ]";
                    textColor = 0xAAAAAA;
                    break;
            }
        }

        // タスク名（短縮）
        String shortGoal = goal.length() > 25 ? goal.substring(0, 22) + "..." : goal;
        String taskText = icon + " " + shortGoal;
        context.drawTextWithShadow(mc.textRenderer, Text.literal(taskText), drawX, currentY, textColor);

        // ボタン領域（右端）
        int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

        // 実行ボタン（▶）
        boolean playHovered = isHovered && "play".equals(hoveredButton);
        int playBgColor = playHovered ? 0xFF55CC55 : 0xFF444444;
        context.fill(buttonX, currentY, buttonX + BUTTON_SIZE, currentY + BUTTON_SIZE, playBgColor);
        if (playHovered) {
            // ホバー時の枠線
            context.fill(buttonX, currentY, buttonX + BUTTON_SIZE, currentY + 1, 0xFF88FF88);
        }
        context.drawTextWithShadow(mc.textRenderer, Text.literal(">"), buttonX + 4, currentY + 3,
                playHovered ? 0xFFFFFF : 0x55FF55);

        // 削除ボタン（✕）
        boolean deleteHovered = isHovered && "delete".equals(hoveredButton);
        int deleteBgColor = deleteHovered ? 0xFFCC5555 : 0xFF444444;
        context.fill(buttonX + BUTTON_SIZE + 4, currentY, buttonX + BUTTON_SIZE * 2 + 4, currentY + BUTTON_SIZE,
                deleteBgColor);
        if (deleteHovered) {
            // ホバー時の枠線
            context.fill(buttonX + BUTTON_SIZE + 4, currentY, buttonX + BUTTON_SIZE * 2 + 4, currentY + 1, 0xFFFF8888);
        }
        context.drawTextWithShadow(mc.textRenderer, Text.literal("x"), buttonX + BUTTON_SIZE + 8, currentY + 3,
                deleteHovered ? 0xFFFFFF : 0xFF5555);

        return currentY + TASK_ITEM_HEIGHT + 2;
    }

    /**
     * タスク詳細をレンダリング
     */
    private static int renderTaskDetails(DrawContext context, MinecraftClient mc,
            TaskTreeState taskTreeState, int drawX, int currentY, int maxTextWidth, int scaledUiHeight) {

        // ゴール
        String goalLine = "Goal: " + taskTreeState.goal;
        int goalColor = getTaskStatusColor(taskTreeState.status);
        for (OrderedText lineText : wrapText(mc, goalLine, maxTextWidth)) {
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, goalColor);
            }
            currentY += LINE_HEIGHT;
        }

        // ストラテジー
        if (taskTreeState.strategy != null && !taskTreeState.strategy.isEmpty()) {
            String strategyLine = "Strategy: " + taskTreeState.strategy;
            for (OrderedText lineText : wrapText(mc, strategyLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xAAAAAA);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // エラー表示
        if (taskTreeState.error != null && !taskTreeState.error.isEmpty()) {
            currentY += LINE_HEIGHT / 2;
            String errorLine = "[ERROR] " + taskTreeState.error;
            for (OrderedText lineText : wrapText(mc, errorLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xFF5555);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 階層的サブタスク
        if (taskTreeState.hierarchicalSubTasks != null && !taskTreeState.hierarchicalSubTasks.isEmpty()) {
            currentY += LINE_HEIGHT / 2;
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal("SubTasks:"), drawX, currentY, 0x55AAFF);
            }
            currentY += LINE_HEIGHT;

            for (TaskTreeState.HierarchicalSubTask sub : taskTreeState.hierarchicalSubTasks) {
                currentY = renderHierarchicalSubTask(context, mc, sub, drawX, currentY, maxTextWidth, scaledUiHeight,
                        0);
            }
        }

        return currentY;
    }

    /**
     * 階層的サブタスクを再帰的にレンダリング
     */
    private static int renderHierarchicalSubTask(DrawContext context, MinecraftClient mc,
            TaskTreeState.HierarchicalSubTask sub, int drawX, int currentY,
            int maxTextWidth, int scaledUiHeight, int depth) {

        String indent = "  ".repeat(depth);
        String icon = sub.getStatusIcon();
        int color = sub.getStatusColor();

        String subTaskLine = indent + icon + " " + sub.goal;
        for (OrderedText lineText : wrapText(mc, subTaskLine, maxTextWidth)) {
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, color);
            }
            currentY += LINE_HEIGHT;
        }

        // 結果（完了の場合）
        if (sub.result != null && !sub.result.isEmpty() && "completed".equals(sub.status)) {
            String shortResult = sub.result.length() > 40 ? sub.result.substring(0, 40) + "..." : sub.result;
            String resultLine = indent + "  => " + shortResult;
            for (OrderedText lineText : wrapText(mc, resultLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0x88FF88);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 失敗理由（エラーの場合）
        if (sub.failureReason != null && !sub.failureReason.isEmpty()) {
            String failureLine = indent + "  [x] " + sub.failureReason;
            for (OrderedText lineText : wrapText(mc, failureLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xFF5555);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 子タスクを再帰的にレンダリング
        if (sub.children != null && !sub.children.isEmpty()) {
            for (TaskTreeState.HierarchicalSubTask child : sub.children) {
                currentY = renderHierarchicalSubTask(context, mc, child, drawX, currentY, maxTextWidth, scaledUiHeight,
                        depth + 1);
            }
        }

        return currentY;
    }

    /**
     * マウスクリック処理
     */
    public static boolean handleClick(int uiX, int uiY, int uiWidth, int uiHeight, double mouseX, double mouseY) {
        // スケール変換
        double scaledX = (mouseX - uiX) / SCALE;
        double scaledY = (mouseY - uiY) / SCALE;

        TaskListStatePacket.TaskListState taskListState = ShannonUIModClient.getTaskListState();
        if (taskListState == null)
            return false;

        int drawX = (int) (4 / SCALE);
        int currentY = (int) (4 / SCALE) + LINE_HEIGHT + 4; // ヘッダー分
        int maxTextWidth = (int) ((uiWidth - 8) / SCALE);

        // 通常タスク
        if (taskListState.tasks != null) {
            for (TaskListStatePacket.TaskListState.TaskInfo task : taskListState.tasks) {
                if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                    // ボタン領域チェック
                    int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

                    if (scaledX >= buttonX && scaledX < buttonX + BUTTON_SIZE) {
                        // 実行ボタン
                        sendTaskAction(TaskActionPacket.ACTION_PRIORITIZE, task.id);
                        return true;
                    } else if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                        // 削除ボタン
                        sendTaskAction(TaskActionPacket.ACTION_DELETE, task.id);
                        return true;
                    } else {
                        // タスク選択
                        ShannonUIModClient.setSelectedTaskId(task.id);
                        return true;
                    }
                }
                currentY += TASK_ITEM_HEIGHT + 2;
            }
        }

        // 緊急タスク
        if (taskListState.emergencyTask != null) {
            if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

                if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                    // 削除ボタン
                    sendTaskAction(TaskActionPacket.ACTION_DELETE, taskListState.emergencyTask.id);
                    return true;
                } else {
                    // タスク選択
                    ShannonUIModClient.setSelectedTaskId(taskListState.emergencyTask.id);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * マウスホバー処理
     */
    public static void handleHover(int uiX, int uiY, int uiWidth, int uiHeight, double mouseX, double mouseY) {
        double scaledX = (mouseX - uiX) / SCALE;
        double scaledY = (mouseY - uiY) / SCALE;

        TaskListStatePacket.TaskListState taskListState = ShannonUIModClient.getTaskListState();
        if (taskListState == null) {
            hoveredTaskId = null;
            hoveredButton = null;
            return;
        }

        int drawX = (int) (4 / SCALE);
        int currentY = (int) (4 / SCALE) + LINE_HEIGHT + 4;
        int maxTextWidth = (int) ((uiWidth - 8) / SCALE);

        hoveredTaskId = null;
        hoveredButton = null;

        // 通常タスク
        if (taskListState.tasks != null) {
            for (TaskListStatePacket.TaskListState.TaskInfo task : taskListState.tasks) {
                if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                    hoveredTaskId = task.id;
                    int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

                    if (scaledX >= buttonX && scaledX < buttonX + BUTTON_SIZE) {
                        hoveredButton = "play";
                    } else if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                        hoveredButton = "delete";
                    }
                    return;
                }
                currentY += TASK_ITEM_HEIGHT + 2;
            }
        }

        // 緊急タスク
        if (taskListState.emergencyTask != null) {
            if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                hoveredTaskId = taskListState.emergencyTask.id;
                int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

                if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                    hoveredButton = "delete";
                }
            }
        }
    }

    /**
     * タスクアクションを送信
     */
    private static void sendTaskAction(String action, String taskId) {
        ClientPlayNetworking.send(new TaskActionPacket(action, taskId));
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

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}
