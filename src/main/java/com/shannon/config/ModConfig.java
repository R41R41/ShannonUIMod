package com.shannon.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side settings, read once from {@code config/shannonuimod.json}.
 *
 * <p>Every key is optional:
 * <pre>{@code
 * {
 *   "backendHost": "localhost",
 *   "backendPort": 8092,
 *   "backendToken": "the MINEBOT_API_TOKEN of the bot backend",
 *   "httpServerPort": 8081,
 *   "botPlayerName": "I_am_Shannon"
 * }
 * }</pre>
 * The token can also come from the {@code MINEBOT_API_TOKEN} environment variable.
 */
public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfig.class);
    private static final JsonObject FILE = readConfigFile();

    // ===== Bot backend =====

    public static final String BACKEND_HOST = string("backendHost", "localhost");
    public static final int BACKEND_PORT = integer("backendPort", 8092);
    public static final String BACKEND_BASE_URL = "http://" + BACKEND_HOST + ":" + BACKEND_PORT;
    /** Bearer token the backend requires; empty when none is configured. */
    public static final String BACKEND_TOKEN = string("backendToken",
            System.getenv().getOrDefault("MINEBOT_API_TOKEN", ""));

    // ===== HTTP servers that receive the backend's pushes =====

    public static final int HTTP_SERVER_PORT = integer("httpServerPort", 8081);
    /** Address the push server listens on. Loopback by default, since the backend runs on the same machine. */
    public static final String HTTP_SERVER_BIND_ADDRESS = string("httpServerBindAddress", "127.0.0.1");
    public static final int CLIENT_HTTP_SERVER_PORT = integer("clientHttpServerPort", 8083);
    public static final int HTTP_THREAD_POOL_SIZE = 4;
    public static final int CONNECTION_TIMEOUT_MS = 5_000;
    public static final int READ_TIMEOUT_MS = 10_000;

    // ===== Backend endpoints =====

    public static final String ENDPOINT_THROW_ITEM = "/throw_item";
    public static final String ENDPOINT_SKILL_SWITCH = "/constant_skill_switch";
    public static final String ENDPOINT_CHAT_MESSAGE = "/chat_message";
    public static final String ENDPOINT_REACTION_SETTING_UPDATE = "/reaction_setting_update";
    public static final String ENDPOINT_REACTION_SETTINGS_RESET = "/reaction_settings_reset";
    public static final String ENDPOINT_TASK_DELETE = "/task_delete";
    public static final String ENDPOINT_TASK_PRIORITIZE = "/task_prioritize";
    public static final String ENDPOINT_TASK_CONTINUE = "/task_continue";
    public static final String ENDPOINT_TASK_LIST = "/task_list";
    public static final String ENDPOINT_VOICE_MODE = "/voice_mode";
    public static final String ENDPOINT_VOICE_PTT = "/voice_ptt";

    // ===== The bot =====

    /** Exact player name of the bot on the server. */
    public static final String TARGET_PLAYER_NAME = string("botPlayerName", "I_am_Shannon");
    /** The backend id of the always-on skill that makes the bot follow the nearest player. */
    public static final String FOLLOW_SKILL_NAME = "auto-follow";

    // ===== Logging =====

    public static final boolean DEBUG_MODE = bool("debug", false);
    public static final boolean LOG_PACKETS = DEBUG_MODE;
    public static final boolean LOG_HTTP = DEBUG_MODE;

    private ModConfig() {
    }

    public static String buildBackendUrl(String endpoint) {
        return BACKEND_BASE_URL + endpoint;
    }

    public static void logConfiguration() {
        LOGGER.info("ShannonUIMod: backend={}, token={}, httpServerPort={}, bot={}",
                BACKEND_BASE_URL, BACKEND_TOKEN.isEmpty() ? "none" : "set", HTTP_SERVER_PORT, TARGET_PLAYER_NAME);
    }

    private static JsonObject readConfigFile() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("shannonuimod.json");
        if (!Files.exists(path)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults", path, e);
            return new JsonObject();
        }
    }

    private static String string(String key, String fallback) {
        try {
            return FILE.has(key) ? FILE.get(key).getAsString() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int integer(String key, int fallback) {
        try {
            return FILE.has(key) ? FILE.get(key).getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static boolean bool(String key, boolean fallback) {
        try {
            return FILE.has(key) ? FILE.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
