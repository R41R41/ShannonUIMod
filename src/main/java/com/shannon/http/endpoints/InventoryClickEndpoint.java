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
 * /inventory_click エンドポイント
 * インベントリアイテムのクリックイベントを受信
 */
public class InventoryClickEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryClickEndpoint.class);
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
            LOGGER.info("受信したJSON (inventory_click): " + json);

            // 将来的にクリックイベントのハンドリングを実装
            // 現在は受信のみ

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("InventoryClickEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
