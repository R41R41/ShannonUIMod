package com.shannon.http.endpoints;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.ChatState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * /chat エンドポイント
 * チャット履歴を受信
 */
public class ChatEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatEndpoint.class);
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
            LOGGER.info("受信したJSON: " + json);

            List<ChatState.ChatMessage> messages = mapper.readValue(json,
                    new TypeReference<List<ChatState.ChatMessage>>() {
                    });

            ChatState newState = new ChatState();
            newState.messages = messages;

            // StateManager経由で更新
            ShannonUIMod.getStateManager().updateChatState(newState);

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("ChatEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
