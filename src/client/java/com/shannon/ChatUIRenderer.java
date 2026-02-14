package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.ChatState;
import com.shannon.network.packet.ChatMessageSendPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

public class ChatUIRenderer {
    private static final float SCALE = 0.7f;
    private static final int LINE_HEIGHT = 10;
    private static String currentInput = "";
    private static boolean wasEnterPressed = false;
    private static int cursorBlinkTimer = 0;
    private static boolean isInputFocused = false;
    private static boolean wasMousePressed = false;

    public static void renderChat(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, int scrollOffset, ChatState chatState, int mouseX,
            int mouseY, boolean mouseClicked) {
        // 入力欄の高さ（スケール適用後の実際の高さ）
        int inputAreaHeight = 14;
        int chatHistoryHeight = uiHeight - inputAreaHeight - 4;

        // チャット履歴の描画
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = (int) ((uiWidth - 8) / SCALE);
            int startY = drawY + yOffset;
            int scaledChatHistoryHeight = (int) (chatHistoryHeight / SCALE);

            // チャット履歴の表示
            if (chatState != null && chatState.messages != null && !chatState.messages.isEmpty()) {
                for (ChatState.ChatMessage msg : chatState.messages) {
                    // 送信者によって色を変える
                    int color = msg.sender.equals("Shannon") ? 0x55FF55 : 0xFFFFFF;
                    String fullText = "[" + msg.sender + "] " + msg.message;
                    for (OrderedText wrapped : wrapText(mc, fullText, maxTextWidth)) {
                        int textY = startY + line * LINE_HEIGHT;
                        if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                            context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, color);
                        }
                        line++;
                    }
                }
            } else {
                // チャット履歴がない場合
                String noMessages = "メッセージなし";
                for (OrderedText wrapped : wrapText(mc, noMessages, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0x666666);
                    }
                    line++;
                }
            }

            // チャット履歴の高さを計算（入力欄を除く）
            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= chatHistoryHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

        } finally {
            context.getMatrices().popMatrix();
        }

        // 入力欄の描画（スケール適用、スクロールの影響を受けない固定位置）
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledChatHistoryHeight = (int) (chatHistoryHeight / SCALE);

            // 入力欄をUIの一番下に固定
            int inputBoxX = (int) (4 / SCALE);
            int inputBoxY = scaledChatHistoryHeight + 4;
            int inputBoxWidth = scaledUiWidth - 8;
            int inputBoxHeight = 12;

            // 区切り線
            context.fill(inputBoxX, inputBoxY - 2, inputBoxX + inputBoxWidth, inputBoxY - 1, 0xFF4d4d4d);

            // 入力欄の背景（フォーカス時は少し明るく）
            int bgColor = isInputFocused ? 0xFF1a1a1a : 0xFF000000;
            int borderColor = isInputFocused ? 0xFF5599FF : 0xFF444444;
            context.fill(inputBoxX, inputBoxY, inputBoxX + inputBoxWidth, inputBoxY + inputBoxHeight, bgColor);
            // 枠線
            context.fill(inputBoxX, inputBoxY, inputBoxX + inputBoxWidth, inputBoxY + 1, borderColor);
            context.fill(inputBoxX, inputBoxY + inputBoxHeight - 1, inputBoxX + inputBoxWidth,
                    inputBoxY + inputBoxHeight, 0xFF222222);
            context.fill(inputBoxX, inputBoxY, inputBoxX + 1, inputBoxY + inputBoxHeight, borderColor);
            context.fill(inputBoxX + inputBoxWidth - 1, inputBoxY, inputBoxX + inputBoxWidth,
                    inputBoxY + inputBoxHeight, 0xFF222222);

            // クリック判定（スケール後の座標系）
            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            boolean overInput = scaledMouseX >= inputBoxX && scaledMouseX <= inputBoxX + inputBoxWidth
                    && scaledMouseY >= inputBoxY && scaledMouseY <= inputBoxY + inputBoxHeight;

            if (mouseClicked && !wasMousePressed && overInput) {
                isInputFocused = true;
            }

            // 入力テキストの描画
            int textX = inputBoxX + 2;
            int textY = inputBoxY + 2;
            String displayText = currentInput;
            if (displayText.isEmpty()) {
                String placeholder = isInputFocused ? "" : "メッセージを入力...";
                context.drawText(mc.textRenderer, placeholder, textX, textY, 0x666666, false);
            } else {
                context.drawText(mc.textRenderer, displayText, textX, textY, 0xFFFFFF, false);
            }

            // カーソルの描画（フォーカス時のみ、点滅）
            if (isInputFocused) {
                cursorBlinkTimer++;
                if (cursorBlinkTimer % 20 < 10) {
                    int cursorX = textX + mc.textRenderer.getWidth(displayText);
                    context.fill(cursorX, textY, cursorX + 1, textY + 8, 0xFFFFFFFF);
                }
            }

            wasMousePressed = mouseClicked;

        } finally {
            context.getMatrices().popMatrix();
        }
    }

    public static boolean isInputFocused() {
        return isInputFocused;
    }

    public static void setInputFocused(boolean focused) {
        isInputFocused = focused;
        if (focused) {
            cursorBlinkTimer = 0;
        }
    }

    public static void handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!wasEnterPressed && !currentInput.isEmpty()) {
                // メッセージを送信
                ClientPlayNetworking.send(new ChatMessageSendPacket(currentInput));
                currentInput = "";
                cursorBlinkTimer = 0;
            }
            wasEnterPressed = true;
        } else {
            wasEnterPressed = false;

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                cursorBlinkTimer = 0;
            }
        }
    }

    public static void handleCharTyped(char chr, int modifiers) {
        // 制御文字を除外
        if (chr >= 32 && chr != 127) {
            currentInput += chr;
            cursorBlinkTimer = 0;
        }
    }

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        return RenderUtils.wrapText(mc, text, maxWidth);
    }

    public static String getCurrentInput() {
        return currentInput;
    }

    public static void setCurrentInput(String input) {
        currentInput = input;
    }
}
