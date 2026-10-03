package com.shannon.ui.hud;

import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;
import net.minecraft.world.waypoint.EntityTickProgress;

import java.util.UUID;

/**
 * Puts the bot's face over its dot on vanilla's locator bar, so the player can tell it apart from
 * other players at a glance.
 *
 * <p>Reads the same waypoints and does the same sums as vanilla's locator bar, and draws only
 * while vanilla shows that bar, so the face always sits exactly on the dot.
 */
public final class LocatorFace {
    /** Vanilla shows a waypoint only this many degrees either side of straight ahead. */
    private static final double HALF_ANGLE = 60;
    /** Vanilla's bar is 182 wide and its dot 9; the dot travels 173 pixels. */
    private static final double TRAVEL = 173;

    private LocatorFace() {
    }

    static void render(DrawContext context, ShannonClient shannon, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        Entity camera = client.getCameraEntity();
        if (player == null || camera == null || client.getNetworkHandler() == null || !locatorBarShown(client)) {
            return;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(shannon.botName());
        if (entry == null) {
            return;
        }
        UUID botId = entry.getProfile().id();
        World world = camera.getEntityWorld();
        TickManager ticks = world.getTickManager();
        EntityTickProgress progress = entity -> tickCounter.getTickProgress(!ticks.shouldSkipTick(entity));
        int barY = context.getScaledWindowHeight() - 24 - 5;
        int centerX = MathHelper.ceil((context.getScaledWindowWidth() - 9) / 2.0F);
        player.networkHandler.getWaypointHandler().forEachWaypoint(camera, waypoint -> {
            boolean isBot = waypoint.getSource().left().map(botId::equals).orElse(false);
            if (!isBot) {
                return;
            }
            double yaw = waypoint.getRelativeYaw(world, client.gameRenderer.getCamera(), progress);
            if (yaw <= -HALF_ANGLE || yaw > HALF_ANGLE) {
                return;
            }
            int offset = MathHelper.floor(yaw * TRAVEL / 2.0 / HALF_ANGLE);
            int x = centerX + offset;
            int y = barY - 2;
            context.fill(x - 1, y - 1, x + 10, y + 10, 0xFF000000);
            Gui.face(context, x, y, 9);
        });
    }

    /** Whether vanilla draws the locator bar this frame, by the same rule its HUD uses. */
    private static boolean locatorBarShown(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (!player.networkHandler.getWaypointHandler().hasWaypoint()) {
            return false;
        }
        boolean jumpBar = player.getJumpingMount() != null && player.getMountJumpStrength() > 0;
        boolean experienceBar = client.interactionManager != null && client.interactionManager.hasExperienceBar()
                && player.experienceBarDisplayStartTime + 100 > player.age;
        return !jumpBar && !experienceBar;
    }
}
