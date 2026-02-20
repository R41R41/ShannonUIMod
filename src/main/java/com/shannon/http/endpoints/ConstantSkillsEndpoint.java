package com.shannon.http.endpoints;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shannon.ShannonUIMod;
import com.shannon.network.packet.ConstantSkillsState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * /constant_skills エンドポイント
 * コンスタントスキルの状態を受信
 */
public class ConstantSkillsEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantSkillsEndpoint.class);
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
            LOGGER.info("受信したJSON: " + json);

            Type listType = new TypeToken<List<ConstantSkillsState.ConstantSkill>>() {}.getType();
            List<ConstantSkillsState.ConstantSkill> skills = gson.fromJson(json, listType);

            ConstantSkillsState newState = new ConstantSkillsState();
            newState.skills = skills;

            // StateManager経由で更新
            ShannonUIMod.getStateManager().updateSkillsState(newState);

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            LOGGER.error("ConstantSkillsEndpointでエラーが発生しました", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
