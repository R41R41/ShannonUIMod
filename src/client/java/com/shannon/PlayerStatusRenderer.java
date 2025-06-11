package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class PlayerStatusRenderer {
    private static final Identifier HEART_CONTAINER = Identifier.of("minecraft",
            "textures/gui/sprites/hud/heart/container.png");
    private static final Identifier HEART_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/half.png");
    private static final Identifier SHANNON_ICON = Identifier.of("shannonuimod", "textures/shannon.png");
    private static final Identifier HUNGER_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/food_full.png");
    private static final Identifier HUNGER_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/food_half.png");
    private static final Identifier HUNGER_EMPTY = Identifier.of("minecraft",
            "textures/gui/sprites/hud/food_empty.png");

    public static void renderPlayerStatus(DrawContext context, MinecraftClient mc, int windowWidth, int windowHeight,
            int textureSize) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!ShannonUIMod.TARGET_PLAYER_NAME.equals(player.getName().getString())) {
                continue;
            }
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            int hearts = (int) Math.ceil(maxHealth / 2.0);
            int fullHearts = (int) (health / 2);
            boolean hasHalfHeart = (health % 2) == 1;
            int x = screenWidth - windowWidth - 10;
            int y = screenHeight - windowHeight - 10;
            context.drawTexture(RenderLayer::getGuiTextured,
                    SHANNON_ICON,
                    x, y,
                    0, 0,
                    textureSize * 2 + 2, textureSize * 2 + 2,
                    textureSize * 2 + 2, textureSize * 2 + 2);
            x += textureSize * 2 + 2 + textureSize;
            for (int i = 0; i < hearts; i++) {
                Identifier heartTex;
                if (i < fullHearts) {
                    heartTex = HEART_FULL;
                } else if (i == fullHearts && hasHalfHeart) {
                    heartTex = HEART_HALF;
                } else {
                    heartTex = HEART_CONTAINER;
                }
                context.drawTexture(RenderLayer::getGuiTextured,
                        HEART_CONTAINER,
                        x + i * textureSize, y,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        heartTex,
                        x + i * textureSize, y,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
            }
            int hunger = player.getHungerManager().getFoodLevel();
            int hungerIcons = 10;
            int fullHunger = hunger / 2;
            boolean hasHalfHunger = (hunger % 2) == 1;
            for (int i = 0; i < hungerIcons; i++) {
                Identifier hungerTex;
                if (i < fullHunger) {
                    hungerTex = HUNGER_FULL;
                } else if (i == fullHunger && hasHalfHunger) {
                    hungerTex = HUNGER_HALF;
                } else {
                    hungerTex = HUNGER_EMPTY;
                }
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        HUNGER_EMPTY,
                        x + (9 - i) * textureSize, y + textureSize + 2,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        hungerTex,
                        x + (9 - i) * textureSize, y + textureSize + 2,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
            }
            break;
        }
    }
}