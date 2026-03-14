package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import com.shannon.network.packet.PlayerStatusState;
import com.shannon.network.packet.TaskTreeState;

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
    private static final Identifier BUBBLE_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/air.png");
    private static final Identifier BUBBLE_BURST = Identifier.of("minecraft",
            "textures/gui/sprites/hud/air_bursting.png");

    public static void renderPlayerStatus(DrawContext context, MinecraftClient mc, int windowWidth, int windowHeight,
            int textureSize, PlayerStatusState state) {
        context.getMatrices().pushMatrix();
        try {
            if (state == null) {
                return;
            }
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            float health = state.health;
            float maxHealth = state.maxHealth;
            int hunger = state.hunger;

            int hearts = (int) Math.ceil(maxHealth / 2.0);
            int fullHearts = (int) (health / 2);
            boolean hasHalfHeart = (health % 2) == 1;
            int x = screenWidth - windowWidth - 10;
            int y = screenHeight - windowHeight - 10;
            context.drawTexture(RenderPipelines.GUI_TEXTURED,
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
                context.drawTexture(RenderPipelines.GUI_TEXTURED,
                        HEART_CONTAINER,
                        x + i * textureSize, y,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        heartTex,
                        x + i * textureSize, y,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
            }
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
                        RenderPipelines.GUI_TEXTURED,
                        HUNGER_EMPTY,
                        x + (9 - i) * textureSize, y + textureSize + 2,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        hungerTex,
                        x + (9 - i) * textureSize, y + textureSize + 2,
                        0, 0,
                        textureSize, textureSize,
                        textureSize, textureSize);
            }

            // 酸素ゲージ（水中のみ）
            PlayerEntity botPlayer = null;
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equals("I_am_Shannon")) {
                    botPlayer = player;
                    break;
                }
            }

            if (botPlayer != null && botPlayer.isSubmergedInWater()) {
                int air = botPlayer.getAir();
                int maxAir = botPlayer.getMaxAir();
                int bubbles = (int) Math.ceil((double) air / maxAir * 10);

                for (int i = 0; i < 10; i++) {
                    Identifier bubbleTex = (i < bubbles) ? BUBBLE_FULL : BUBBLE_BURST;
                    context.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            bubbleTex,
                            x + (9 - i) * textureSize, y + textureSize * 2 + 4,
                            0, 0,
                            textureSize, textureSize,
                            textureSize, textureSize);
                }
            }

            // === 座標・バイオーム表示 ===
            if (botPlayer != null && mc.world != null) {
                int bx = (int) botPlayer.getX();
                int by2 = (int) botPlayer.getY();
                int bz = (int) botPlayer.getZ();
                String posText = "X:" + bx + " Y:" + by2 + " Z:" + bz;

                // バイオーム名取得
                String biomeText = "";
                try {
                    var biomeEntry = mc.world.getBiome(botPlayer.getBlockPos());
                    biomeText = biomeEntry.getKey()
                            .map(k -> k.getValue().getPath().replace('_', ' '))
                            .orElse("");
                } catch (Exception ignored) {}

                int textX = screenWidth - windowWidth - 10;
                int textY = screenHeight - windowHeight - 10 - 10;

                // 座標テキスト背景と描画
                int posWidth = mc.textRenderer.getWidth(posText);
                context.fill(textX - 2, textY - 1, textX + posWidth + 2, textY + 9, 0x88000000);
                context.drawText(mc.textRenderer, posText, textX, textY, 0xFFAAAAAA, false);

                // バイオーム表示
                if (!biomeText.isEmpty()) {
                    int biomeY = textY - 11;
                    int biomeWidth = mc.textRenderer.getWidth(biomeText);
                    context.fill(textX - 2, biomeY - 1, textX + biomeWidth + 2, biomeY + 9, 0x88000000);
                    context.drawText(mc.textRenderer, biomeText, textX, biomeY, 0xFF88AACC, false);
                }

                // currentThinking（1行HUD表示）
                TaskTreeState taskTreeState = ShannonUIModClient.getTaskTreeState();
                if (taskTreeState != null && taskTreeState.currentThinking != null
                        && !taskTreeState.currentThinking.isEmpty()) {
                    String raw = taskTreeState.currentThinking;
                    // 最大50文字に切り詰め
                    String thinking = raw.length() > 50 ? raw.substring(0, 48) + "..." : raw;
                    String thinkLine = "💭 " + thinking;
                    int thinkY = textY - 22;
                    int thinkWidth = mc.textRenderer.getWidth(thinkLine);
                    context.fill(textX - 2, thinkY - 1, textX + thinkWidth + 2, thinkY + 9, 0xAA000011);
                    context.drawText(mc.textRenderer, thinkLine, textX, thinkY, 0xFF88CCFF, false);
                }
            }
        } finally {
            context.getMatrices().popMatrix();
        }
    }
}