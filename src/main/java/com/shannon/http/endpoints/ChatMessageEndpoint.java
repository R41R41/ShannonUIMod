package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.shannon.ShannonUIMod;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * /chat_message エンドポイント
 * チャットメッセージの送信イベントを受信
 */
public class ChatMessageEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageEndpoint.class);
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
            LOGGER.info("受信したJSON (chat_message): " + json);

            // 将来的にメッセージ送信のハンドリングを実装
            // 現在は受信のみ

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("ChatMessageEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
