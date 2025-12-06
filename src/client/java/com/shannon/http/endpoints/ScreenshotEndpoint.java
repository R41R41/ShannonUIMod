package com.shannon.http.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shannon.util.ScreenshotUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * スクリーンショット取得エンドポイント
 * GET /screenshot - 現在の画面をキャプチャしてBase64で返す
 * POST /screenshot - リクエストボディで設定を指定可能
 * 
 * レスポンス:
 * {
 * "success": true,
 * "image": "data:image/png;base64,iVBORw0KGgoAAAANS...",
 * "width": 1920,
 * "height": 1080,
 * "playerPosition": { "x": 100, "y": 64, "z": 200 },
 * "playerRotation": { "yaw": 45.0, "pitch": 0.0 }
 * }
 */
public class ScreenshotEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotEndpoint.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORSヘッダー
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // オプションの設定を取得（POST時）
            ScreenshotOptions options = new ScreenshotOptions();
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.isEmpty()) {
                        options = MAPPER.readValue(body, ScreenshotOptions.class);
                    }
                } catch (Exception e) {
                    LOGGER.warn("リクエストボディのパースに失敗: {}", e.getMessage());
                }
            }

            // スクリーンショットを撮影（メインスレッドで実行が必要）
            final ScreenshotOptions finalOptions = options;
            CompletableFuture<ScreenshotResult> future = new CompletableFuture<>();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                sendErrorResponse(exchange, 500, "MinecraftClient is not initialized");
                return;
            }

            // メインスレッドで実行
            client.execute(() -> {
                try {
                    ScreenshotResult result = ScreenshotUtil.captureScreenshot(finalOptions);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            // 結果を待機（最大5秒）
            ScreenshotResult result = future.get(5, TimeUnit.SECONDS);

            if (result == null || !result.success) {
                String errorMsg = result != null ? result.error : "Screenshot capture failed";
                sendErrorResponse(exchange, 500, errorMsg);
                return;
            }

            // レスポンスを構築
            ScreenshotResponse response = new ScreenshotResponse();
            response.success = true;
            response.image = result.base64Image;
            response.width = result.width;
            response.height = result.height;
            response.playerPosition = result.playerPosition;
            response.playerRotation = result.playerRotation;

            String jsonResponse = MAPPER.writeValueAsString(response);
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }

            LOGGER.info("📸 Screenshot captured: {}x{}", result.width, result.height);

        } catch (Exception e) {
            LOGGER.error("Screenshot error: {}", e.getMessage(), e);
            sendErrorResponse(exchange, 500, e.getMessage());
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = "{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}";
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // リクエストオプション
    public static class ScreenshotOptions {
        public Integer width; // リサイズ先の幅（オプション）
        public Integer height; // リサイズ先の高さ（オプション）
        public Boolean hideUI; // UIを非表示にするか
        public Boolean hideHand; // 手を非表示にするか
    }

    // スクリーンショット結果（内部用）
    public static class ScreenshotResult {
        public boolean success;
        public String base64Image;
        public int width;
        public int height;
        public PlayerPosition playerPosition;
        public PlayerRotation playerRotation;
        public String error;
    }

    // プレイヤー位置
    public static class PlayerPosition {
        public double x;
        public double y;
        public double z;
    }

    // プレイヤー回転
    public static class PlayerRotation {
        public float yaw;
        public float pitch;
    }

    // レスポンス
    public static class ScreenshotResponse {
        public boolean success;
        public String image;
        public int width;
        public int height;
        public PlayerPosition playerPosition;
        public PlayerRotation playerRotation;
        public String error;
    }
}
