package com.shannon.client;

import com.shannon.config.ModConfig;
import com.shannon.sync.SyncJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the bot backend over HTTP.
 *
 * <p>Every call runs on one background thread and returns a future, so a slow or absent backend
 * never stalls the server tick. Requests carry the configured bearer token.
 */
public final class BackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendClient.class);

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ShannonUIMod-Backend");
        thread.setDaemon(true);
        return thread;
    });

    /** The outcome of one request. {@code status} is -1 when the backend could not be reached. */
    public record Response(int status, String body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private BackendClient() {
    }

    /** Posts {@code body} as JSON to {@code endpoint}. The future never completes exceptionally. */
    public static CompletableFuture<Response> post(String endpoint, Object body) {
        String json = body instanceof String text ? text : SyncJson.GSON.toJson(body);
        return CompletableFuture.supplyAsync(() -> send("POST", endpoint, json), EXECUTOR);
    }

    /** Fire and forget: posts and logs a failure. */
    public static void postJson(String endpoint, Object body) {
        post(endpoint, body).thenAccept(response -> {
            if (!response.ok()) {
                LOGGER.warn("POST {} failed: {} {}", endpoint, response.status(), abbreviate(response.body()));
            }
        });
    }

    public static CompletableFuture<Response> get(String endpoint) {
        return CompletableFuture.supplyAsync(() -> send("GET", endpoint, null), EXECUTOR);
    }

    private static Response send(String method, String endpoint, String json) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(ModConfig.buildBackendUrl(endpoint)).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(ModConfig.CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(ModConfig.READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (!ModConfig.BACKEND_TOKEN.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + ModConfig.BACKEND_TOKEN);
            }
            if (json != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            String body = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (ModConfig.LOG_HTTP) {
                LOGGER.info("{} {} -> {}", method, endpoint, status);
            }
            return new Response(status, body);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("{} {} could not reach the backend: {}", method, endpoint, e.toString());
            return new Response(-1, "");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String abbreviate(String text) {
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
