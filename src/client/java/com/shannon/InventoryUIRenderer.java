package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.InventoryItemClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InventoryUIRenderer {

    private static boolean wasMousePressed = false;

    public static void renderInventory(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, int scrollOffset, InventoryState inventoryState, int mouseX,
            int mouseY, boolean mouseClicked) {
        context.getMatrices().push();
        try {
            if (inventoryState == null) {
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

            // 装備情報の表示
            String[] equipLabels = { "mainhand", "offhand", "head", "chest", "legs", "feet" };
            InventoryState.Item[] equipItems = {
                    inventoryState.mainHand,
                    inventoryState.offHand,
                    inventoryState.head,
                    inventoryState.chest,
                    inventoryState.legs,
                    inventoryState.feet
            };
            for (int i = 0; i < equipLabels.length; i++) {
                String label = equipLabels[i];
                InventoryState.Item item = equipItems[i];
                String text = label + " " + (item != null ? item.displayName + ": " + item.count : "-");
                for (OrderedText wrapped : wrapText(mc, text, maxTextWidth)) {
                    int textY = startY + line * 12;
                    int rectX1 = drawX;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + uiWidth - 8;
                    int rectY2 = rectY1 + 12;
                    boolean hovered = (mouseX >= rectX1 && mouseX <= rectX2 && mouseY >= rectY1 && mouseY <= rectY2);
                    if (textY >= 0 && textY + 12 <= uiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, uiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF); // 背景を白
                            context.drawText(mc.textRenderer, wrapped, drawX, textY, 0x000000, false);
                            if (mouseClicked && !wasMousePressed && item != null) {
                                ClientPlayNetworking.send(new InventoryItemClickPacket(item.name));
                            }
                        } else {
                            context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xAAAAAA);
                        }
                    }
                    line++;
                }
            }

            // インベントリ満タン表示
            if (inventoryState.isFull) {
                String fullText = "※インベントリが満タンです！";
                for (OrderedText wrapped : wrapText(mc, fullText, maxTextWidth)) {
                    int textY = startY + line * 12;
                    if (textY >= 0 && textY + 12 <= uiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFF4444);
                    }
                    line++;
                }
            }

            // アイテムリスト表示
            for (InventoryState.Item item : inventoryState.items) {
                String lineText = item.displayName + ": " + item.count;
                for (OrderedText wrapped : wrapText(mc, lineText, maxTextWidth)) {
                    int textY = startY + line * 12;
                    int rectX1 = drawX;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + uiWidth - 8;
                    int rectY2 = rectY1 + 12;
                    boolean hovered = (mouseX >= rectX1 && mouseX <= rectX2 && mouseY >= rectY1 && mouseY <= rectY2);
                    if (textY >= 0 && textY + 12 <= uiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, uiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF); // 背景を白
                            context.drawText(mc.textRenderer, wrapped, drawX, textY, 0x000000, false);
                            if (mouseClicked && !wasMousePressed) {
                                ClientPlayNetworking.send(new InventoryItemClickPacket(item.name));
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