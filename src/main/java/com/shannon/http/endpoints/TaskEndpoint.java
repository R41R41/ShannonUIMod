package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.TaskTreeState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * /task エンドポイント
 * タスクツリーの状態を受信
 */
public class TaskEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskEndpoint.class);
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
            TaskTreeState newState = gson.fromJson(json, TaskTreeState.class);
            LOGGER.info("受信: task goal={}, status={}, subTasks={}",
                    newState.goal,
                    newState.status,
                    newState.hierarchicalSubTasks != null ? newState.hierarchicalSubTasks.size() : 0);
            LOGGER.debug("受信したJSON (task): {}", json);

            // StateManager経由で更新
            ShannonUIMod.getStateManager().updateTaskTreeState(newState);

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("TaskEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
