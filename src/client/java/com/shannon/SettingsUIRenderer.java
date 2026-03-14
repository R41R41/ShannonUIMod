package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.shannon.network.packet.ReactionSettingsState;
import com.shannon.network.packet.ReactionSettingUpdatePacket;
import com.shannon.network.packet.ReactionSettingsResetPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 設定タブ用レンダラー
 * スライダー値の数値直接入力対応
 */
public class SettingsUIRenderer {
    private static final float SCALE = RenderUtils.SCALE;
    private static final int SLIDER_WIDTH = 80;
    private static final int SLIDER_HEIGHT = 6;
    private static final int CHECKBOX_SIZE = 8;
    private static final int LINE_HEIGHT = 10;

    private static boolean wasMousePressed = false;
    private static String draggingSlider = null;

    // 数値入力モード
    private static String editingSlider = null; // 編集中のeventType（nullなら非編集）
    private static String editingValue = "";

    public static void renderSettings(DrawContext context, MinecraftClient mc, int x, int y,
            int uiWidth, int uiHeight, UIRenderer.UIState state, int scrollOffset,
            int mouseX, int mouseY, boolean mouseClicked) {
        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int yOff = (int) (-scrollOffset / SCALE);
            int rightEdge = scaledUiWidth - 16;
            int maxTextWidth = rightEdge - drawX;
            int startY = (int) (4 / SCALE) + yOff;

            // ヘッダー
            int headerY = startY + LINE_HEIGHT * line;
            if (headerY >= 0 && headerY + RenderUtils.SECTION_HEADER_HEIGHT <= scaledUiHeight) {
                RenderUtils.drawSectionHeader(context, mc, "設定",
                        drawX - 2, headerY, maxTextWidth + 2, RenderUtils.COLOR_HEADER);
            }
            line += 2; // SECTION_HEADER_HEIGHT分

            // リセットボタン
            int resetBtnX = rightEdge - 50;
            int resetBtnY = startY + LINE_HEIGHT;
            boolean resetClicked = RenderUtils.drawButton(context, mc, "リセット",
                    resetBtnX, resetBtnY, 50, 12,
                    scaledMouseX, scaledMouseY, mouseClicked, wasMousePressed);
            if (resetClicked) {
                sendResetRequest();
            }
            line++;

            // 反応設定を取得
            ReactionSettingsState settingsState = ShannonUIModClient.getReactionSettingsState();

            // === 反応イベント設定 ===
            line++;
            int reactHeaderY = startY + LINE_HEIGHT * line;
            if (reactHeaderY >= 0 && reactHeaderY + RenderUtils.SECTION_HEADER_HEIGHT <= scaledUiHeight) {
                RenderUtils.drawSectionHeader(context, mc, "反応イベント",
                        drawX - 2, reactHeaderY, maxTextWidth + 2, RenderUtils.COLOR_CATEGORY);
            }
            line += 2;

            if (settingsState != null && settingsState.reactions != null) {
                int reactionIdx = 0;
                for (ReactionSettingsState.ReactionConfig reaction : settingsState.reactions) {
                    // 交互背景色
                    if (reactionIdx % 2 == 1) {
                        int bgY = startY + LINE_HEIGHT * line;
                        int bgH = reaction.enabled ? LINE_HEIGHT * 2 + 2 : LINE_HEIGHT + 2;
                        context.fill(drawX - 2, bgY - 1, rightEdge, bgY + bgH, 0x11FFFFFF);
                    }
                    line = renderReactionSetting(context, mc, drawX, startY, line, scaledUiWidth, scaledUiHeight,
                            reaction, scaledMouseX, scaledMouseY, mouseClicked);
                    reactionIdx++;
                }
            } else {
                String noData = "  読み込み中...";
                for (OrderedText lineText : RenderUtils.wrapText(mc, noData, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, RenderUtils.COLOR_MUTED);
                    }
                    line++;
                }
                line++;
                String hint = "  (バックエンド接続を確認)";
                for (OrderedText lineText : RenderUtils.wrapText(mc, hint, maxTextWidth)) {
                    int textY = startY + LINE_HEIGHT * line;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, lineText, drawX, textY, 0xFF666666);
                    }
                    line++;
                }
            }

            state.contentHeight = (int) ((line + 2) * LINE_HEIGHT * SCALE) + 8;
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

            if (!mouseClicked) {
                draggingSlider = null;
            }

            // マウスクリックで編集中でない場所をクリックしたら編集終了
            if (mouseClicked && !wasMousePressed && editingSlider != null) {
                // 入力欄外をクリックしたら確定
                cancelEditing(settingsState);
            }

            wasMousePressed = mouseClicked;
        } finally {
            context.disableScissor();
            context.getMatrices().popMatrix();
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

        // チェックボックス
        int checkBgColor = overCheck ? 0xFF555555 : 0xFF333333;
        context.fill(checkX, checkY, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, checkBgColor);
        int borderColor = overCheck ? 0xFF888888 : 0xFF555555;
        context.fill(checkX, checkY, checkX + CHECKBOX_SIZE, checkY + 1, borderColor);
        context.fill(checkX, checkY, checkX + 1, checkY + CHECKBOX_SIZE, borderColor);
        context.fill(checkX, checkY + CHECKBOX_SIZE - 1, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, 0xFF222222);
        context.fill(checkX + CHECKBOX_SIZE - 1, checkY, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, 0xFF222222);
        if (reaction.enabled) {
            context.fill(checkX + 2, checkY + 2, checkX + CHECKBOX_SIZE - 2, checkY + CHECKBOX_SIZE - 2,
                    RenderUtils.COLOR_SUCCESS);
        }

        if (mouseClicked && overCheck && !wasMousePressed) {
            sendSettingUpdate(reaction.eventType, !reaction.enabled, reaction.probability);
        }

        String eventName = getEventDisplayName(reaction.eventType);
        int nameX = checkX + CHECKBOX_SIZE + 4;
        int nameColor = reaction.enabled ? 0xFFFFFFFF : 0xFF888888;
        context.drawTextWithShadow(mc.textRenderer, Text.literal(eventName), nameX, textY, nameColor);

        line++;
        textY = startY + LINE_HEIGHT * line;

        if (reaction.enabled) {
            int sliderX = drawX + indent + 16;
            int sliderY = textY + 1;

            boolean overSlider = mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH
                    && mouseY >= sliderY - 4 && mouseY <= sliderY + SLIDER_HEIGHT + 8;
            boolean isDragging = reaction.eventType.equals(draggingSlider);

            // スライダー背景
            int bgColor = (overSlider || isDragging) ? 0xFF444444 : 0xFF333333;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT, bgColor);

            // スライダー値
            int fillWidth = (int) (SLIDER_WIDTH * reaction.probability / 100.0);
            int sliderColor = getSliderColor(reaction.probability, overSlider || isDragging);
            context.fill(sliderX, sliderY, sliderX + fillWidth, sliderY + SLIDER_HEIGHT, sliderColor);

            // スライダー枠
            int sliderBorderColor = (overSlider || isDragging) ? 0xFF777777 : 0xFF555555;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 1, sliderBorderColor);
            context.fill(sliderX, sliderY + SLIDER_HEIGHT - 1, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT,
                    0xFF222222);

            // パーセント表示 or 入力欄
            int percentX = sliderX + SLIDER_WIDTH + 4;
            boolean isEditing = reaction.eventType.equals(editingSlider);

            if (isEditing) {
                // 入力モード: テキスト入力欄を表示
                int inputW = 30;
                int inputH = 10;
                context.fill(percentX - 1, sliderY - 2, percentX + inputW + 1, sliderY + inputH, 0xFF000000);
                context.fill(percentX - 1, sliderY - 2, percentX + inputW + 1, sliderY - 1, 0xFF5599FF);
                context.fill(percentX - 1, sliderY + inputH - 1, percentX + inputW + 1, sliderY + inputH, 0xFF5599FF);

                String displayVal = editingValue + "_";
                context.drawTextWithShadow(mc.textRenderer, Text.literal(displayVal),
                        percentX, sliderY - 1, 0xFFFFFFFF);
            } else {
                // 通常表示: クリックで入力モードに
                String percentText = reaction.probability + "%";
                boolean overPercent = mouseX >= percentX && mouseX <= percentX + 30
                        && mouseY >= sliderY - 4 && mouseY <= sliderY + SLIDER_HEIGHT + 8;
                int percentColor = overPercent ? 0xFF5599FF : ((overSlider || isDragging) ? 0xFFFFFFFF : 0xFFAAAAAA);
                context.drawTextWithShadow(mc.textRenderer, Text.literal(percentText),
                        percentX, sliderY - 1, percentColor);

                // パーセント表示をクリックで入力モードに
                if (mouseClicked && !wasMousePressed && overPercent) {
                    editingSlider = reaction.eventType;
                    editingValue = String.valueOf(reaction.probability);
                }
            }

            // スライダードラッグ
            if (mouseClicked && !wasMousePressed && overSlider && !isEditing) {
                draggingSlider = reaction.eventType;
            }

            if (mouseClicked && (isDragging || overSlider) && !isEditing) {
                int newValue = (int) ((mouseX - sliderX) * 100.0 / SLIDER_WIDTH);
                newValue = Math.max(0, Math.min(100, newValue));
                if (newValue != reaction.probability) {
                    sendSettingUpdate(reaction.eventType, reaction.enabled, newValue);
                }
            }
        }

        return line + 1;
    }

    // === キーボード入力ハンドリング ===

    /**
     * 数値入力中かどうか
     */
    public static boolean isEditing() {
        return editingSlider != null;
    }

    /**
     * キー入力処理（ShannonUIScreenから呼ばれる）
     */
    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editingSlider == null)
            return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            // 確定
            applyEditingValue();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // キャンセル
            editingSlider = null;
            editingValue = "";
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!editingValue.isEmpty()) {
                editingValue = editingValue.substring(0, editingValue.length() - 1);
            }
            return true;
        }

        return true; // 編集中は全キーを消費
    }

    /**
     * 文字入力処理（ShannonUIScreenから呼ばれる）
     */
    public static boolean handleCharTyped(char chr, int modifiers) {
        if (editingSlider == null)
            return false;

        // 数字のみ受け付け
        if (chr >= '0' && chr <= '9') {
            String newVal = editingValue + chr;
            // 3桁以下（0-100）に制限
            if (newVal.length() <= 3) {
                editingValue = newVal;
            }
        }
        return true;
    }

    /**
     * 入力値を適用
     */
    private static void applyEditingValue() {
        if (editingSlider == null)
            return;

        try {
            int value = Integer.parseInt(editingValue);
            value = Math.max(0, Math.min(100, value));

            ReactionSettingsState settingsState = ShannonUIModClient.getReactionSettingsState();
            if (settingsState != null && settingsState.reactions != null) {
                for (ReactionSettingsState.ReactionConfig r : settingsState.reactions) {
                    if (r.eventType.equals(editingSlider)) {
                        sendSettingUpdate(r.eventType, r.enabled, value);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            // 無視
        }

        editingSlider = null;
        editingValue = "";
    }

    /**
     * 編集キャンセル
     */
    private static void cancelEditing(ReactionSettingsState settingsState) {
        if (editingSlider == null)
            return;

        // まず値を適用してからリセット
        applyEditingValue();
    }

    private static String getEventDisplayName(String eventType) {
        switch (eventType) {
            case "player_facing":
                return "プレイヤーと対面（挨拶）";
            case "player_speak":
                return "プレイヤーの発言";
            case "hostile_approach":
                return "敵対MOBの接近";
            case "item_obtained":
                return "アイテム取得";
            case "time_change":
                return "時間変化";
            case "weather_change":
                return "天候変化";
            case "biome_change":
                return "バイオーム変化";
            case "teleported":
                return "テレポート";
            case "damage":
                return "ダメージ受信";
            case "suffocation":
                return "窒息";
            default:
                return eventType;
        }
    }

    private static int getSliderColor(int probability, boolean hovered) {
        if (probability >= 80)
            return hovered ? 0xFF77FF77 : 0xFF55FF55;
        if (probability >= 50)
            return hovered ? 0xFFFFFF77 : 0xFFFFFF55;
        if (probability >= 20)
            return hovered ? 0xFFFFCC77 : 0xFFFFAA55;
        return hovered ? 0xFFFF7777 : 0xFFFF5555;
    }

    private static void sendSettingUpdate(String eventType, boolean enabled, int probability) {
        try {
            ClientPlayNetworking.send(new ReactionSettingUpdatePacket(eventType, enabled, probability));
        } catch (Exception e) {
            System.err.println("Failed to send setting update: " + e.getMessage());
        }
    }

    private static void sendResetRequest() {
        try {
            ClientPlayNetworking.send(new ReactionSettingsResetPacket());
        } catch (Exception e) {
            System.err.println("Failed to send reset request: " + e.getMessage());
        }
    }
}
