package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.InventoryItemClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

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
        context.getMatrices().pushMatrix();
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
        try {
            if (inventoryState == null) {
                state.contentHeight = uiHeight;
                return;
            }

            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-scrollOffset / SCALE);
            int rightEdge = scaledUiWidth - 16;
            int maxTextWidth = rightEdge - drawX;
            int startY = drawY + yOffset;
            int currentY = startY;

            // === Equipment セクション ===
            if (currentY >= 0 && currentY + RenderUtils.SECTION_HEADER_HEIGHT <= scaledUiHeight) {
                RenderUtils.drawSectionHeader(context, mc, "装備",
                        drawX - 2, currentY, maxTextWidth + 2, 0xFF5599CC);
            }
            currentY += RenderUtils.SECTION_HEADER_HEIGHT + 2;

            // 装備スロットを描画
            String[] equipLabels = { "mainhand", "offhand", "head", "chest", "legs", "feet" };

            // 装備カード背景（ヘッダーとアクセントバーが視覚的に連続するよう gap を狭く）
            int equipCardH = equipLabels.length * ITEM_ROW_HEIGHT + 4;
            if (currentY >= -equipCardH && currentY + equipCardH <= scaledUiHeight + equipCardH) {
                RenderUtils.drawCard(context, drawX - 2, currentY - 2, maxTextWidth + 2,
                        equipCardH, 0xFF5599CC, false);
            }
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
                    boolean hovered = scaledMouseX >= drawX + 4 && scaledMouseX <= drawX + 4 + SLOT_SIZE
                            && scaledMouseY >= slotMouseY && scaledMouseY <= slotMouseY + SLOT_SIZE;

                    // スロット背景（アクセントバーと被らないよう右寄せ）
                    int slotX = drawX + 4;
                    RenderUtils.drawSlotBackground(context, slotX, slotY, SLOT_SIZE, hovered);

                    // アイテムアイコン
                    if (item != null) {
                        ItemStack stack = getItemStack(item.name);
                        if (!stack.isEmpty()) {
                            context.drawItem(stack, slotX + 1, slotY + 1);
                        }
                    }

                    // ラベルとアイテム名（クライアント言語で表示）
                    String labelText = equipDisplayLabels[i] + ": "
                            + getLocalizedName(item);
                    if (slotY >= 0 && slotY + 10 <= scaledUiHeight) {
                        int textColor = hovered ? 0xFFFFFF55 : (item != null ? 0xFFFFFFFF : 0xFF888888);
                        context.drawTextWithShadow(mc.textRenderer, Text.literal(labelText),
                                slotX + SLOT_SIZE + 4, slotY + 5, textColor);
                    }

                    // クリック処理（Ctrl+クリックで1スタック全部捨てる）
                    if (hovered && mouseClicked && !wasMousePressed && item != null) {
                        boolean ctrl = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                                || GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                        int throwCount = ctrl ? parseCount(item.count) : 1;
                        ClientPlayNetworking.send(new InventoryItemClickPacket(item.name, throwCount));
                    }
                }

                currentY += ITEM_ROW_HEIGHT;
            }

            currentY += 6;

            // === Inventory セクション ===
            // インベントリ満タン警告
            if (inventoryState.isFull) {
                if (currentY >= 0 && currentY + 10 <= scaledUiHeight) {
                    // 警告カード背景
                    RenderUtils.drawCard(context, drawX - 2, currentY - 2, maxTextWidth + 2,
                            LINE_HEIGHT + 4, RenderUtils.COLOR_ERROR, false);
                    context.drawTextWithShadow(mc.textRenderer, Text.literal("! インベントリ満タン !"),
                            drawX + 4, currentY, RenderUtils.COLOR_ERROR);
                }
                currentY += LINE_HEIGHT + 6;
            }

            // アイテム数ヘッダー（スタック統合後の種類数 / 合計個数）
            int itemCount = inventoryState.items != null ? inventoryState.items.size() : 0;
            if (currentY >= 0 && currentY + RenderUtils.SECTION_HEADER_HEIGHT <= scaledUiHeight) {
                RenderUtils.drawSectionHeader(context, mc,
                        "インベントリ (" + itemCount + "個)",
                        drawX - 2, currentY, maxTextWidth + 2, RenderUtils.COLOR_HEADER);
            }
            currentY += RenderUtils.SECTION_HEADER_HEIGHT + 4;

            // アイテムリスト（アイコン付き、同名スタック統合）
            String hintText = null;
            int hintDrawX = drawX, hintDrawY = 0;
            if (inventoryState.items != null) {
                // 同名アイテムを統合: 表示名 → {代表Item, 合計count}
                java.util.LinkedHashMap<String, InventoryState.Item> reprMap = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, Integer> countMap = new java.util.LinkedHashMap<>();
                java.util.List<InventoryState.Item> sortedItems = new java.util.ArrayList<>(inventoryState.items);
                java.util.Collections.sort(sortedItems,
                        (a, b) -> getLocalizedName(a).compareToIgnoreCase(getLocalizedName(b)));
                for (InventoryState.Item it : sortedItems) {
                    String key = getLocalizedName(it);
                    int cnt = parseCount(it.count);
                    if (!reprMap.containsKey(key)) {
                        reprMap.put(key, it);
                        countMap.put(key, cnt);
                    } else {
                        countMap.put(key, countMap.get(key) + cnt);
                    }
                }

                int rowIdx = 0;
                for (java.util.Map.Entry<String, InventoryState.Item> entry : reprMap.entrySet()) {
                    String localName = entry.getKey();
                    InventoryState.Item item = entry.getValue();
                    int totalCount = countMap.get(localName);
                    int itemY = currentY;

                    if (itemY >= -SLOT_SIZE && itemY + SLOT_SIZE <= scaledUiHeight + SLOT_SIZE) {
                        int itemMouseY = itemY - yOffset;
                        boolean hovered = scaledMouseX >= drawX && scaledMouseX <= rightEdge
                                && scaledMouseY >= itemMouseY && scaledMouseY <= itemMouseY + SLOT_SIZE;

                        // 交互背景色
                        if (rowIdx % 2 == 0 && itemY >= 0 && itemY + SLOT_SIZE <= scaledUiHeight) {
                            context.fill(drawX - 2, itemY - 1, rightEdge, itemY + SLOT_SIZE + 1, 0x22FFFFFF);
                        }

                        // アイテムアイコン（アクセントバーと被らないよう右寄せ）
                        int itemSlotX = drawX + 4;
                        ItemStack stack = getItemStack(item.name);
                        if (!stack.isEmpty() && itemY >= 0 && itemY + SLOT_SIZE <= scaledUiHeight) {
                            RenderUtils.drawSlotBackground(context, itemSlotX, itemY, SLOT_SIZE, hovered);
                            context.drawItem(stack, itemSlotX + 1, itemY + 1);
                        }

                        // テキスト（クライアント言語で表示、合計数）
                        String itemText = localName + ": " + totalCount;
                        if (itemY >= 0 && itemY + 10 <= scaledUiHeight) {
                            if (hovered) {
                                context.fill(itemSlotX + SLOT_SIZE + 2, itemY,
                                        rightEdge, itemY + SLOT_SIZE, 0x44FFFFFF);
                                context.drawTextWithShadow(mc.textRenderer, Text.literal(itemText),
                                        itemSlotX + SLOT_SIZE + 4, itemY + 5, 0xFFFFFF55);
                                hintText = "クリック: 1個 | Ctrl: 全" + totalCount + "個";
                                hintDrawX = itemSlotX + SLOT_SIZE + 4;
                                hintDrawY = scaledUiHeight - 11;
                            } else {
                                context.drawTextWithShadow(mc.textRenderer, Text.literal(itemText),
                                        itemSlotX + SLOT_SIZE + 4, itemY + 5, 0xFFFFFFFF);
                            }
                        }

                        // クリック処理（Ctrl+クリックでスタック全数、通常クリックで1個）
                        if (hovered && mouseClicked && !wasMousePressed) {
                            boolean ctrl = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                                    || GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                            int throwCount = ctrl ? totalCount : 1;
                            ClientPlayNetworking.send(new InventoryItemClickPacket(item.name, throwCount));
                        }
                    }

                    currentY += ITEM_ROW_HEIGHT;
                    rowIdx++;
                }
            }

            // ホバーヒントツールチップ
            if (hintText != null) {
                int hw = mc.textRenderer.getWidth(hintText);
                context.fill(hintDrawX - 1, hintDrawY - 1, hintDrawX + hw + 2, hintDrawY + 9, 0xEE000022);
                context.drawTextWithShadow(mc.textRenderer, Text.literal(hintText), hintDrawX, hintDrawY, 0xFF6688AA);
            }

            // コンテンツ高さ計算
            int totalContentPixels = currentY - startY + 16;
            RenderUtils.updateContentHeight(state, totalContentPixels, SCALE, uiHeight);

        } finally {
            wasMousePressed = mouseClicked;
            context.disableScissor();
            context.getMatrices().popMatrix();
        }
    }

    /**
     * クライアント側の言語設定でアイテム表示名を取得
     */
    private static String getLocalizedName(InventoryState.Item item) {
        if (item == null) return "-";
        ItemStack stack = getItemStack(item.name);
        if (!stack.isEmpty()) {
            return stack.getName().getString();
        }
        return item.displayName;
    }

    /** count フィールド(String)を int に変換 */
    private static int parseCount(String count) {
        if (count == null || count.isEmpty()) return 1;
        try { return Integer.parseInt(count); } catch (NumberFormatException e) { return 1; }
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
