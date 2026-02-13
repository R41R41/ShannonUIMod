package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.InventoryItemClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * インベントリUIレンダラー
 * アイテムアイコン表示＆装備スロットの視覚化対応
 */
public class InventoryUIRenderer {
    private static final float SCALE = RenderUtils.SCALE;
    private static final int LINE_HEIGHT = 10;
    private static final int SLOT_SIZE = 18; // スロット背景のサイズ（16pxアイコン + 2px余白）
    private static final int ITEM_ROW_HEIGHT = 20; // アイテム行の高さ（アイコン+テキスト）
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

            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int maxTextWidth = scaledUiWidth - 8;
            int startY = drawY + yOffset;
            int currentY = startY;

            // === Equipment セクション ===
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer, Text.literal("装備"), drawX, currentY,
                        RenderUtils.COLOR_HEADER);
            }
            currentY += LINE_HEIGHT + 4;

            // 装備スロットを描画
            String[] equipLabels = { "mainhand", "offhand", "head", "chest", "legs", "feet" };
            String[] equipDisplayLabels = { "メイン", "オフハンド", "頭", "胸", "脚", "足" };
            InventoryState.Item[] equipItems = {
                    inventoryState.mainHand, inventoryState.offHand,
                    inventoryState.head, inventoryState.chest,
                    inventoryState.legs, inventoryState.feet
            };

            for (int i = 0; i < equipLabels.length; i++) {
                InventoryState.Item item = equipItems[i];
                int slotY = currentY;

                if (slotY >= -SLOT_SIZE && slotY + SLOT_SIZE <= scaledUiHeight + SLOT_SIZE) {
                    int slotMouseY = slotY - yOffset;
                    boolean hovered = scaledMouseX >= drawX && scaledMouseX <= drawX + SLOT_SIZE
                            && scaledMouseY >= slotMouseY && scaledMouseY <= slotMouseY + SLOT_SIZE;

                    // スロット背景
                    RenderUtils.drawSlotBackground(context, drawX, slotY, SLOT_SIZE, hovered);

                    // アイテムアイコン
                    if (item != null) {
                        ItemStack stack = getItemStack(item.name);
                        if (!stack.isEmpty()) {
                            context.drawItem(stack, drawX + 1, slotY + 1);
                        }
                    }

                    // ラベルとアイテム名
                    String labelText = equipDisplayLabels[i] + ": "
                            + (item != null ? item.displayName : "-");
                    if (slotY >= 0 && slotY + 10 <= scaledUiHeight) {
                        int textColor = hovered ? 0xFFFF55 : (item != null ? 0xFFFFFF : 0x888888);
                        context.drawTextWithShadow(mc.textRenderer, Text.literal(labelText),
                                drawX + SLOT_SIZE + 4, slotY + 5, textColor);
                    }

                    // クリック処理
                    if (hovered && mouseClicked && !wasMousePressed && item != null) {
                        ClientPlayNetworking.send(new InventoryItemClickPacket(item.name));
                    }
                }

                currentY += ITEM_ROW_HEIGHT;
            }

            currentY += 4;

            // セパレーター
            if (currentY >= 0 && currentY <= scaledUiHeight) {
                context.fill(drawX, currentY, scaledUiWidth - 8, currentY + 1, RenderUtils.COLOR_SEPARATOR);
            }
            currentY += 6;

            // === Inventory セクション ===
            // インベントリ満タン警告
            if (inventoryState.isFull) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("! インベントリ満タン !"),
                            drawX, currentY, RenderUtils.COLOR_ERROR);
                }
                currentY += LINE_HEIGHT + 2;
            }

            // アイテム数ヘッダー
            int itemCount = inventoryState.items != null ? inventoryState.items.size() : 0;
            if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                context.drawTextWithShadow(mc.textRenderer,
                        Text.literal("インベントリ (" + itemCount + "個)"),
                        drawX, currentY, RenderUtils.COLOR_HEADER);
            }
            currentY += LINE_HEIGHT + 4;

            // アイテムリスト（アイコン付き）
            if (inventoryState.items != null) {
                java.util.List<InventoryState.Item> sortedItems = new java.util.ArrayList<>(inventoryState.items);
                java.util.Collections.sort(sortedItems, (a, b) -> a.displayName.compareToIgnoreCase(b.displayName));

                for (InventoryState.Item item : sortedItems) {
                    int itemY = currentY;

                    if (itemY >= -SLOT_SIZE && itemY + SLOT_SIZE <= scaledUiHeight + SLOT_SIZE) {
                        int itemMouseY = itemY - yOffset;
                        boolean hovered = scaledMouseX >= drawX && scaledMouseX <= scaledUiWidth - 8
                                && scaledMouseY >= itemMouseY && scaledMouseY <= itemMouseY + SLOT_SIZE;

                        // アイテムアイコン（小さめスロット）
                        ItemStack stack = getItemStack(item.name);
                        if (!stack.isEmpty() && itemY >= 0 && itemY + SLOT_SIZE <= scaledUiHeight) {
                            RenderUtils.drawSlotBackground(context, drawX, itemY, SLOT_SIZE, hovered);
                            context.drawItem(stack, drawX + 1, itemY + 1);
                        }

                        // テキスト
                        String itemText = item.displayName + ": " + item.count;
                        if (itemY >= 0 && itemY + 10 <= scaledUiHeight) {
                            if (hovered) {
                                context.fill(drawX + SLOT_SIZE + 2, itemY,
                                        scaledUiWidth - 8, itemY + SLOT_SIZE, 0x44FFFFFF);
                                context.drawTextWithShadow(mc.textRenderer, Text.literal(itemText),
                                        drawX + SLOT_SIZE + 4, itemY + 5, 0xFFFF55);
                            } else {
                                context.drawTextWithShadow(mc.textRenderer, Text.literal(itemText),
                                        drawX + SLOT_SIZE + 4, itemY + 5, 0xFFFFFF);
                            }
                        }

                        // クリック処理
                        if (hovered && mouseClicked && !wasMousePressed) {
                            ClientPlayNetworking.send(new InventoryItemClickPacket(item.name));
                        }
                    }

                    currentY += ITEM_ROW_HEIGHT;
                }
            }

            // コンテンツ高さ計算
            int totalContentPixels = currentY - startY + 16;
            RenderUtils.updateContentHeight(state, totalContentPixels, SCALE, uiHeight);

        } finally {
            wasMousePressed = mouseClicked;
            context.getMatrices().pop();
        }
    }

    /**
     * アイテム名からItemStackを生成
     */
    private static ItemStack getItemStack(String itemName) {
        try {
            if (itemName == null || itemName.isEmpty() || itemName.equals("-"))
                return ItemStack.EMPTY;

            Identifier id;
            if (itemName.contains(":")) {
                id = Identifier.of(itemName);
            } else {
                id = Identifier.of("minecraft", itemName);
            }

            net.minecraft.item.Item item = Registries.ITEM.get(id);
            if (item != null) {
                return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
