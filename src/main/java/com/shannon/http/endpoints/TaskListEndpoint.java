package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.TaskListStatePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * タスクリスト状態を受信するエンドポイント（POST）
 */
public class TaskListEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskListEndpoint.class);
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> data = GSON.fromJson(requestBody, Map.class);

            TaskListStatePacket.TaskListState state = new TaskListStatePacket.TaskListState();
            state.tasks = new ArrayList<>();

            // タスクリストをパース
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) data.get("tasks");
            if (tasks != null) {
                for (Map<String, Object> taskData : tasks) {
                    TaskListStatePacket.TaskListState.TaskInfo task = new TaskListStatePacket.TaskListState.TaskInfo();
                    task.id = (String) taskData.get("id");
                    task.goal = (String) taskData.get("goal");
                    task.status = (String) taskData.get("status");
                    Object createdAt = taskData.get("createdAt");
                    task.createdAt = createdAt != null ? ((Number) createdAt).longValue() : 0;
                    state.tasks.add(task);
                }
            }

            // 緊急タスクをパース
            Map<String, Object> emergencyData = (Map<String, Object>) data.get("emergencyTask");
            if (emergencyData != null) {
                state.emergencyTask = new TaskListStatePacket.TaskListState.EmergencyTaskInfo();
                state.emergencyTask.id = (String) emergencyData.get("id");
                state.emergencyTask.goal = (String) emergencyData.get("goal");
                Object createdAt = emergencyData.get("createdAt");
                state.emergencyTask.createdAt = createdAt != null ? ((Number) createdAt).longValue() : 0;
            }

            state.currentTaskId = (String) data.get("currentTaskId");

            // StateManagerを更新
            ShannonUIMod.getStateManager().updateTaskListState(state);

            LOGGER.info("📥 Task list state received: {} tasks, emergencyTask={}",
                    state.tasks.size(),
                    state.emergencyTask != null ? state.emergencyTask.goal : "null");
            sendResponse(exchange, 200, "{\"success\": true}");

        } catch (Exception e) {
            LOGGER.error("Error parsing task list state: {}", e.getMessage());
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
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
