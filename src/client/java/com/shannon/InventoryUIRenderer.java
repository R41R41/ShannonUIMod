package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class InventoryUIRenderer {
    public static void renderInventory(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state) {
        // TODO: インベントリ表示処理をここに実装
        state.contentHeight = 100;
        context.drawTextWithShadow(mc.textRenderer, Text.literal("インベントリ"), x, y, 0xFFFFFF);
    }
}