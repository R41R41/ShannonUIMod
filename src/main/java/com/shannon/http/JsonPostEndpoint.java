package com.shannon.http;

import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Base for the endpoints the bot backend pushes JSON to.
 *
 * <p>Subclasses only turn the body into state; method checks, size limits, responses and error
 * handling live here.
 */
public abstract class JsonPostEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPostEndpoint.class);
    /** Larger bodies are refused; the biggest real push is a few hundred kilobytes. */
    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;

    /** Applies one pushed body. Throw {@link JsonParseException} or {@link IllegalArgumentException} for bad input. */
    protected abstract void accept(String body);

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"success\":false,\"error\":\"method not allowed\"}");
                return;
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                respond(exchange, 413, "{\"success\":false,\"error\":\"body too large\"}");
                return;
            }
            try {
                accept(new String(bytes, StandardCharsets.UTF_8));
                respond(exchange, 200, "{\"success\":true}");
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException e) {
                LOGGER.warn("{} refused a body: {}", getClass().getSimpleName(), e.toString());
                respond(exchange, 400, "{\"success\":false,\"error\":\"bad request\"}");
            } catch (RuntimeException e) {
                LOGGER.error("{} failed", getClass().getSimpleName(), e);
                respond(exchange, 500, "{\"success\":false,\"error\":\"internal error\"}");
            }
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
