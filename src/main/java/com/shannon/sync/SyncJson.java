package com.shannon.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.nio.charset.StandardCharsets;

/**
 * The one JSON format shared by the network payloads and the HTTP endpoints.
 *
 * <p>Unknown fields are ignored and missing ones keep their defaults, so either side can add a
 * field without breaking the other.
 */
public final class SyncJson {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SyncJson() {
    }

    public static byte[] encode(Object value) {
        return GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    /** Decodes {@code bytes}, or returns {@code null} when they are not valid JSON for {@code type}. */
    public static <T> T decode(byte[] bytes, Class<T> type) {
        try {
            return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
        } catch (JsonParseException e) {
            return null;
        }
    }
}
