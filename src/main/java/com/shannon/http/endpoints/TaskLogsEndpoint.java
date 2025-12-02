package com.shannon.http.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            InputStream is = exchange.getRequestBody();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LOGGER.info("受信したJSON (task_logs): " + json);

            JsonNode rootNode = mapper.readTree(json);
            JsonNode logsNode = rootNode.get("logs");

            DetailedLogsState newLogsState = new DetailedLogsState();
            if (logsNode != null && logsNode.isArray()) {
                for (JsonNode logNode : logsNode) {
                    DetailedLogsState.LogEntry log = new DetailedLogsState.LogEntry();
                    log.timestamp = logNode.get("timestamp").asText("");
                    log.phase = logNode.get("phase").asText("");
                    log.level = logNode.get("level").asText("");
                    log.source = logNode.get("source").asText("");
                    log.content = logNode.get("content").asText("");

                    JsonNode metadataNode = logNode.get("metadata");
                    if (metadataNode != null && !metadataNode.isNull()) {
                        log.metadata = new DetailedLogsState.LogEntry.LogMetadata();
                        if (metadataNode.has("skillName")) {
                            log.metadata.skillName = metadataNode.get("skillName").asText("");
                        }
                        if (metadataNode.has("toolName")) {
                            log.metadata.toolName = metadataNode.get("toolName").asText("");
                        }
                        if (metadataNode.has("parameters")) {
                            log.metadata.parameters = metadataNode.get("parameters").toString();
                        }
                        if (metadataNode.has("result")) {
                            log.metadata.result = metadataNode.get("result").toString();
                        }
                        if (metadataNode.has("duration")) {
                            log.metadata.duration = metadataNode.get("duration").asInt(-1);
                        }
                        if (metadataNode.has("error")) {
                            log.metadata.error = metadataNode.get("error").asText("");
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
}
