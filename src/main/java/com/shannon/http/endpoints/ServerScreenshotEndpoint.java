package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.ScreenshotRequestPacket;
import com.shannon.network.packet.ScreenshotResultPacket;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * /screenshot エンドポイント
 * バックエンドからのリクエストを受け取り、クライアントにパケットでスクリーンショット要求を送信
 * クライアントからの結果をパケットで受け取り、HTTPレスポンスとして返す
 */
public class ServerScreenshotEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerScreenshotEndpoint.class);
    private static final Gson GSON = new Gson();

    // リクエストIDとFutureのマッピング
    private static final Map<String, CompletableFuture<ScreenshotResultPacket>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        try {
            // リクエストボディを読み取り
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ScreenshotRequest request = GSON.fromJson(requestBody, ScreenshotRequest.class);

            // デフォルト値を設定
            int width = request.width > 0 ? request.width : 512;
            int height = request.height > 0 ? request.height : 512;
            String context = request.context != null ? request.context : "";

            // スクリーンショット撮影者を選択
            // 1. Rai1241がいればRai1241
            // 2. いなければボットに一番近いプレイヤー
            ServerPlayerEntity player = selectScreenshotPlayer(request.botX, request.botY, request.botZ);
            if (player == null) {
                sendResponse(exchange, 400, "{\"success\": false, \"error\": \"No player connected\"}");
                return;
            }
            LOGGER.info("📸 Screenshot player selected: {}", player.getName().getString());

            // リクエストIDを生成
            String requestId = UUID.randomUUID().toString();

            // Futureを作成して保存
            CompletableFuture<ScreenshotResultPacket> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);

            // ボット名のデフォルト値
            String botName = request.botName != null ? request.botName : "Shannon";

            // クライアントにスクリーンショット要求パケットを送信（ボットの名前・位置・向きを含む）
            ScreenshotRequestPacket packet = new ScreenshotRequestPacket(
                    requestId, width, height, context, botName,
                    request.botX, request.botY, request.botZ,
                    request.botYaw, request.botPitch);
            ServerPlayNetworking.send(player, packet);

            LOGGER.info("📸 Screenshot request sent to client: {} (bot: {} at {}, {}, {})",
                    requestId, botName, request.botX, request.botY, request.botZ);

            // 結果を待つ（タイムアウト: 10秒）
            ScreenshotResultPacket result = future.get(10, TimeUnit.SECONDS);

            // レスポンスを作成
            ScreenshotResponse response = new ScreenshotResponse();
            response.success = result.success();
            response.image = result.imageBase64();
            response.width = result.width();
            response.height = result.height();
            response.playerPosition = new PlayerPosition(result.playerX(), result.playerY(), result.playerZ());
            response.playerRotation = new PlayerRotation(result.playerYaw(), result.playerPitch());
            response.error = result.error();

            sendResponse(exchange, 200, GSON.toJson(response));

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Screenshot request timed out");
            sendResponse(exchange, 504, "{\"success\": false, \"error\": \"Screenshot request timed out\"}");
        } catch (Exception e) {
            LOGGER.error("Screenshot error: {}", e.getMessage(), e);
            sendResponse(exchange, 500, "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * クライアントからのスクリーンショット結果を受け取る
     */
    public static void handleResult(ScreenshotResultPacket result) {
        CompletableFuture<ScreenshotResultPacket> future = pendingRequests.remove(result.requestId());
        if (future != null) {
            future.complete(result);
            LOGGER.info("📸 Screenshot result received: {} (success: {})", result.requestId(), result.success());
        } else {
            LOGGER.warn("Unknown screenshot request ID: {}", result.requestId());
        }
    }

    /**
     * スクリーンショット撮影者を選択
     * 1. Rai1241がオンラインならRai1241
     * 2. いなければボットに一番近いプレイヤー
     */
    private ServerPlayerEntity selectScreenshotPlayer(double botX, double botY, double botZ) {
        var server = ShannonUIMod.getStateManager().getServer();
        if (server == null) {
            return null;
        }

        var players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return null;
        }

        // 1. Rai1241を探す
        for (ServerPlayerEntity player : players) {
            if (player.getName().getString().equalsIgnoreCase("Rai1241")) {
                LOGGER.info("📸 Primary screenshot player found: Rai1241");
                return player;
            }
        }

        // 2. ボットに一番近いプレイヤーを探す
        ServerPlayerEntity closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (ServerPlayerEntity player : players) {
            double dx = player.getX() - botX;
            double dy = player.getY() - botY;
            double dz = player.getZ() - botZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            }
        }

        if (closestPlayer != null) {
            LOGGER.info("📸 Closest player selected: {} (distance: {:.1f})",
                    closestPlayer.getName().getString(), closestDistance);
        }

        return closestPlayer;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // リクエスト用クラス
    public static class ScreenshotRequest {
        public int width;
        public int height;
        public String context;
        // ボットの名前
        public String botName;
        // ボットの位置と向き
        public double botX;
        public double botY;
        public double botZ;
        public float botYaw;
        public float botPitch;
    }

    // レスポンス用クラス
    public static class ScreenshotResponse {
        public boolean success;
        public String image;
        public int width;
        public int height;
        public PlayerPosition playerPosition;
        public PlayerRotation playerRotation;
        public String error;
    }

    public static class PlayerPosition {
        public double x, y, z;

        public PlayerPosition() {
        }

        public PlayerPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class PlayerRotation {
        public float yaw, pitch;

        public PlayerRotation() {
        }

        public PlayerRotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
