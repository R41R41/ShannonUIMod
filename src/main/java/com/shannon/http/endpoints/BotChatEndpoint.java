package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.ChatState;
import com.shannon.state.StateManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ボットのチャットを受け取るエンドポイント
 * POST /bot_chat
 * 
 * リクエストボディ:
 * {
 * "message": "こんにちは！"
 * }
 */
public class BotChatEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotChatEndpoint.class);
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORSヘッダー
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            String response = "{\"error\":\"Method not allowed\"}";
            exchange.sendResponseHeaders(405, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        try {
            // リクエストボディを読み取り
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // JSONをパース
            BotChatRequest request = GSON.fromJson(body, BotChatRequest.class);

            if (request.message == null || request.message.isEmpty()) {
                String response = "{\"success\":false,\"error\":\"Message is required\"}";
                exchange.sendResponseHeaders(400, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            // ChatStateに追加
            StateManager stateManager = ShannonUIMod.getStateManager();
            ChatState chatState = stateManager.getChatState();

            ChatState.ChatMessage chatMessage = new ChatState.ChatMessage();
            chatMessage.sender = "Shannon";
            chatMessage.message = request.message;
            chatMessage.timestamp = System.currentTimeMillis();
            chatState.messages.add(chatMessage);

            stateManager.updateChatState(chatState);

            LOGGER.info("💬 Bot chat received: {}", request.message);

            // 成功レスポンス
            String response = "{\"success\":true}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            LOGGER.error("Error processing bot chat: {}", e.getMessage());
            String response = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
            exchange.sendResponseHeaders(500, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // リクエストボディのクラス
    public static class BotChatRequest {
        public String message;
    }
}
