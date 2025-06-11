package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.ConstantSkillClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ConstantSkillsUIRenderer {
    private static boolean wasMousePressed = false;

    public static void renderConstantSkills(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, ConstantSkillsState constantSkillsState, int mouseX, int mouseY,
            boolean mouseClicked) {
        context.getMatrices().push();
        try {
            if (constantSkillsState == null)
                return;
            int line = 0;
            float scale = 1.0f;
            int drawX = 4;
            int drawY = 4;
            int yOffset = -state.scrollOffset;
            int maxTextWidth = uiWidth - 8;
            int startY = drawY + yOffset;

            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0f);

            for (ConstantSkillsState.ConstantSkill skill : constantSkillsState.skills) {
                String lineText = skill.skillName;
                for (OrderedText wrapped : wrapText(mc, lineText, maxTextWidth)) {
                    int textY = startY + line * 12;
                    int rectX1 = drawX;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + uiWidth - 8;
                    int rectY2 = rectY1 + 12;
                    boolean hovered = (mouseX >= rectX1 && mouseX <= rectX2 && mouseY >= rectY1 && mouseY <= rectY2);
                    if (textY >= 0 && textY + 12 <= uiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, uiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF);
                            context.drawText(mc.textRenderer, wrapped, drawX, textY, 0x000000, false);
                            if (mouseClicked && !wasMousePressed) {
                                ClientPlayNetworking.send(new ConstantSkillClickPacket(skill.skillName));
                            }
                        } else {
                            context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFFFFFF);
                        }
                    }
                    line++;
                }
            }
            state.contentHeight = (line + 1) * 10 + 8;
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