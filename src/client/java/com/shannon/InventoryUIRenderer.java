package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.InventoryItemClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InventoryUIRenderer {
    private static final float SCALE = 0.7f;
    private static final int LINE_HEIGHT = 10;
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

            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(SCALE, SCALE, 1.0f);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = scaledUiWidth - 8;
            int startY = drawY + yOffset;

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
                    int textY = startY + line * LINE_HEIGHT;
                    int rectX1 = drawX;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + scaledUiWidth - 32;
                    int rectY2 = rectY1 + LINE_HEIGHT;
                    boolean hovered = (scaledMouseX >= rectX1 && scaledMouseX <= rectX2 && scaledMouseY >= rectY1
                            && scaledMouseY <= rectY2);
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, scaledUiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF);
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
                String fullText = "※Inventory is full!";
                for (OrderedText wrapped : wrapText(mc, fullText, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        context.drawTextWithShadow(mc.textRenderer, wrapped, drawX, textY, 0xFF4444);
                    }
                    line++;
                }
            }

            // アイテムリスト表示
            java.util.List<InventoryState.Item> sortedItems = new java.util.ArrayList<>(inventoryState.items);
            java.util.Collections.sort(sortedItems, (a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
            for (InventoryState.Item item : sortedItems) {
                String lineText = item.displayName + ": " + item.count;
                for (OrderedText wrapped : wrapText(mc, lineText, maxTextWidth)) {
                    int textY = startY + line * LINE_HEIGHT;
                    int rectX1 = drawX;
                    int rectY1 = textY - 2 - yOffset;
                    int rectX2 = rectX1 + scaledUiWidth - 32;
                    int rectY2 = rectY1 + LINE_HEIGHT;
                    boolean hovered = (scaledMouseX >= rectX1 && scaledMouseX <= rectX2 && scaledMouseY >= rectY1
                            && scaledMouseY <= rectY2);
                    if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                        if (hovered) {
                            context.fill(rectX1 - 1, rectY1 + yOffset, scaledUiWidth - 8, rectY2 + yOffset, 0xFFFFFFFF);
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
            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;

            // 表示するものが何もない、または少ない場合はスクロールを一番上に
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
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