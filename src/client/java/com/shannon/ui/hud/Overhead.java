package com.shannon.ui.hud;

import com.shannon.model.ChatState;
import com.shannon.model.TaskTreeState;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.gfx.PixelIcon;
import com.shannon.ui.state.BotStatus;
import com.shannon.ui.state.TaskView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

/**
 * What the bot is doing and saying, drawn just above its name tag while it is in view.
 *
 * <p>Drawn on the HUD at the bot's projected position rather than in the world, so it needs no
 * world rendering hooks and stays readable at any distance. It hides when a block is between the
 * camera and the bot, so it never shows through walls.
 */
public final class Overhead {
    /** Farther than this, vanilla name tags fade too, and so does this. */
    private static final double MAX_DISTANCE = 48;
    /** Above the bot's feet: its height plus room for vanilla's name tag. */
    private static final double ABOVE_HEAD = 0.8;
    private static final int BUBBLE_WIDTH = 150;
    private static final int PLATE_BACK = 0x40000000;

    private Overhead() {
    }

    /** Where the spot above the bot's name tag is on screen, in GUI pixels. */
    record Spot(int x, int y) {
    }

    /**
     * Draws the bot's state and its latest words above its head. Returns whether it drew the
     * words, so the HUD does not show them a second time under the card.
     */
    static boolean render(DrawContext context, ShannonClient shannon, float tickProgress, boolean speak) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity bot = BotLocator.findEntity(client, shannon.botName());
        if (bot == null || bot == client.getCameraEntity()) {
            return false;
        }
        Spot spot = project(client, bot, tickProgress, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        if (spot == null) {
            return false;
        }
        int y = spot.y();
        BotStatus status = shannon.store().status();
        boolean danger = shannon.danger().inDanger();
        if (danger || status != BotStatus.IDLE) {
            y -= plate(context, shannon, status, danger, spot.x(), y) + 2;
        }
        if (!speak) {
            return false;
        }
        ChatState.Message speech = ShannonHud.currentSpeech(shannon);
        if (speech == null) {
            return false;
        }
        bubble(context, Text.literal(speech.message), spot.x(), y, ShannonHud.speechAlpha(shannon));
        return true;
    }

    /** A small plate with the state icon and, while working, how far along the task is. Returns its height. */
    private static int plate(DrawContext context, ShannonClient shannon, BotStatus status, boolean danger, int centerX, int bottom) {
        PixelIcon icon = danger ? Icons.WARNING : Icons.status(status);
        TaskTreeState tree = shannon.store().taskTree();
        int[] progress = TaskView.progress(tree);
        boolean showBar = !danger && status == BotStatus.WORKING && progress[1] > 0;
        Text count = showBar ? Text.literal(progress[0] + "/" + progress[1]) : null;
        int width = 3 + 9 + (showBar ? 3 + 28 + 3 + Gui.width(count) : 0) + 3;
        int height = 11;
        int x = centerX - width / 2;
        int y = bottom - height;
        context.fill(x, y, x + width, y + height, PLATE_BACK);
        icon.draw(context, x + 3, y + 1);
        if (showBar) {
            Gui.progress(context, x + 15, y + 3, 28, 5, progress[0] / (float) progress[1]);
            Gui.text(context, count, x + 46, y + 2, Palette.WHITE);
        }
        return height;
    }

    /** A speech bubble whose tail points down at {@code centerX, bottom}. */
    private static void bubble(DrawContext context, Text text, int centerX, int bottom, float alpha) {
        List<OrderedText> lines = Gui.wrap(text, BUBBLE_WIDTH);
        if (lines.size() > 3) {
            lines = lines.subList(0, 3);
        }
        int textWidth = 0;
        for (OrderedText line : lines) {
            textWidth = Math.max(textWidth, Gui.font().getWidth(line));
        }
        int width = textWidth + 8;
        int height = lines.size() * 9 + 5;
        int x = centerX - width / 2;
        int y = bottom - 3 - height;
        int back = Palette.fade(Palette.HUD_BOX, alpha);
        context.fill(x, y, x + width, y + height, back);
        context.fill(centerX - 2, y + height, centerX + 2, y + height + 1, back);
        context.fill(centerX - 1, y + height + 1, centerX + 1, y + height + 2, back);
        for (int i = 0; i < lines.size(); i++) {
            Gui.text(context, lines.get(i), x + 4, y + 3 + i * 9, Palette.fade(Palette.WHITE, alpha));
        }
    }

    /**
     * Projects the spot above the bot's head onto the screen with the camera's position, angles
     * and field of view. Returns {@code null} when it is behind the camera, off screen, too far
     * or hidden behind blocks.
     */
    static Spot project(MinecraftClient client, PlayerEntity bot, float tickProgress, int screenWidth, int screenHeight) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d eye = camera.getCameraPos();
        Vec3d feet = bot.getLerpedPos(tickProgress);
        Vec3d target = feet.add(0, bot.getHeight() + ABOVE_HEAD, 0);
        Vec3d relative = target.subtract(eye);
        if (relative.length() > MAX_DISTANCE) {
            return null;
        }
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        // Minecraft's yaw 0 looks toward +Z and grows turning right; positive pitch looks down.
        Vec3d forward = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = right.crossProduct(forward);
        double depth = relative.dotProduct(forward);
        if (depth < 0.3) {
            return null;
        }
        double fov = Math.toRadians(fieldOfView(client));
        double scale = (screenHeight / 2.0) / Math.tan(fov / 2);
        int x = (int) Math.round(screenWidth / 2.0 + relative.dotProduct(right) / depth * scale);
        int y = (int) Math.round(screenHeight / 2.0 - relative.dotProduct(up) / depth * scale);
        if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
            return null;
        }
        Vec3d head = feet.add(0, bot.getStandingEyeHeight(), 0);
        HitResult hit = client.world.raycast(new RaycastContext(eye, head, RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE, client.player));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getPos().squaredDistanceTo(eye) < head.squaredDistanceTo(eye) - 0.25) {
            return null;
        }
        return new Spot(x, y);
    }

    /** The vertical field of view, as the game renders it but without the brief easing. */
    private static double fieldOfView(MinecraftClient client) {
        double fov = client.options.getFov().getValue();
        if (client.getCameraEntity() instanceof AbstractClientPlayerEntity player) {
            fov *= player.getFovMultiplier(client.options.getPerspective().isFirstPerson(),
                    client.options.getFovEffectScale().getValue().floatValue());
        }
        return fov;
    }
}
