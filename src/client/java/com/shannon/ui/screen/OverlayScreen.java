package com.shannon.ui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A screen that floats over the game like the chat: the world keeps running and stays sharp
 * behind it.
 */
public abstract class OverlayScreen extends Screen {
    protected OverlayScreen(Text title) {
        super(title);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // No blur and no dimming: the player keeps watching the bot.
    }
}
