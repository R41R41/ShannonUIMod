package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.DetailedLogsState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * /task_logs エンドポイント
 * 詳細ログを受信
 */
public class TaskLogsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskLogsEndpoint.class);
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject rootNode = JsonParser.parseString(json).getAsJsonObject();
            JsonElement goalElement = rootNode.get("goal");
            JsonElement logsArrayElement = rootNode.get("logs");
            LOGGER.info("受信: task_logs goal={}, logs={}件",
                    goalElement != null && !goalElement.isJsonNull() ? goalElement.getAsString() : "?",
                    logsArrayElement != null && logsArrayElement.isJsonArray() ? logsArrayElement.getAsJsonArray().size() : 0);
            LOGGER.debug("受信したJSON (task_logs): {}", json);

            DetailedLogsState newLogsState = new DetailedLogsState();
            if (logsArrayElement != null && logsArrayElement.isJsonArray()) {
                JsonArray logsArray = logsArrayElement.getAsJsonArray();
                for (JsonElement logElement : logsArray) {
                    JsonObject logNode = logElement.getAsJsonObject();
                    DetailedLogsState.LogEntry log = new DetailedLogsState.LogEntry();
                    log.timestamp = getAsString(logNode, "timestamp", "");
                    log.phase = getAsString(logNode, "phase", "");
                    log.level = getAsString(logNode, "level", "");
                    log.source = getAsString(logNode, "source", "");
                    log.content = getAsString(logNode, "content", "");

                    JsonElement metadataElement = logNode.get("metadata");
                    if (metadataElement != null && !metadataElement.isJsonNull() && metadataElement.isJsonObject()) {
                        JsonObject metadataNode = metadataElement.getAsJsonObject();
                        log.metadata = new DetailedLogsState.LogEntry.LogMetadata();
                        if (metadataNode.has("skillName")) {
                            log.metadata.skillName = getAsString(metadataNode, "skillName", "");
                        }
                        if (metadataNode.has("toolName")) {
                            log.metadata.toolName = getAsString(metadataNode, "toolName", "");
                        }
                        if (metadataNode.has("parameters")) {
                            log.metadata.parameters = metadataNode.get("parameters").toString();
                        }
                        if (metadataNode.has("result")) {
                            log.metadata.result = metadataNode.get("result").toString();
                        }
                        if (metadataNode.has("duration") && !metadataNode.get("duration").isJsonNull()) {
                            log.metadata.duration = metadataNode.get("duration").getAsInt();
                        }
                        if (metadataNode.has("error")) {
                            log.metadata.error = getAsString(metadataNode, "error", "");
                        }
                    }

                    newLogsState.logs.add(log);
                }
            }

            // StateManager経由で更新
            ShannonUIMod.getStateManager().updateLogsState(newLogsState);

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("TaskLogsEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }

    private String getAsString(JsonObject obj, String key, String defaultValue) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return defaultValue;
        return el.getAsString();
    }
}
