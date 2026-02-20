package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shannon.ShannonUIMod;
import com.shannon.config.ModConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * /advancements エンドポイント (GET)
 * プレイヤーの進捗（実績）達成状況を返す
 *
 * クエリパラメータ:
 *   playerName - 対象プレイヤー名（省略時はTARGET_PLAYER_NAME）
 *   category   - フィルターカテゴリ（minecraft:story 等 / all）省略時はall
 */
public class AdvancementsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancementsEndpoint.class);
    private final Gson gson = new Gson();

    /**
     * サーバースレッドから収集する軽量データ
     */
    private record AdvancementData(
        String id,
        String category,
        String title,
        String description,
        String frame,
        boolean done,
        float progress,
        int criteriaCompleted,
        int criteriaTotal
    ) {}

    /**
     * サーバースレッドから収集する結果
     */
    private record CollectedData(
        String playerName,
        List<AdvancementData> advancements,
        String error
    ) {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            // クエリパラメータの解析
            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String playerName = params.getOrDefault("playerName", ModConfig.TARGET_PLAYER_NAME);
            String category = params.getOrDefault("category", "all");

            MinecraftServer server = ShannonUIMod.getStateManager().getServer();
            if (server == null) {
                sendErrorResponse(exchange, 503, "サーバーが初期化されていません");
                return;
            }

            // サーバースレッドでは最小限のデータ収集のみ行う
            CompletableFuture<CollectedData> future = new CompletableFuture<>();

            server.execute(() -> {
                try {
                    CollectedData data = collectAdvancementData(server, playerName, category);
                    future.complete(data);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            CollectedData collected = future.get(5, TimeUnit.SECONDS);

            // JSONシリアライズはHTTPスレッドで行う（サーバースレッドをブロックしない）
            String jsonResponse = buildJsonResponse(collected);

            // レスポンス送信
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();

        } catch (Exception e) {
            LOGGER.error("AdvancementsEndpointでエラーが発生しました", e);
            sendErrorResponse(exchange, 500, "進捗データの取得に失敗: " + e.getMessage());
        }
    }

    /**
     * サーバースレッドで実行: 最小限のデータ収集のみ
     * Jackson/JSONの操作は一切行わない
     */
    private CollectedData collectAdvancementData(MinecraftServer server, String playerName, String category) {
        // プレイヤーを検索
        ServerPlayerEntity targetPlayer = null;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            if (name.equals(playerName) || name.contains(playerName)) {
                targetPlayer = player;
                break;
            }
        }

        if (targetPlayer == null) {
            return new CollectedData(null, null, "プレイヤー「" + playerName + "」が見つかりません");
        }

        String resolvedPlayerName = targetPlayer.getName().getString();
        PlayerAdvancementTracker tracker = targetPlayer.getAdvancementTracker();
        ServerAdvancementLoader advancementLoader = server.getAdvancementLoader();
        Collection<AdvancementEntry> allAdvancements = advancementLoader.getAdvancements();

        List<AdvancementData> results = new ArrayList<>();

        for (AdvancementEntry entry : allAdvancements) {
            String advancementId = entry.id().toString();

            // 1. レシピ進捗を先にスキップ（最も数が多い）
            if (isRecipeAdvancement(advancementId)) {
                continue;
            }

            // 2. 表示がない進捗をスキップ（非表示進捗）
            Optional<AdvancementDisplay> display = entry.value().display();
            if (display.isEmpty()) {
                continue;
            }

            // 3. カテゴリーフィルター
            String advCategory = getCategory(advancementId);
            if (advCategory == null) {
                continue;
            }
            if (!"all".equals(category) && !category.equals(advCategory)) {
                continue;
            }

            // 4. 進捗状況を軽量に取得
            AdvancementProgress progress = tracker.getProgress(entry);
            boolean done = progress != null && progress.isDone();

            int obtainedCount = 0;
            int totalCount = 0;
            float progressPercent = 0.0f;

            if (progress != null) {
                for (String ignored : progress.getObtainedCriteria()) {
                    obtainedCount++;
                }
                totalCount = obtainedCount;
                for (String ignored : progress.getUnobtainedCriteria()) {
                    totalCount++;
                }
                progressPercent = progress.getProgressBarPercentage();
            }

            // 5. 表示テキストを取得
            AdvancementDisplay d = display.get();
            String title = d.getTitle().getString();
            String description = d.getDescription().getString();
            String frame = d.getFrame().toString();

            results.add(new AdvancementData(
                advancementId, advCategory, title, description, frame,
                done, progressPercent, obtainedCount, totalCount
            ));
        }

        return new CollectedData(resolvedPlayerName, results, null);
    }

    /**
     * HTTPスレッドで実行: 収集済みデータからJSON文字列を生成
     */
    private String buildJsonResponse(CollectedData collected) throws Exception {
        JsonObject root = new JsonObject();

        if (collected.error() != null) {
            root.addProperty("error", collected.error());
            return gson.toJson(root);
        }

        root.addProperty("playerName", collected.playerName());
        JsonArray advancementsArray = new JsonArray();
        root.add("advancements", advancementsArray);

        for (AdvancementData adv : collected.advancements()) {
            JsonObject advNode = new JsonObject();
            advNode.addProperty("id", adv.id());
            advNode.addProperty("category", adv.category());
            advNode.addProperty("title", adv.title());
            advNode.addProperty("description", adv.description());
            advNode.addProperty("frame", adv.frame());
            advNode.addProperty("done", adv.done());
            advNode.addProperty("progress", adv.progress());
            advNode.addProperty("criteriaCompleted", adv.criteriaCompleted());
            advNode.addProperty("criteriaTotal", adv.criteriaTotal());
            advancementsArray.add(advNode);
        }

        return gson.toJson(root);
    }

    /**
     * レシピ進捗かどうかを判定（バニラ・データパック両方）
     */
    private boolean isRecipeAdvancement(String advancementId) {
        int colonIndex = advancementId.indexOf(':');
        if (colonIndex < 0) return false;
        String path = advancementId.substring(colonIndex + 1);
        return path.startsWith("recipes/");
    }

    /**
     * 進捗IDからカテゴリーを動的に抽出
     */
    private String getCategory(String advancementId) {
        int colonIndex = advancementId.indexOf(':');
        if (colonIndex < 0) return null;

        String namespace = advancementId.substring(0, colonIndex);
        String path = advancementId.substring(colonIndex + 1);

        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return namespace + ":" + path.substring(0, slashIndex);
        } else {
            return namespace;
        }
    }

    /**
     * URIからクエリパラメータを解析
     */
    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }

    /**
     * エラーレスポンスを送信
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        try {
            JsonObject errorNode = new JsonObject();
            errorNode.addProperty("error", message);
            byte[] responseBytes = gson.toJson(errorNode).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        } catch (Exception e) {
            LOGGER.error("エラーレスポンスの送信に失敗", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
