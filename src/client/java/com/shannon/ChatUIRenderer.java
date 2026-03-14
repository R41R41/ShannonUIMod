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

    private static int hoveredMessageIndex = -1;
    private static long copyFeedbackExpiry = 0;
    private static int copiedMessageIndex = -1;

    public static void renderChat(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, int scrollOffset, ChatState chatState, int mouseX,
            int mouseY, boolean mouseClicked) {
        // 入力欄の高さ（スケール適用後の実際の高さ）— テキスト2行分
        int inputAreaHeight = 24;
        int chatHistoryHeight = uiHeight - inputAreaHeight - 4;

        // チャット履歴の描画
        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int rightEdge = scaledUiWidth - 16;
            int maxTextWidth = rightEdge - drawX;
            int startY = drawY + yOffset;
            int scaledChatHistoryHeight = (int) (chatHistoryHeight / SCALE);

            float scaledMouseX = (mouseX + 4) / SCALE;
            float scaledMouseY = (mouseY + 4) / SCALE;
            float viewportMouseY = (mouseY + 4 - scrollOffset) / SCALE;
            boolean mouseInChatArea = viewportMouseY >= 0 && viewportMouseY < scaledChatHistoryHeight
                    && scaledMouseX >= drawX && scaledMouseX <= drawX + maxTextWidth;

            int newHoveredIndex = -1;

            // チャット履歴の表示
            if (chatState != null && chatState.messages != null && !chatState.messages.isEmpty()) {
                int msgIndex = 0;
                for (ChatState.ChatMessage msg : chatState.messages) {
                    boolean isShannon = msg.sender.equals("Shannon");
                    String timeStr = formatChatTime(msg.timestamp);
                    int msgStartLine = line;

                    if (isShannon) {
                        // Shannon発言: 右寄せ + 薄緑背景バブル
                        String msgText = msg.message;
                        java.util.List<OrderedText> lines = wrapText(mc, msgText, maxTextWidth - 4);
                        int bubbleH = lines.size() * LINE_HEIGHT + 4;
                        int textY0 = startY + line * LINE_HEIGHT;
                        // バブル背景
                        if (textY0 >= 0 && textY0 + bubbleH <= scaledChatHistoryHeight) {
                            context.fill(drawX + 2, textY0 - 1,
                                    drawX + maxTextWidth, textY0 + bubbleH - 2, 0x33226622);
                        }
                        // 送信者ラベル（右端）+ 時刻
                        String senderLabel = "[Shannon]" + timeStr + " ";
                        int senderW = mc.textRenderer.getWidth(senderLabel);
                        int senderX = drawX + maxTextWidth - senderW;
                        if (textY0 >= 0 && textY0 + 10 <= scaledChatHistoryHeight) {
                            context.drawTextWithShadow(mc.textRenderer, Text.literal(senderLabel),
                                    senderX, textY0, 0xFF22BB22);
                        }
                        line++;
                        // メッセージ本文（右寄せ）
                        for (OrderedText wrapped : lines) {
                            int textY = startY + line * LINE_HEIGHT;
                            int textW = mc.textRenderer.getWidth(wrapped);
                            int textX = drawX + maxTextWidth - textW;
                            if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                                context.drawTextWithShadow(mc.textRenderer, wrapped, textX, textY, 0xFF55FF55);
                            }
                            line++;
                        }
                    } else {
                        // プレイヤー発言: 左寄せ（時刻付き）
                        String fullText = "[" + msg.sender + "]" + timeStr + " " + msg.message;
                        for (OrderedText wrapped : wrapText(mc, fullText, maxTextWidth)) {
                            int textY = startY + line * LINE_HEIGHT;
                            if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                                context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFFFFFFFF);
                            }
                            line++;
                        }
                    }

                    // ホバー判定（viewportMouseY を使用: scrollOffset 補正済み）
                    if (mouseInChatArea) {
                        float msgTopY = startY + msgStartLine * LINE_HEIGHT;
                        float msgBottomY = startY + line * LINE_HEIGHT;
                        if (viewportMouseY >= msgTopY && viewportMouseY < msgBottomY) {
                            newHoveredIndex = msgIndex;
                        }
                    }

                    // ホバー中のメッセージにハイライト表示
                    if (hoveredMessageIndex == msgIndex) {
                        int highlightTop = startY + msgStartLine * LINE_HEIGHT - 1;
                        int highlightBottom = startY + line * LINE_HEIGHT;
                        if (highlightTop < scaledChatHistoryHeight && highlightBottom > 0) {
                            context.fill(drawX - 2, highlightTop, rightEdge, highlightBottom, 0x18FFFFFF);
                            // コピーアイコン表示
                            boolean showCopied = copiedMessageIndex == msgIndex
                                    && System.currentTimeMillis() < copyFeedbackExpiry;
                            String copyLabel = showCopied ? "Copied!" : "[Copy]";
                            int copyColor = showCopied ? 0xFF55FF55 : 0xFFAAAAFF;
                            int labelW = mc.textRenderer.getWidth(copyLabel);
                            int labelX = drawX + maxTextWidth - labelW;
                            int labelY = highlightTop - LINE_HEIGHT;
                            if (labelY < 0) labelY = highlightBottom + 1;
                            context.fill(labelX - 2, labelY - 1, labelX + labelW + 2, labelY + 9, 0xCC000000);
                            context.drawTextWithShadow(mc.textRenderer, Text.literal(copyLabel),
                                    labelX, labelY, copyColor);
                        }
                    }

                    line++; // メッセージ間の空行
                    msgIndex++;
                }
            } else {
                // チャット履歴がない場合
                String noMessages = "メッセージなし";
                for (OrderedText wrapped : wrapText(mc, noMessages, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFF666666);
                    }
                    line++;
                }
                String hint = "Shannonにメッセージを送信できます";
                for (OrderedText wrapped : wrapText(mc, hint, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    if (textY >= 0 && textY + 10 <= scaledChatHistoryHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFF555555);
                    }
                    line++;
                }
            }

            hoveredMessageIndex = newHoveredIndex;

            // クリックでコピー
            if (mouseClicked && !wasMousePressed && hoveredMessageIndex >= 0
                    && chatState != null && chatState.messages != null
                    && hoveredMessageIndex < chatState.messages.size()) {
                ChatState.ChatMessage msg = chatState.messages.get(hoveredMessageIndex);
                String copyText = "[" + msg.sender + "] " + msg.message;
                mc.keyboard.setClipboard(copyText);
                copiedMessageIndex = hoveredMessageIndex;
                copyFeedbackExpiry = System.currentTimeMillis() + 1500;
                isInputFocused = false;
            }

            // チャット履歴の高さを計算（入力欄を除く）
            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= chatHistoryHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

        } finally {
            context.disableScissor();
            context.getMatrices().popMatrix();
        }

        // 入力欄の描画（スケール適用、スクロールの影響を受けない固定位置）
        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledChatHistoryHeight = (int) (chatHistoryHeight / SCALE);

            // 入力欄をUIの一番下に固定
            int inputBoxX = (int) (4 / SCALE);
            int inputBoxY = scaledChatHistoryHeight + 4;
            int inputBoxWidth = scaledUiWidth - 16;
            int inputBoxHeight = (int)(inputAreaHeight / SCALE) - 4;

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
            int textY = inputBoxY + (inputBoxHeight - 8) / 2;
            String displayText = currentInput;
            if (displayText.isEmpty()) {
                String placeholder = isInputFocused ? "" : "メッセージを入力...";
                context.drawText(mc.textRenderer, placeholder, textX, textY, 0xFF666666, false);
            } else {
                context.drawText(mc.textRenderer, displayText, textX, textY, 0xFFFFFFFF, false);
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
            context.disableScissor();
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

    /** Unix ms タイムスタンプを " HH:mm" 形式に変換 (JST) */
    private static String formatChatTime(long timestampMs) {
        if (timestampMs <= 0) return "";
        try {
            java.time.ZonedDateTime time = java.time.Instant.ofEpochMilli(timestampMs)
                    .atZone(java.time.ZoneId.of("Asia/Tokyo"));
            return String.format(" %02d:%02d", time.getHour(), time.getMinute());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getCurrentInput() {
        return currentInput;
    }

    public static void setCurrentInput(String input) {
        currentInput = input;
    }
}
