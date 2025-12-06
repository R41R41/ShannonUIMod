package com.shannon.http.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shannon.config.ModConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * タスク削除エンドポイント
 * バックエンドにリクエストを転送
 */
public class TaskDeleteEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDeleteEndpoint.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            LOGGER.info("📤 Task delete request: {}", requestBody);

            // バックエンドに転送
            String backendUrl = ModConfig.BACKEND_BASE_URL + ModConfig.ENDPOINT_TASK_DELETE;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.info("📥 Backend response: {} - {}", response.statusCode(), response.body());
            sendResponse(exchange, response.statusCode(), response.body());

        } catch (Exception e) {
            LOGGER.error("Task delete error: {}", e.getMessage());
            sendResponse(exchange, 500, "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
