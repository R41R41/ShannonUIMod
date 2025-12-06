package com.shannon.util;

import com.shannon.http.endpoints.ScreenshotEndpoint.ScreenshotOptions;
import com.shannon.http.endpoints.ScreenshotEndpoint.ScreenshotResult;
import com.shannon.http.endpoints.ScreenshotEndpoint.PlayerPosition;
import com.shannon.http.endpoints.ScreenshotEndpoint.PlayerRotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * スクリーンショット撮影ユーティリティ
 * Minecraftのフレームバッファをキャプチャしてエンコード
 */
public class ScreenshotUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotUtil.class);

    /**
     * 現在の画面をキャプチャ
     * ※メインスレッドから呼び出す必要がある
     */
    public static ScreenshotResult captureScreenshot(ScreenshotOptions options) {
        ScreenshotResult result = new ScreenshotResult();

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                result.success = false;
                result.error = "MinecraftClient is null";
                return result;
            }

            Framebuffer framebuffer = client.getFramebuffer();
            if (framebuffer == null) {
                result.success = false;
                result.error = "Framebuffer is null";
                return result;
            }

            int width = framebuffer.textureWidth;
            int height = framebuffer.textureHeight;

            // NativeImageを使用してスクリーンショットを撮影
            NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(framebuffer);

            if (nativeImage == null) {
                result.success = false;
                result.error = "Failed to capture screenshot";
                return result;
            }

            // NativeImageをBufferedImageに変換
            BufferedImage bufferedImage = nativeImageToBufferedImage(nativeImage);
            nativeImage.close();

            if (bufferedImage == null) {
                result.success = false;
                result.error = "Failed to convert image";
                return result;
            }

            // リサイズが指定されている場合
            if (options != null && options.width != null && options.height != null) {
                bufferedImage = resizeImage(bufferedImage, options.width, options.height);
            }

            // Base64エンコード
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            result.success = true;
            result.base64Image = "data:image/png;base64," + base64;
            result.width = bufferedImage.getWidth();
            result.height = bufferedImage.getHeight();

            // プレイヤー情報を追加
            if (client.player != null) {
                PlayerPosition pos = new PlayerPosition();
                pos.x = Math.round(client.player.getX() * 100.0) / 100.0;
                pos.y = Math.round(client.player.getY() * 100.0) / 100.0;
                pos.z = Math.round(client.player.getZ() * 100.0) / 100.0;
                result.playerPosition = pos;

                PlayerRotation rot = new PlayerRotation();
                rot.yaw = client.player.getYaw();
                rot.pitch = client.player.getPitch();
                result.playerRotation = rot;
            }

            return result;

        } catch (Exception e) {
            LOGGER.error("Screenshot capture error: {}", e.getMessage(), e);
            result.success = false;
            result.error = e.getMessage();
            return result;
        }
    }

    /**
     * NativeImageをBufferedImageに変換
     */
    private static BufferedImage nativeImageToBufferedImage(NativeImage nativeImage) {
        try {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // NativeImageはABGR形式
                    int abgr = nativeImage.getColorArgb(x, y);

                    // ABGRからARGBに変換
                    int a = (abgr >> 24) & 0xFF;
                    int b = (abgr >> 16) & 0xFF;
                    int g = (abgr >> 8) & 0xFF;
                    int r = abgr & 0xFF;

                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    bufferedImage.setRGB(x, y, argb);
                }
            }

            return bufferedImage;

        } catch (Exception e) {
            LOGGER.error("Image conversion error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 画像をリサイズ
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = resized.createGraphics();

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * ボットの視点からスクリーンショットを撮影
     * ワールド内のボットエンティティを見つけてカメラをアタッチ（プレイヤーは動かさない）
     * 
     * @param botName ボットのプレイヤー名
     */
    public static ScreenshotResult captureFromBotView(
            ScreenshotOptions options,
            String botName,
            double botX, double botY, double botZ,
            float botYaw, float botPitch) {

        ScreenshotResult result = new ScreenshotResult();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || client.world == null) {
            result.success = false;
            result.error = "MinecraftClient, player, or world is null";
            return result;
        }

        // 元のカメラエンティティを保存
        Entity originalCameraEntity = client.getCameraEntity();

        try {
            LOGGER.info("📸 Looking for bot entity: {} at ({}, {}, {})", botName, botX, botY, botZ);

            // ワールド内でボットのエンティティを検索
            Entity botEntity = null;
            for (net.minecraft.entity.player.PlayerEntity player : client.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(botName)) {
                    botEntity = player;
                    LOGGER.info("📸 Found bot entity: {}", player.getName().getString());
                    break;
                }
            }

            if (botEntity != null) {
                // ボットのエンティティにカメラをアタッチ
                client.setCameraEntity(botEntity);
                LOGGER.info("📸 Camera attached to bot entity");

                // カメラの位置を更新
                client.gameRenderer.getCamera().update(
                        client.world,
                        botEntity,
                        false, // not third person
                        false, // not inverted
                        1.0f // tick delta
                );

                // スクリーンショットを撮影
                result = captureScreenshot(options);

                // ボットの位置情報を結果に設定
                if (result.success) {
                    result.playerPosition = new PlayerPosition();
                    result.playerPosition.x = botEntity.getX();
                    result.playerPosition.y = botEntity.getY();
                    result.playerPosition.z = botEntity.getZ();

                    result.playerRotation = new PlayerRotation();
                    result.playerRotation.yaw = botEntity.getYaw();
                    result.playerRotation.pitch = botEntity.getPitch();
                }
            } else {
                // ボットが見つからない場合
                LOGGER.warn("📸 Bot entity '{}' not found in world", botName);
                result.success = false;
                result.error = "Bot '" + botName + "' not found. Make sure the bot is within render distance.";
            }

        } catch (Exception e) {
            LOGGER.error("Bot view screenshot error: {}", e.getMessage(), e);
            result.success = false;
            result.error = e.getMessage();
        } finally {
            // カメラを元のエンティティ（プレイヤー）に戻す
            if (originalCameraEntity != null) {
                client.setCameraEntity(originalCameraEntity);
            } else {
                client.setCameraEntity(client.player);
            }

            // カメラの位置を元に戻す
            client.gameRenderer.getCamera().update(
                    client.world,
                    client.getCameraEntity(),
                    false,
                    false,
                    1.0f);
        }

        return result;
    }

    // 保留中のスクリーンショットリクエスト
    private static PendingScreenshot pendingScreenshot = null;
    private static int frameWaitCount = 0;

    /**
     * ボット視点のスクリーンショットを撮影（フレーム待機あり）
     * カメラを変更して、数フレーム待ってからスクリーンショットを撮影
     */
    public static void captureFromBotViewDelayed(
            ScreenshotOptions options,
            String botName,
            double botX, double botY, double botZ,
            float botYaw, float botPitch,
            Consumer<ScreenshotResult> callback) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || client.world == null) {
            ScreenshotResult result = new ScreenshotResult();
            result.success = false;
            result.error = "MinecraftClient, player, or world is null";
            callback.accept(result);
            return;
        }

        LOGGER.info("📸 Looking for bot entity: {} at ({}, {}, {})", botName, botX, botY, botZ);

        // ワールド内でボットのエンティティを検索
        Entity botEntity = null;
        for (net.minecraft.entity.player.PlayerEntity player : client.world.getPlayers()) {
            LOGGER.info("📸 Checking player: {}", player.getName().getString());
            if (player.getName().getString().equalsIgnoreCase(botName)) {
                botEntity = player;
                LOGGER.info("📸 Found bot entity: {}", player.getName().getString());
                break;
            }
        }

        if (botEntity == null) {
            LOGGER.warn("📸 Bot entity '{}' not found in world", botName);
            ScreenshotResult result = new ScreenshotResult();
            result.success = false;
            result.error = "Bot '" + botName + "' not found. Make sure the bot is within render distance.";
            callback.accept(result);
            return;
        }

        // 元のカメラエンティティを保存
        Entity originalCameraEntity = client.getCameraEntity();

        // ボットのエンティティにカメラをアタッチ
        client.setCameraEntity(botEntity);
        LOGGER.info("📸 Camera attached to bot entity, waiting for frame render...");

        // 保留中のスクリーンショットを設定
        final Entity finalBotEntity = botEntity;
        pendingScreenshot = new PendingScreenshot(options, finalBotEntity, originalCameraEntity, callback);
        frameWaitCount = 0;
    }

    /**
     * 毎フレーム呼び出す（クライアントティックから）
     * 保留中のスクリーンショットがあれば処理
     */
    public static void tick() {
        if (pendingScreenshot == null) {
            return;
        }

        frameWaitCount++;

        // 3フレーム待ってからスクリーンショットを撮影
        if (frameWaitCount < 3) {
            return;
        }

        LOGGER.info("📸 Taking screenshot after {} frames", frameWaitCount);

        MinecraftClient client = MinecraftClient.getInstance();
        PendingScreenshot pending = pendingScreenshot;
        pendingScreenshot = null;

        try {
            // スクリーンショットを撮影
            ScreenshotResult result = captureScreenshot(pending.options);

            // ボットの位置情報を結果に設定
            if (result.success && pending.botEntity != null) {
                result.playerPosition = new PlayerPosition();
                result.playerPosition.x = pending.botEntity.getX();
                result.playerPosition.y = pending.botEntity.getY();
                result.playerPosition.z = pending.botEntity.getZ();

                result.playerRotation = new PlayerRotation();
                result.playerRotation.yaw = pending.botEntity.getYaw();
                result.playerRotation.pitch = pending.botEntity.getPitch();
            }

            pending.callback.accept(result);

        } catch (Exception e) {
            LOGGER.error("Screenshot error: {}", e.getMessage(), e);
            ScreenshotResult result = new ScreenshotResult();
            result.success = false;
            result.error = e.getMessage();
            pending.callback.accept(result);
        } finally {
            // カメラを元のエンティティに戻す
            if (pending.originalCameraEntity != null) {
                client.setCameraEntity(pending.originalCameraEntity);
            } else if (client.player != null) {
                client.setCameraEntity(client.player);
            }
            LOGGER.info("📸 Camera restored to original entity");
        }
    }

    // 保留中のスクリーンショット情報
    private static class PendingScreenshot {
        final ScreenshotOptions options;
        final Entity botEntity;
        final Entity originalCameraEntity;
        final Consumer<ScreenshotResult> callback;

        PendingScreenshot(ScreenshotOptions options, Entity botEntity,
                Entity originalCameraEntity, Consumer<ScreenshotResult> callback) {
            this.options = options;
            this.botEntity = botEntity;
            this.originalCameraEntity = originalCameraEntity;
            this.callback = callback;
        }
    }
}
