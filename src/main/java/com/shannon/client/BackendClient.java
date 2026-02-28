package com.shannon.client;

import com.google.gson.Gson;
import com.shannon.config.ModConfig;
import com.shannon.error.ModErrorHandler;
import com.shannon.error.exceptions.BackendCommunicationException;
import com.shannon.error.exceptions.JsonSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Backend（Minebot）との通信を抽象化するクラス
 * HTTP通信の重複コードを削減し、エラーハンドリングを統一
 */
public class BackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendClient.class);
    private static final Gson GSON = new Gson();

    /**
     * BackendにPOSTリクエストを送信（JSON文字列）
     * 
     * @param endpoint エンドポイント (例: "/throw_item")
     * @param jsonBody JSON文字列
     */
    public static void post(String endpoint, String jsonBody) {
        post(endpoint, jsonBody, null);
    }

    /**
     * BackendにPOSTリクエストを送信（コールバック付き）
     * 
     * @param endpoint エンドポイント
     * @param jsonBody JSON文字列
     * @param callback レスポンスコードを受け取るコールバック
     */
    public static void post(String endpoint, String jsonBody, Consumer<Integer> callback) {
        try {
            String url = ModConfig.buildBackendUrl(endpoint);
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

            // リクエスト設定
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(ModConfig.CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(ModConfig.READ_TIMEOUT_MS);

            // ボディ書き込み
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // レスポンス確認
            int responseCode = conn.getResponseCode();

            if (ModConfig.LOG_HTTP) {
                LOGGER.info("POST {} response: {}", endpoint, responseCode);
            }

            if (callback != null) {
                callback.accept(responseCode);
            }

            conn.disconnect();

        } catch (Exception e) {
            LOGGER.error("POST {} 送信失敗: {}", endpoint, e.getMessage());
            ModErrorHandler.handle(new BackendCommunicationException(endpoint, e));
        }
    }

    /**
     * BackendにPOSTリクエストを送信（Javaオブジェクト）
     * 
     * @param endpoint エンドポイント
     * @param data     送信するオブジェクト（自動的にJSONに変換）
     */
    public static void postJson(String endpoint, Object data) {
        postJson(endpoint, data, null);
    }

    /**
     * BackendにPOSTリクエストを送信（Javaオブジェクト、コールバック付き）
     * 
     * @param endpoint エンドポイント
     * @param data     送信するオブジェクト
     * @param callback レスポンスコードを受け取るコールバック
     */
    public static void postJson(String endpoint, Object data, Consumer<Integer> callback) {
        try {
            String json = GSON.toJson(data);

            if (ModConfig.DEBUG_MODE) {
                LOGGER.debug("POST {} body: {}", endpoint, json);
            }

            post(endpoint, json, callback);
        } catch (Exception e) {
            LOGGER.error("JSON変換失敗: {}", e.getMessage());
            ModErrorHandler.handle(new JsonSerializationException("serializing request", e));
        }
    }

    /**
     * BackendにPOSTリクエストを送信し、レスポンスボディも取得する
     *
     * @param endpoint エンドポイント
     * @param jsonBody JSON文字列
     * @param callback (レスポンスコード, レスポンスボディ) を受け取るコールバック
     */
    public static void postWithBody(String endpoint, String jsonBody, BiConsumer<Integer, String> callback) {
        try {
            String url = ModConfig.buildBackendUrl(endpoint);
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(ModConfig.CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(ModConfig.READ_TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String body = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            } catch (Exception ignored) {
            }

            if (ModConfig.LOG_HTTP) {
                LOGGER.info("POST {} response: {} body: {}", endpoint, responseCode, body);
            }

            if (callback != null) {
                callback.accept(responseCode, body);
            }

            conn.disconnect();

        } catch (Exception e) {
            LOGGER.error("POST {} 送信失敗: {}", endpoint, e.getMessage());
            if (callback != null) {
                callback.accept(-1, e.getMessage());
            }
            ModErrorHandler.handle(new BackendCommunicationException(endpoint, e));
        }
    }

    /**
     * 接続テスト
     * 
     * @return Backendに接続可能かどうか
     */
    public static boolean testConnection() {
        try {
            String url = ModConfig.BACKEND_BASE_URL + "/";
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(ModConfig.CONNECTION_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            LOGGER.info("Backend接続テスト: {}", responseCode >= 200 && responseCode < 500 ? "成功" : "失敗");
            return responseCode >= 200 && responseCode < 500;
        } catch (Exception e) {
            LOGGER.error("Backend接続テスト失敗: {}", e.getMessage());
            return false;
        }
    }

    private BackendClient() {
        // ユーティリティクラスなのでインスタンス化を防ぐ
    }
}
