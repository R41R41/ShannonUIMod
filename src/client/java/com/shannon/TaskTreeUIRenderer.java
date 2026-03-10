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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * タスク状態UIレンダラー
 * タスクリスト（最大3つ + 緊急1つ）と選択されたタスクの詳細を表示
 * サブタスクの折りたたみ機能付き
 */
public class TaskTreeUIRenderer {
    private static final float SCALE = RenderUtils.SCALE;
    private static final int LINE_HEIGHT = 9;
    private static final int TASK_ITEM_HEIGHT = 20;
    private static final int BUTTON_SIZE = 14;

    // ホバー状態
    private static String hoveredTaskId = null;
    private static String hoveredButton = null; // "play" or "delete"

    // サブタスク折りたたみ状態
    private static final Set<String> collapsedSubTasks = new HashSet<>();

    // サブタスクのクリック判定に使うスクロールオフセット
    private static int lastScrollOffset = 0;

    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state,
            DetailedLogsState logsState, com.shannon.network.packet.LogToggleState logToggleState,
            int relMouseX, int relMouseY, boolean mouseJustClicked) {

        lastScrollOffset = scrollOffset;
        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = (int) ((uiWidth - 8) / SCALE);
            int startY = drawY + yOffset;
            int scaledUiHeight = (int) (uiHeight / SCALE);
            int currentY = startY;

            // マウス座標をスケーリング
            int scaledMouseX = (int) ((relMouseX + 4) / SCALE);
            int scaledMouseY = (int) ((relMouseY + 4) / SCALE);
            int scrollComp = (int) (scrollOffset / SCALE);
            int clickMouseY = scaledMouseY - scrollComp;

            // クライアントからタスクリストを取得
            TaskListStatePacket.TaskListState taskListState = ShannonUIModClient.getTaskListState();
            String selectedTaskId = ShannonUIModClient.getSelectedTaskId();

            // === タスクリストセクション ===
            String header = "== タスク一覧 ==";
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal(header), drawX, currentY,
                        RenderUtils.COLOR_HEADER);
            }
            currentY += LINE_HEIGHT + 4;

            // タスクがない場合
            if (taskListState == null || (taskListState.tasks == null || taskListState.tasks.isEmpty())
                    && taskListState.emergencyTask == null) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("タスクなし"), drawX, currentY,
                            RenderUtils.COLOR_MUTED);
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
            String detailHeader = "== タスク詳細 ==";
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal(detailHeader), drawX, currentY,
                        RenderUtils.COLOR_HEADER);
            }
            currentY += LINE_HEIGHT + 4;

            // 選択されたタスクがあれば詳細を表示
            if (taskTreeState != null && taskTreeState.goal != null && !taskTreeState.goal.isEmpty()) {
                currentY = renderTaskDetails(context, mc, taskTreeState, drawX, currentY, maxTextWidth, scaledUiHeight,
                        scaledMouseX, clickMouseY, mouseJustClicked);
            } else {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("タスクを選択して詳細を表示"), drawX,
                            currentY, RenderUtils.COLOR_MUTED);
                }
                currentY += LINE_HEIGHT;
            }

            state.contentHeight = (int) ((currentY - startY + 20) * SCALE);
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }
        } finally {
            context.disableScissor();
            context.getMatrices().popMatrix();
        }
    }

    // === 旧シグネチャの互換ラッパー ===
    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state,
            DetailedLogsState logsState, com.shannon.network.packet.LogToggleState logToggleState) {
        renderTaskTreeUI(context, mc, x, y, uiWidth, uiHeight, taskTreeState, scrollOffset, state,
                logsState, logToggleState, 0, 0, false);
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

        boolean isHovered = hoveredTaskId != null && hoveredTaskId.equals(taskId);
        int bgColor;
        if (isSelected) {
            bgColor = isHovered ? 0x55AACCFF : 0x4488AAFF;
        } else if (isHovered) {
            bgColor = 0x44FFFFFF;
        } else {
            bgColor = 0x22FFFFFF;
        }
        context.fill(drawX - 2, currentY - 2, drawX + maxTextWidth, currentY + TASK_ITEM_HEIGHT - 2, bgColor);

        if (isHovered) {
            int borderColor = 0x88FFFFFF;
            context.fill(drawX - 2, currentY - 2, drawX + maxTextWidth, currentY - 1, borderColor);
            context.fill(drawX - 2, currentY + TASK_ITEM_HEIGHT - 3, drawX + maxTextWidth,
                    currentY + TASK_ITEM_HEIGHT - 2, borderColor);
        }

        String icon;
        int textColor;
        if (isEmergency) {
            icon = "[!]";
            textColor = RenderUtils.COLOR_ERROR;
        } else {
            switch (status) {
                case "executing":
                    icon = "[>]";
                    textColor = RenderUtils.COLOR_IN_PROGRESS;
                    break;
                case "paused":
                    icon = "[=]";
                    textColor = RenderUtils.COLOR_WARNING;
                    break;
                default:
                    icon = "[ ]";
                    textColor = RenderUtils.COLOR_SUBTEXT;
                    break;
            }
        }

        String shortGoal = goal.length() > 25 ? goal.substring(0, 22) + "..." : goal;
        String taskText = icon + " " + shortGoal;
        context.drawTextWithShadow(mc.textRenderer, Text.literal(taskText), drawX, currentY, textColor);

        int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;

        boolean playHovered = isHovered && "play".equals(hoveredButton);
        int playBgColor = playHovered ? 0xFF55CC55 : 0xFF444444;
        context.fill(buttonX, currentY, buttonX + BUTTON_SIZE, currentY + BUTTON_SIZE, playBgColor);
        if (playHovered) {
            context.fill(buttonX, currentY, buttonX + BUTTON_SIZE, currentY + 1, 0xFF88FF88);
        }
        context.drawTextWithShadow(mc.textRenderer, Text.literal(">"), buttonX + 4, currentY + 3,
                playHovered ? 0xFFFFFF : RenderUtils.COLOR_SUCCESS);

        boolean deleteHovered = isHovered && "delete".equals(hoveredButton);
        int deleteBgColor = deleteHovered ? 0xFFCC5555 : 0xFF444444;
        context.fill(buttonX + BUTTON_SIZE + 4, currentY, buttonX + BUTTON_SIZE * 2 + 4, currentY + BUTTON_SIZE,
                deleteBgColor);
        if (deleteHovered) {
            context.fill(buttonX + BUTTON_SIZE + 4, currentY, buttonX + BUTTON_SIZE * 2 + 4, currentY + 1, 0xFFFF8888);
        }
        context.drawTextWithShadow(mc.textRenderer, Text.literal("x"), buttonX + BUTTON_SIZE + 8, currentY + 3,
                deleteHovered ? 0xFFFFFF : RenderUtils.COLOR_ERROR);

        return currentY + TASK_ITEM_HEIGHT + 2;
    }

    /**
     * タスク詳細をレンダリング
     */
    private static int renderTaskDetails(DrawContext context, MinecraftClient mc,
            TaskTreeState taskTreeState, int drawX, int currentY, int maxTextWidth, int scaledUiHeight,
            int mouseX, int mouseY, boolean mouseJustClicked) {

        // ゴール
        String goalLine = "目標:";
        int goalColor = getTaskStatusColor(taskTreeState.status);
        for (OrderedText lineText : RenderUtils.wrapText(mc, goalLine, maxTextWidth)) {
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, goalColor);
            }
            currentY += LINE_HEIGHT;
        }
        for (OrderedText lineText : RenderUtils.wrapText(mc, "  " + taskTreeState.goal, maxTextWidth)) {
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, goalColor);
            }
            currentY += LINE_HEIGHT;
        }

        // ストラテジー
        if (taskTreeState.strategy != null && !taskTreeState.strategy.isEmpty()) {
            String strategyLabel = "戦略:";
            for (OrderedText lineText : RenderUtils.wrapText(mc, strategyLabel, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, RenderUtils.COLOR_SUBTEXT);
                }
                currentY += LINE_HEIGHT;
            }
            for (OrderedText lineText : RenderUtils.wrapText(mc, "  " + taskTreeState.strategy, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, RenderUtils.COLOR_SUBTEXT);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 思考表示
        if (taskTreeState.currentThinking != null && !taskTreeState.currentThinking.isEmpty()) {
            currentY += LINE_HEIGHT / 2;
            String thinkingLabel = "💭 思考:";
            for (OrderedText lineText : RenderUtils.wrapText(mc, thinkingLabel, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xFF88CCFF);
                }
                currentY += LINE_HEIGHT;
            }
            for (OrderedText lineText : RenderUtils.wrapText(mc, "  " + taskTreeState.currentThinking, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xFFAADDFF);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // エラー表示
        if (taskTreeState.error != null && !taskTreeState.error.isEmpty()) {
            currentY += LINE_HEIGHT / 2;
            String errorLine = "[エラー] " + taskTreeState.error;
            for (OrderedText lineText : RenderUtils.wrapText(mc, errorLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, RenderUtils.COLOR_ERROR);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 階層的サブタスク（折りたたみ対応）
        if (taskTreeState.hierarchicalSubTasks != null && !taskTreeState.hierarchicalSubTasks.isEmpty()) {
            currentY += LINE_HEIGHT / 2;
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal("サブタスク:"), drawX, currentY,
                        RenderUtils.COLOR_HEADER);
            }
            currentY += LINE_HEIGHT;

            for (TaskTreeState.HierarchicalSubTask sub : taskTreeState.hierarchicalSubTasks) {
                currentY = renderHierarchicalSubTask(context, mc, sub, drawX, currentY, maxTextWidth, scaledUiHeight,
                        0, mouseX, mouseY, mouseJustClicked);
            }
        }

        return currentY;
    }

    /**
     * 階層的サブタスクを再帰的にレンダリング（折りたたみ対応）
     */
    private static int renderHierarchicalSubTask(DrawContext context, MinecraftClient mc,
            TaskTreeState.HierarchicalSubTask sub, int drawX, int currentY,
            int maxTextWidth, int scaledUiHeight, int depth,
            int mouseX, int mouseY, boolean mouseJustClicked) {

        String indent = "  ".repeat(depth);
        String icon = sub.getStatusIcon();
        int color = sub.getStatusColor();
        boolean hasChildren = sub.children != null && !sub.children.isEmpty();
        String subId = sub.id != null ? sub.id : sub.goal;
        boolean isCollapsed = hasChildren && collapsedSubTasks.contains(subId);

        // 折りたたみアイコン
        String collapseIcon = hasChildren ? (isCollapsed ? "> " : "v ") : "  ";
        String subTaskLine = indent + collapseIcon + icon + " " + sub.goal;

        // クリック判定用のY位置を記録
        int lineStartY = currentY;

        for (OrderedText lineText : RenderUtils.wrapText(mc, subTaskLine, maxTextWidth)) {
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, color);
            }
            currentY += LINE_HEIGHT;
        }

        // クリック判定（折りたたみトグル）
        if (hasChildren && mouseJustClicked) {
            if (mouseX >= drawX && mouseX <= drawX + maxTextWidth
                    && mouseY >= lineStartY && mouseY < currentY) {
                if (isCollapsed) {
                    collapsedSubTasks.remove(subId);
                } else {
                    collapsedSubTasks.add(subId);
                }
            }
        }

        // 結果（完了の場合）
        if (sub.result != null && !sub.result.isEmpty() && "completed".equals(sub.status)) {
            String shortResult = sub.result.length() > 40 ? sub.result.substring(0, 40) + "..." : sub.result;
            String resultLine = indent + "    => " + shortResult;
            for (OrderedText lineText : RenderUtils.wrapText(mc, resultLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, 0xFF88FF88);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 失敗理由（エラーの場合）
        if (sub.failureReason != null && !sub.failureReason.isEmpty()) {
            String failureLine = indent + "    [x] 結果: 失敗 詳細:";
            for (OrderedText lineText : RenderUtils.wrapText(mc, failureLine, maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, RenderUtils.COLOR_ERROR);
                }
                currentY += LINE_HEIGHT;
            }
            for (OrderedText lineText : RenderUtils.wrapText(mc, indent + "      " + sub.failureReason,
                    maxTextWidth)) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, currentY, RenderUtils.COLOR_ERROR);
                }
                currentY += LINE_HEIGHT;
            }
        }

        // 子タスクを再帰的にレンダリング（折りたたみ時はスキップ）
        if (hasChildren && !isCollapsed) {
            for (TaskTreeState.HierarchicalSubTask child : sub.children) {
                currentY = renderHierarchicalSubTask(context, mc, child, drawX, currentY, maxTextWidth, scaledUiHeight,
                        depth + 1, mouseX, mouseY, mouseJustClicked);
            }
        } else if (hasChildren && isCollapsed) {
            // 折りたたみ時: 子タスク数を表示
            String countText = indent + "    (" + sub.children.size() + "個のサブタスクが非表示)";
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal(countText), drawX, currentY,
                        RenderUtils.COLOR_MUTED);
            }
            currentY += LINE_HEIGHT;
        }

        return currentY;
    }

    // === クリック・ホバーハンドリング（タスクリスト部分） ===

    public static boolean handleClick(int uiX, int uiY, int uiWidth, int uiHeight, double mouseX, double mouseY) {
        double scaledX = (mouseX - uiX) / SCALE;
        double scaledY = (mouseY - uiY) / SCALE;

        TaskListStatePacket.TaskListState taskListState = ShannonUIModClient.getTaskListState();
        if (taskListState == null)
            return false;

        int drawX = (int) (4 / SCALE);
        int currentY = (int) (4 / SCALE) + LINE_HEIGHT + 4;
        int maxTextWidth = (int) ((uiWidth - 8) / SCALE);

        if (taskListState.tasks != null) {
            for (TaskListStatePacket.TaskListState.TaskInfo task : taskListState.tasks) {
                if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                    int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;
                    if (scaledX >= buttonX && scaledX < buttonX + BUTTON_SIZE) {
                        sendTaskAction(TaskActionPacket.ACTION_PRIORITIZE, task.id);
                        return true;
                    } else if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                        sendTaskAction(TaskActionPacket.ACTION_DELETE, task.id);
                        return true;
                    } else {
                        ShannonUIModClient.setSelectedTaskId(task.id);
                        return true;
                    }
                }
                currentY += TASK_ITEM_HEIGHT + 2;
            }
        }

        if (taskListState.emergencyTask != null) {
            if (scaledY >= currentY && scaledY < currentY + TASK_ITEM_HEIGHT) {
                int buttonX = drawX + maxTextWidth - BUTTON_SIZE * 2 - 8;
                if (scaledX >= buttonX + BUTTON_SIZE + 4 && scaledX < buttonX + BUTTON_SIZE * 2 + 4) {
                    sendTaskAction(TaskActionPacket.ACTION_DELETE, taskListState.emergencyTask.id);
                    return true;
                } else {
                    ShannonUIModClient.setSelectedTaskId(taskListState.emergencyTask.id);
                    return true;
                }
            }
        }

        return false;
    }

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

    private static void sendTaskAction(String action, String taskId) {
        ClientPlayNetworking.send(new TaskActionPacket(action, taskId));
    }

    private static int getTaskStatusColor(String status) {
        if (status == null)
            return RenderUtils.COLOR_SUBTEXT;
        switch (status.toLowerCase()) {
            case "completed":
                return RenderUtils.COLOR_SUCCESS;
            case "in_progress":
                return RenderUtils.COLOR_IN_PROGRESS;
            case "error":
                return RenderUtils.COLOR_ERROR;
            default:
                return RenderUtils.COLOR_SUBTEXT;
        }
    }
}
