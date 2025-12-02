package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.TaskTreeState;
import net.minecraft.text.Style;

public class TaskTreeUIRenderer {
    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset, UIRenderer.UIState state) {
        context.getMatrices().push();
        try {
            if (taskTreeState == null) {
                state.contentHeight = uiHeight;
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

            // ゴール（太字）
            for (OrderedText lineText : wrapText(mc, taskTreeState.goal, maxTextWidth)) {
                int textY = startY + 10 * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, statusColor);
                }
                line++;
            }
            // ストラテジー
            for (OrderedText lineText : wrapText(mc, taskTreeState.strategy, maxTextWidth)) {
                int textY = startY + 10 * line;
                if (textY >= 0 && textY + 10 <= uiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, statusColor);
                }
                line++;
            }
            // エラー
            if (taskTreeState.error != null && !taskTreeState.error.isEmpty()) {
                for (OrderedText lineText : wrapText(mc, "Error: " + taskTreeState.error, maxTextWidth)) {
                    int textY = startY + 10 * line;
                    if (textY >= 0 && textY + 10 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF8888);
                    }
                    line++;
                }
            }
            line++;
            // サブタスク
            if (taskTreeState.subTasks != null && !taskTreeState.subTasks.isEmpty()) {
                for (TaskTreeState.SubTask sub : taskTreeState.subTasks) {
                    int subColor = 0xAAAAAA;
                    String subStatus = sub.subTaskStatus == null ? "" : sub.subTaskStatus.toLowerCase();
                    switch (subStatus) {
                        case "pending":
                            subColor = 0xAAAAAA;
                            break;
                        case "in_progress":
                            subColor = 0xFFFF00;
                            break;
                        case "completed":
                            subColor = 0x00FF00;
                            break;
                        case "error":
                            subColor = 0xFF0000;
                            break;
                    }
                    Text boldSubText = Text.literal("  > " + sub.subTaskGoal).copy()
                            .setStyle(Style.EMPTY.withBold(true));
                    for (OrderedText lineText : wrapText(mc, boldSubText.getString(), maxTextWidth)) {
                        int textY = startY + 10 * line;
                        if (textY >= 0 && textY + 10 <= uiHeight) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, subColor);
                        }
                        line++;
                    }
                    for (OrderedText lineText : wrapText(mc, "    " + sub.subTaskStrategy, maxTextWidth)) {
                        int textY = startY + 10 * line;
                        if (textY >= 0 && textY + 10 <= uiHeight) {
                            context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, subColor);
                        }
                        line++;
                    }
                    if (sub.subTaskResult != null && !sub.subTaskResult.isEmpty()) {
                        for (OrderedText lineText : wrapText(mc, "  => " + sub.subTaskResult, maxTextWidth)) {
                            int textY = startY + 10 * line;
                            if (textY >= 0 && textY + 10 <= uiHeight) {
                                context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x8888FF);
                            }
                            line++;
                        }
                    }
                    line++;
                }
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

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}