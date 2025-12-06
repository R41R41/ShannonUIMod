package com.shannon.util;

import com.shannon.http.endpoints.ScreenshotEndpoint.ScreenshotOptions;
import com.shannon.http.endpoints.ScreenshotEndpoint.ScreenshotResult;
import com.shannon.http.endpoints.ScreenshotEndpoint.PlayerPosition;
import com.shannon.http.endpoints.ScreenshotEndpoint.PlayerRotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

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
}
