package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.TaskTreeState;
import net.minecraft.text.Style;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;

public class UIRenderer {
    public static int contentHeight = 0;

    public static void renderTaskTreeUI(DrawContext context, MinecraftClient mc, int x, int y, int windowWidth,
            int windowHeight, int uiWidth, int uiHeight, TaskTreeState taskTreeState, int scrollOffset) {
        context.getMatrices().push();
        try {
            if (taskTreeState == null)
                return;
            int line = 0;
            float scale = 1.0f;
            int drawX = 8;
            int drawY = 8;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 16; // 8pxマージン×2
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

            contentHeight = (line + 1) * 10 + 16; // 16px余白
        } finally {
            context.getMatrices().pop();
        }
    }

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }

    public static class UIState {
        public int selectedTab = 0;
        public int scrollOffset = 0;
        public boolean prevTabPressed = false;
        public int lastUiWidth = 0;
        public int lastUiHeight = 0;
        public int lastPanelX = 0;
        public int lastPanelY = 0;
    }

    public static void handleInput(MinecraftClient mc, UIState state, boolean isUIVisible, int lastUiHeight) {
        // Tabキーでタブ切り替え（押した瞬間のみ）
        boolean tabPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_TAB);
        if (isUIVisible && tabPressed && !state.prevTabPressed) {
            state.selectedTab = (state.selectedTab + 1) % 3;
        }
        state.prevTabPressed = tabPressed;
    }

    public static void handleScroll(double yoffset, UIState state, int lastUiHeight) {
        int scrollStep = 20;
        state.scrollOffset -= yoffset * scrollStep;
        int maxOffset = Math.max(0, contentHeight - lastUiHeight);
        if (state.scrollOffset < 0)
            state.scrollOffset = 0;
        if (state.scrollOffset > maxOffset)
            state.scrollOffset = maxOffset;
    }

    public static void renderTabbedUI(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth, int uiHeight,
            UIState state, TaskTreeState taskTreeState, int lastUiHeight) {
        // タブのラベル（translatable対応）
        Text[] tabs = {
                Text.translatable("tab.tasktree"),
                Text.translatable("tab.inventory"),
                Text.translatable("tab.passiveskill")
        };
        int tabHeight = 18;
        int tabWidth = uiWidth / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tabX = x + i * tabWidth;
            int color = (i == state.selectedTab) ? 0xFFAAAAAA : 0xFF444444;
            context.fill(tabX, y, tabX + tabWidth, y + tabHeight, color);
            int textWidth = mc.textRenderer.getWidth(tabs[i]);
            int textX = tabX + (tabWidth - textWidth) / 2;
            int textY = y + (tabHeight - 10) / 2;
            context.drawTextWithShadow(mc.textRenderer, tabs[i], textX, textY, 0xFFFFFF);
        }
        // タブごとの内容描画
        int contentY = y + tabHeight + 2;
        int contentHeight = uiHeight - tabHeight - 2;
        switch (state.selectedTab) {
            case 0: // タスクツリー
                renderTaskTreeUI(context, mc, x, contentY, uiWidth, contentHeight, uiWidth, contentHeight,
                        taskTreeState, state.scrollOffset);
                break;
            case 1: // インベントリ
                // TODO: インベントリ表示（今は空）
                break;
            case 2: // 常時スキル
                // TODO: 常時スキル表示（今は空）
                break;
        }
    }

    public static boolean isMouseOverPanel(MinecraftClient mc, boolean isUIVisible, int lastPanelX, int lastPanelY,
            int lastUiWidth, int lastUiHeight) {
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return isUIVisible && mouseX >= lastPanelX && mouseX <= lastPanelX + lastUiWidth && mouseY >= lastPanelY
                && mouseY <= lastPanelY + lastUiHeight;
    }

    public static GLFWScrollCallbackI handleToggleUIVisible(boolean isUIVisible,
            GLFWScrollCallbackI originalScrollCallback, long windowHandle, UIRenderer.UIState uiState, int lastUiHeight,
            PanelMouseChecker panelChecker) {
        if (isUIVisible) {
            if (originalScrollCallback == null) {
                return GLFW.glfwSetScrollCallback(windowHandle,
                        (handle, xoffset, yoffset) -> {
                            if (panelChecker.isMouseOverPanel()) {
                                UIRenderer.handleScroll(yoffset, uiState, lastUiHeight);
                            } else if (originalScrollCallback != null) {
                                originalScrollCallback.invoke(handle, xoffset, yoffset);
                            }
                        });
            }
        } else {
            if (originalScrollCallback != null) {
                GLFW.glfwSetScrollCallback(windowHandle, originalScrollCallback);
                return null;
            }
        }
        return originalScrollCallback;
    }

    @FunctionalInterface
    public interface PanelMouseChecker {
        boolean isMouseOverPanel();
    }

    public static void updatePanelLayout(MinecraftClient mc, UIState state, int windowWidth, int windowHeight,
            int textureSize) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int uiWidth = windowWidth * 2;
        int uiHeight = screenHeight - windowHeight - 30;
        int x = screenWidth - uiWidth - 10;
        int y = screenHeight - uiHeight - windowHeight - 20;
        state.lastUiWidth = uiWidth;
        state.lastUiHeight = uiHeight;
        state.lastPanelX = x;
        state.lastPanelY = y;
    }
}