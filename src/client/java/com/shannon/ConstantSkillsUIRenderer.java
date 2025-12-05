package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.ConstantSkillClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class ConstantSkillsUIRenderer {
    private static final float SCALE = 0.7f;
    private static final int LINE_HEIGHT = 10;
    private static final Identifier STATUS_TRUE = Identifier.of("shannonuimod", "textures/status_true.png");
    private static final Identifier STATUS_FALSE = Identifier.of("shannonuimod", "textures/status_false.png");
    private static boolean wasMousePressed = false;

    public static void renderConstantSkills(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, ConstantSkillsState constantSkillsState, int mouseX, int mouseY,
            boolean mouseClicked) {
        context.getMatrices().push();
        try {
            if (constantSkillsState == null) {
                state.contentHeight = uiHeight;
                return;
            }

            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-state.scrollOffset / SCALE);
            int maxTextWidth = scaledUiWidth - 24;
            int startY = drawY + yOffset;

            // ツールチップ用一時変数
            String tooltipDesc = null;
            int tooltipX = 0, tooltipY = 0, tooltipWidth = 0;
            java.util.List<OrderedText> tooltipLines = null;

            int hoveredLine = -1;
            int hoveredLineCount = 0;

            for (ConstantSkillsState.ConstantSkill skill : constantSkillsState.skills) {
                String lineText = skill.skillName;
                // ステータスの円形を描画
                int statusY = startY + line * LINE_HEIGHT;
                if (statusY >= 0 && statusY + 10 <= scaledUiHeight) {
                    if (skill.status) {
                        context.drawTexture(
                                RenderLayer::getGuiTextured,
                                STATUS_TRUE,
                                drawX, statusY,
                                0, 0,
                                8, 8,
                                8, 8);
                    } else {
                        context.drawTexture(
                                RenderLayer::getGuiTextured,
                                STATUS_FALSE,
                                drawX, statusY,
                                0, 0,
                                8, 8,
                                8, 8);
                    }
                }
                for (OrderedText wrapped : wrapText(mc, lineText, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    int rectX1 = drawX + 12;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + scaledUiWidth - 32;
                    int rectY2 = rectY1 + LINE_HEIGHT;
                    boolean hovered = (scaledMouseX >= rectX1 && scaledMouseX <= rectX2 && scaledMouseY >= rectY1
                            && scaledMouseY <= rectY2);
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, scaledUiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF);
                            context.drawText(mc.textRenderer, wrapped, drawX + 12, textY, 0xFF000000, false);

                            if (mouseClicked && !wasMousePressed) {
                                ClientPlayNetworking.send(new ConstantSkillClickPacket(skill.skillName, !skill.status));
                            }
                            // ツールチップ情報を保存
                            if (skill.description != null && !skill.description.isEmpty()) {
                                tooltipDesc = skill.description;
                                tooltipX = rectX1;
                                tooltipY = textY + 12;
                                tooltipLines = wrapText(mc, skill.description, scaledUiWidth - 32);
                                tooltipWidth = 0;
                                for (OrderedText descLine : tooltipLines) {
                                    int w = mc.textRenderer.getWidth(descLine);
                                    if (w > tooltipWidth)
                                        tooltipWidth = w;
                                }
                            }
                            hoveredLine = line;
                            hoveredLineCount = wrapText(mc, lineText, maxTextWidth).size();
                        } else {
                            context.drawText(mc.textRenderer, wrapped, drawX + 12, textY, 0xFFFFFFFF, false);
                        }
                    }
                    line++;
                }
            }
            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

            // 3行分の黒背景とテキストスキップ
            if (hoveredLine != -1) {
                int skipStart = hoveredLine + 1;
                int skipEnd = hoveredLine + 3;
                for (int i = skipStart; i <= skipEnd; i++) {
                    int textY = startY + i * LINE_HEIGHT;
                    int rectX1 = drawX + 12;
                    int rectY1 = textY - 2 - yOffset;
                    int rectY2 = rectY1 + LINE_HEIGHT;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.fill(rectX1 - 1, rectY1 + yOffset, scaledUiWidth - 8, rectY2 + yOffset, 0xFF000000);
                    }
                }
            }

            // ループ後にツールチップを最前面に描画
            if (tooltipDesc != null && tooltipLines != null) {
                context.fill(tooltipX - 1, tooltipY - 3, scaledUiWidth - 8, tooltipY + 30,
                        0xF0000000);
                int descY = tooltipY;
                for (OrderedText descLine : tooltipLines) {
                    context.drawTextWithShadow(mc.textRenderer, descLine, tooltipX, descY, 0xFFFFFF);
                    descY += 12;
                }
            }
        } finally {
            wasMousePressed = mouseClicked;
            context.getMatrices().pop();
        }
    }

    public static java.util.List<OrderedText> wrapText(MinecraftClient mc, String text, int maxWidth) {
        Text txt = Text.literal(text);
        return mc.textRenderer.wrapLines(txt, maxWidth);
    }
}