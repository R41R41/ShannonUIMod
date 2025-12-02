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
    private static String currentInput = "";
    private static boolean wasEnterPressed = false;
    private static int cursorBlinkTimer = 0;

    public static void renderChat(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, int scrollOffset, ChatState chatState, int mouseX,
            int mouseY, boolean mouseClicked) {
        // 入力欄の高さ
        int inputAreaHeight = 24;
        int chatHistoryHeight = uiHeight - inputAreaHeight - 8;

        // チャット履歴の描画
        context.getMatrices().push();
        try {
            int line = 0;
            float scale = 1.0f;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -scrollOffset;
            int maxTextWidth = uiWidth - 8;
            int startY = drawY + yOffset;

            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);

            // チャット履歴の表示
            if (chatState != null && chatState.messages != null && !chatState.messages.isEmpty()) {
                for (ChatState.ChatMessage msg : chatState.messages) {
                    String fullText = "[" + msg.sender + "] " + msg.message;
                    for (OrderedText wrapped : wrapText(mc, fullText, maxTextWidth)) {
                        int textY = startY + line * 12;
                        if (textY >= 0 && textY + 12 <= chatHistoryHeight) {
                            context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFFFFFF);
                        }
                        line++;
                    }
                }
            } else {
                // チャット履歴がない場合
                String noMessages = "チャット履歴がありません";
                for (OrderedText wrapped : wrapText(mc, noMessages, maxTextWidth)) {
                    int textY = startY + line * 12;
                    if (textY >= 0 && textY + 12 <= chatHistoryHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xAAAAAA);
                    }
                    line++;
                }
            }

            // チャット履歴の高さを計算（入力欄を除く）
            state.contentHeight = (line + 1) * 12 + 8;

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= chatHistoryHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

        } finally {
            context.getMatrices().pop();
        }

        // 入力欄の描画（スクロールの影響を受けない固定位置）- 常に表示
        context.getMatrices().push();
        try {
            int drawX = 4;

            context.getMatrices().translate(x, y, 0);

            // 入力欄をUIの一番下に固定
            int inputY = chatHistoryHeight + 8;
            int inputBoxX = drawX;
            int inputBoxY = inputY;
            int inputBoxWidth = uiWidth - 16;
            int inputBoxHeight = 16;

            // 区切り線
            context.fill(inputBoxX, inputBoxY - 4, inputBoxX + inputBoxWidth, inputBoxY - 3, 0xFF4d4d4d);

            // 入力欄の背景
            context.fill(inputBoxX, inputBoxY - 2, inputBoxX + inputBoxWidth,
                    inputBoxY + inputBoxHeight - 2, 0xFF000000);
            context.fill(inputBoxX, inputBoxY - 2, inputBoxX + inputBoxWidth,
                    inputBoxY + inputBoxHeight - 1, 0x80FFFFFF);

            // 入力テキストの描画
            String displayText = currentInput;
            if (displayText.isEmpty()) {
                displayText = "メッセージを入力...";
                context.drawText(mc.textRenderer, displayText, inputBoxX, inputBoxY, 0x808080, false);
            } else {
                context.drawText(mc.textRenderer, displayText, inputBoxX, inputBoxY, 0xFFFFFF, false);

                // カーソルの描画（点滅）
                cursorBlinkTimer++;
                if (cursorBlinkTimer % 20 < 10) {
                    int cursorX = inputBoxX + mc.textRenderer.getWidth(displayText);
                    context.fill(cursorX, inputBoxY, cursorX + 1, inputBoxY + 10, 0xFFFFFFFF);
                }
            }

        } finally {
            context.getMatrices().pop();
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
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }

    public static String getCurrentInput() {
        return currentInput;
    }

    public static void setCurrentInput(String input) {
        currentInput = input;
    }
}
