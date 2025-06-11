package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import com.shannon.network.packet.TaskTreeState;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.ConstantSkillsState;

// このクラスはScreenからUI部品描画・レイアウト補助として呼び出す用途に整理
// 例: ShannonUIScreen#render から UIRenderer.renderUI(...) を呼ぶ
public class UIRenderer {
    public static int contentHeight = 0;

    private static final Identifier TASK_TREE = Identifier.of("shannonuimod", "textures/tasktree.png");
    private static final Identifier INVENTORY = Identifier.of("shannonuimod", "textures/inventory.png");
    private static final Identifier PASSIVE_SKILL = Identifier.of("shannonuimod", "textures/passive_skill.png");

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
        public int contentHeight = 0;
    }

    public static void handleInput(MinecraftClient mc, UIState state, int lastUiHeight,
            KeyBinding tabSwitchNextKey) {
        int selectedTab = ShannonUIModClient.getSelectedTab();
        if (tabSwitchNextKey != null && tabSwitchNextKey.wasPressed()) {
            ShannonUIModClient.setTabScrollOffset(selectedTab, state.scrollOffset);
            selectedTab = (selectedTab + 1) % 3;
            ShannonUIModClient.setSelectedTab(selectedTab);
            state.scrollOffset = ShannonUIModClient.getTabScrollOffset(selectedTab);
        }
        state.selectedTab = selectedTab;
    }

    public static void handleScroll(double yoffset, UIState state, int lastUiHeight) {
        int scrollStep = 20;
        state.scrollOffset -= yoffset * scrollStep;
        int maxOffset = Math.max(0, state.contentHeight - lastUiHeight);
        if (state.scrollOffset < 0)
            state.scrollOffset = 0;
        if (state.scrollOffset > maxOffset)
            state.scrollOffset = maxOffset;
        ShannonUIModClient.setTabScrollOffset(state.selectedTab, state.scrollOffset);
    }

    public static void renderUI(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth, int uiHeight,
            UIState state, TaskTreeState taskTreeState,
            InventoryState inventoryState,
            ConstantSkillsState constantSkillsState, int lastUiHeight) {
        renderBaseUI(context, state);
        // タブのラベル（translatable対応）
        int tabHeight = 18;
        int tabWidth = 18;
        renderTabbedUI(context, mc, x, y, uiWidth, uiHeight, state, tabWidth, tabHeight);

        int innerY = y + 4;
        int innerX = x + 2;
        // タブごとの内容描画
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        int relMouseX = (int) mouseX - (x + 2 + 4);
        int relMouseY = (int) mouseY - (y + 4 + 4) + state.scrollOffset;
        boolean mouseClicked = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        switch (state.selectedTab) {
            case 0:
                TaskTreeUIRenderer.renderTaskTreeUI(context, mc, innerX, innerY, uiWidth,
                        uiHeight - 2,
                        taskTreeState, state.scrollOffset, state);
                break;
            case 1:
                ConstantSkillsUIRenderer.renderConstantSkills(context, mc, innerX, innerY, uiWidth, uiHeight - 2,
                        state, constantSkillsState, relMouseX, relMouseY, mouseClicked);
                break;
            case 2:
                InventoryUIRenderer.renderInventory(context, mc, innerX, innerY, uiWidth, uiHeight - 2, state,
                        state.scrollOffset, inventoryState, relMouseX, relMouseY, mouseClicked);
                break;
        }
        // スクロールバー描画
        if (state.contentHeight > uiHeight) {
            int barWidth = 4;
            int barX = x + uiWidth - barWidth - 2;
            int barY = y + 4;
            int barHeight = uiHeight - 8;
            float ratio = (float) barHeight / state.contentHeight;
            int handleHeight = Math.max((int) (barHeight * ratio), 16);
            int maxOffset = state.contentHeight - (uiHeight - 2);
            int handleY = barY + (int) ((float) state.scrollOffset / maxOffset * (barHeight - handleHeight));
            int barColor = 0x66000000;
            int handleColor = 0xFFAAAAAA;
            context.fill(barX, barY, barX + barWidth, barY + barHeight, barColor);
            context.fill(barX, handleY, barX + barWidth, handleY + handleHeight, handleColor);
        }
    }

    private static void renderBaseUI(DrawContext context, UIState state) {
        int bgColor = 0x96000000;
        int borderColor0 = 0xbb000000;
        int borderColor1 = 0xff838383;
        int borderColor2 = 0xff4d4d4d;
        int borderColor3 = 0xff000000;
        context.fill(state.lastPanelX + 1, state.lastPanelY + 1, state.lastPanelX + state.lastUiWidth,
                state.lastPanelY + state.lastUiHeight, bgColor);

        // 枠線0
        context.fill(state.lastPanelX, state.lastPanelY, state.lastPanelX + 1,
                state.lastPanelY + state.lastUiHeight, borderColor0);
        context.fill(state.lastPanelX, state.lastPanelY, state.lastPanelX + state.lastUiWidth,
                state.lastPanelY + 1, borderColor0);

        // 枠線1
        context.fill(state.lastPanelX - 2, state.lastPanelY - 2, state.lastPanelX -
                1,
                state.lastPanelY + state.lastUiHeight + 1, borderColor1);
        context.fill(state.lastPanelX - 2, state.lastPanelY - 2, state.lastPanelX +
                state.lastUiWidth + 1,
                state.lastPanelY - 1, borderColor1);
        context.fill(state.lastPanelX - 2, state.lastPanelY + state.lastUiHeight,
                state.lastPanelX + state.lastUiWidth + 1,
                state.lastPanelY + state.lastUiHeight + 1, borderColor1);
        context.fill(state.lastPanelX + state.lastUiWidth, state.lastPanelY - 2,
                state.lastPanelX + state.lastUiWidth + 1,
                state.lastPanelY + state.lastUiHeight + 1, borderColor1);

        // 枠線2
        context.fill(state.lastPanelX - 1, state.lastPanelY - 1, state.lastPanelX,
                state.lastPanelY + state.lastUiHeight, borderColor2);
        context.fill(state.lastPanelX - 1, state.lastPanelY - 1, state.lastPanelX +
                state.lastUiWidth,
                state.lastPanelY, borderColor2);
        context.fill(state.lastPanelX + state.lastUiWidth + 1, state.lastPanelY - 2,
                state.lastPanelX + state.lastUiWidth + 2,
                state.lastPanelY + state.lastUiHeight + 2, borderColor2);
        context.fill(state.lastPanelX - 2, state.lastPanelY + state.lastUiHeight + 1,
                state.lastPanelX + state.lastUiWidth + 2,
                state.lastPanelY + state.lastUiHeight + 2, borderColor2);

        // 枠線3
        context.fill(state.lastPanelX - 3, state.lastPanelY - 3, state.lastPanelX -
                2,
                state.lastPanelY + state.lastUiHeight + 3, borderColor3);
        context.fill(state.lastPanelX - 3, state.lastPanelY - 3, state.lastPanelX +
                state.lastUiWidth + 3,
                state.lastPanelY - 2, borderColor3);
        context.fill(state.lastPanelX + state.lastUiWidth + 2, state.lastPanelY - 3,
                state.lastPanelX + state.lastUiWidth + 3,
                state.lastPanelY + state.lastUiHeight + 3, borderColor3);
        context.fill(state.lastPanelX - 3, state.lastPanelY + state.lastUiHeight + 2,
                state.lastPanelX + state.lastUiWidth + 3,
                state.lastPanelY + state.lastUiHeight + 3, borderColor3);
    }

    public static void renderTabbedUI(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth, int uiHeight,
            UIState state, int tabWidth, int tabHeight) {
        int bgColor1 = 0xff838383;
        int bgColor2 = 0xff4d4d4d;
        int bdColor1 = 0xffaaaaaa;
        int bdColor2 = 0xff333333;
        int borderColor3 = 0xff000000;
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        boolean mouseClicked = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        for (int i = 0; i < 3; i++) {
            int tabX = x - tabWidth - 2;
            int tabY = y + i * (tabHeight + 5) + 2;
            int bgColor = (i == state.selectedTab) ? bgColor1 : bgColor2;
            int borderColor1 = (i == state.selectedTab) ? bdColor1 : bgColor1;
            int borderColor2 = (i == state.selectedTab) ? bgColor2 : bdColor2;
            context.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, bgColor);
            context.fill(tabX - 1, tabY - 1, tabX, tabY + tabHeight + 1, borderColor1);
            context.fill(tabX - 1, tabY - 1, tabX + tabWidth + 1, tabY, borderColor1);
            context.fill(tabX - 1, tabY + tabHeight, tabX + tabWidth, tabY + tabHeight + 1, borderColor2);
            context.fill(tabX - 2, tabY - 2, tabX - 1, tabY + tabHeight + 2, borderColor3);
            context.fill(tabX - 2, tabY - 2, tabX + tabWidth, tabY - 1, borderColor3);
            context.fill(tabX - 2, tabY + tabHeight + 1, tabX + tabWidth, tabY + tabHeight + 2, borderColor3);
            Identifier[] icon = { TASK_TREE, PASSIVE_SKILL, INVENTORY };
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    icon[i],
                    tabX, tabY,
                    0, 0,
                    tabWidth, tabHeight,
                    tabWidth, tabHeight);
            // クリック判定
            if (mouseClicked && mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY
                    && mouseY <= tabY + tabHeight) {
                state.selectedTab = i;
            }
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
            // 毎回コールバックを上書き
            return GLFW.glfwSetScrollCallback(windowHandle,
                    (handle, xoffset, yoffset) -> {
                        if (panelChecker.isMouseOverPanel()) {
                            UIRenderer.handleScroll(yoffset, uiState, lastUiHeight);
                        } else if (originalScrollCallback != null) {
                            originalScrollCallback.invoke(handle, xoffset, yoffset);
                        }
                    });
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