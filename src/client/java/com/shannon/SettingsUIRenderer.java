package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.shannon.network.packet.ReactionSettingsState;
import com.shannon.network.packet.ReactionSettingUpdatePacket;
import com.shannon.network.packet.ReactionSettingsResetPacket;

/**
 * 設定タブ用レンダラー
 * 反応イベントと常時スキルの設定UI
 */
public class SettingsUIRenderer {
    private static final float SCALE = 0.7f;
    private static final int SLIDER_WIDTH = 80;
    private static final int SLIDER_HEIGHT = 6;
    private static final int CHECKBOX_SIZE = 8;
    private static final int LINE_HEIGHT = 10;

    private static boolean wasMousePressed = false;
    private static String draggingSlider = null;

    public static void renderSettings(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, UIRenderer.UIState state, int scrollOffset,
            int mouseX, int mouseY, boolean mouseClicked) {
        context.getMatrices().push();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = scaledUiWidth - 8;
            int startY = (int) (4 / SCALE) + yOffset;

            // ヘッダー
            String header = "Settings";
            for (OrderedText lineText : wrapText(mc, header, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x55AAFF);
                }
                line++;
            }
            line++; // 空行

            // リセットボタン
            int resetBtnX = scaledUiWidth - 60;
            int resetBtnY = startY + LINE_HEIGHT;
            int resetBtnW = 50;
            int resetBtnH = 12;
            boolean overResetBtn = scaledMouseX >= resetBtnX && scaledMouseX <= resetBtnX + resetBtnW
                    && scaledMouseY >= resetBtnY && scaledMouseY <= resetBtnY + resetBtnH;
            int resetBtnColor = overResetBtn ? 0xFF666666 : 0xFF444444;
            context.fill(resetBtnX, resetBtnY, resetBtnX + resetBtnW, resetBtnY + resetBtnH, resetBtnColor);
            context.fill(resetBtnX, resetBtnY, resetBtnX + resetBtnW, resetBtnY + 1, 0xFF888888);
            context.fill(resetBtnX, resetBtnY, resetBtnX + 1, resetBtnY + resetBtnH, 0xFF888888);
            context.fill(resetBtnX, resetBtnY + resetBtnH - 1, resetBtnX + resetBtnW, resetBtnY + resetBtnH,
                    0xFF222222);
            context.fill(resetBtnX + resetBtnW - 1, resetBtnY, resetBtnX + resetBtnW, resetBtnY + resetBtnH,
                    0xFF222222);
            context.drawTextWithShadow(mc.textRenderer, Text.literal("Reset"), resetBtnX + 10, resetBtnY + 2,
                    overResetBtn ? 0xFFFFFF : 0xCCCCCC);

            if (mouseClicked && !wasMousePressed && overResetBtn) {
                sendResetRequest();
            }
            line++;

            // 反応設定を取得
            ReactionSettingsState settingsState = ShannonUIModClient.getReactionSettingsState();

            // === 反応イベント設定 ===
            line++;
            String reactionsHeader = "Reaction Events";
            for (OrderedText lineText : wrapText(mc, reactionsHeader, maxTextWidth)) {
                int textY = startY + LINE_HEIGHT * line;
                if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFFAA55);
                }
                line++;
            }

            if (settingsState != null && settingsState.reactions != null) {
                for (ReactionSettingsState.ReactionConfig reaction : settingsState.reactions) {
                    line = renderReactionSetting(context, mc, drawX, startY, line, scaledUiWidth, scaledUiHeight,
                            reaction, scaledMouseX, scaledMouseY, mouseClicked);
                }
            } else {
                String noData = "  Loading...";
                for (OrderedText lineText : wrapText(mc, noData, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x888888);
                    }
                    line++;
                }

                line++;
                String hint = "  (Check backend connection)";
                for (OrderedText lineText : wrapText(mc, hint, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0x666666);
                    }
                    line++;
                }
            }

            state.contentHeight = (int) ((line + 2) * LINE_HEIGHT * SCALE) + 8;

            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

            // マウスが離されたらドラッグ終了
            if (!mouseClicked) {
                draggingSlider = null;
            }

            // マウス状態更新
            wasMousePressed = mouseClicked;

        } finally {
            context.disableScissor();
            context.getMatrices().pop();
        }
    }

    /**
     * 反応設定の1行をレンダリング
     */
    private static int renderReactionSetting(DrawContext context, MinecraftClient mc,
            int drawX, int startY, int line, int scaledUiWidth, int scaledUiHeight,
            ReactionSettingsState.ReactionConfig reaction,
            int mouseX, int mouseY, boolean mouseClicked) {

        int textY = startY + LINE_HEIGHT * line;
        if (textY < -20 || textY > scaledUiHeight + 20) {
            return line + 2;
        }

        int indent = 8;

        int checkX = drawX + indent;
        int checkY = textY + 1;
        boolean overCheck = mouseX >= checkX && mouseX <= checkX + CHECKBOX_SIZE
                && mouseY >= checkY && mouseY <= checkY + CHECKBOX_SIZE;

        int checkBgColor = overCheck ? 0xFF555555 : 0xFF333333;
        context.fill(checkX, checkY, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, checkBgColor);

        int borderColor = overCheck ? 0xFF888888 : 0xFF555555;
        context.fill(checkX, checkY, checkX + CHECKBOX_SIZE, checkY + 1, borderColor);
        context.fill(checkX, checkY, checkX + 1, checkY + CHECKBOX_SIZE, borderColor);
        context.fill(checkX, checkY + CHECKBOX_SIZE - 1, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, 0xFF222222);
        context.fill(checkX + CHECKBOX_SIZE - 1, checkY, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, 0xFF222222);

        if (reaction.enabled) {
            context.fill(checkX + 2, checkY + 2, checkX + CHECKBOX_SIZE - 2, checkY + CHECKBOX_SIZE - 2, 0xFF55FF55);
        }

        if (mouseClicked && overCheck && !wasMousePressed) {
            sendSettingUpdate(reaction.eventType, !reaction.enabled, reaction.probability);
        }

        String eventName = getEventDisplayName(reaction.eventType);
        int nameX = checkX + CHECKBOX_SIZE + 4;
        int nameColor = reaction.enabled ? 0xFFFFFF : 0x888888;
        context.drawTextWithShadow(mc.textRenderer, Text.literal(eventName), nameX, textY, nameColor);

        line++;
        textY = startY + LINE_HEIGHT * line;

        if (reaction.enabled) {
            int sliderX = drawX + indent + 16;
            int sliderY = textY + 1;

            boolean overSlider = mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH
                    && mouseY >= sliderY && mouseY <= sliderY + SLIDER_HEIGHT + 4;
            boolean isDragging = reaction.eventType.equals(draggingSlider);

            int bgColor = (overSlider || isDragging) ? 0xFF444444 : 0xFF333333;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT, bgColor);

            int fillWidth = (int) (SLIDER_WIDTH * reaction.probability / 100.0);
            int sliderColor = getSliderColor(reaction.probability, overSlider || isDragging);
            context.fill(sliderX, sliderY, sliderX + fillWidth, sliderY + SLIDER_HEIGHT, sliderColor);

            int sliderBorderColor = (overSlider || isDragging) ? 0xFF777777 : 0xFF555555;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 1, sliderBorderColor);
            context.fill(sliderX, sliderY + SLIDER_HEIGHT - 1, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT,
                    0xFF222222);

            String percentText = reaction.probability + "%";
            int percentColor = (overSlider || isDragging) ? 0xFFFFFF : 0xAAAAAA;
            context.drawTextWithShadow(mc.textRenderer, Text.literal(percentText),
                    sliderX + SLIDER_WIDTH + 4, sliderY - 1, percentColor);

            if (mouseClicked && !wasMousePressed && overSlider) {
                draggingSlider = reaction.eventType;
            }

            if (mouseClicked && (isDragging || overSlider)) {
                int newValue = (int) ((mouseX - sliderX) * 100.0 / SLIDER_WIDTH);
                newValue = Math.max(0, Math.min(100, newValue));
                if (newValue != reaction.probability) {
                    sendSettingUpdate(reaction.eventType, reaction.enabled, newValue);
                }
            }
        }

        return line + 1;
    }

    /**
     * イベントタイプの表示名を取得
     */
    private static String getEventDisplayName(String eventType) {
        switch (eventType) {
            case "player_facing":
                return "Player Facing (greeting)";
            case "player_speak":
                return "Player Speak";
            case "hostile_approach":
                return "Hostile Mob Approach";
            case "item_obtained":
                return "Item Obtained";
            case "time_change":
                return "Time Change";
            case "weather_change":
                return "Weather Change";
            case "biome_change":
                return "Biome Change";
            case "teleported":
                return "Teleported";
            case "damage":
                return "Damage Received";
            case "suffocation":
                return "Suffocation";
            default:
                return eventType;
        }
    }

    /**
     * 確率に応じたスライダー色を取得
     */
    private static int getSliderColor(int probability, boolean hovered) {
        if (probability >= 80)
            return hovered ? 0xFF77FF77 : 0xFF55FF55;
        if (probability >= 50)
            return hovered ? 0xFFFFFF77 : 0xFFFFFF55;
        if (probability >= 20)
            return hovered ? 0xFFFFCC77 : 0xFFFFAA55;
        return hovered ? 0xFFFF7777 : 0xFFFF5555;
    }

    /**
     * 設定更新リクエストを送信（パケット経由）
     */
    private static void sendSettingUpdate(String eventType, boolean enabled, int probability) {
        try {
            ClientPlayNetworking.send(new ReactionSettingUpdatePacket(eventType, enabled, probability));
        } catch (Exception e) {
            System.err.println("Failed to send setting update: " + e.getMessage());
        }
    }

    /**
     * 設定リセットリクエストを送信（パケット経由）
     */
    private static void sendResetRequest() {
        try {
            ClientPlayNetworking.send(new ReactionSettingsResetPacket());
        } catch (Exception e) {
            System.err.println("Failed to send reset request: " + e.getMessage());
        }
    }

    private static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}
