package com.shannon.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.shannon.ShannonUIMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** The player's display preferences, kept in {@code config/shannonuimod-client.json}. */
public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("shannonuimod-client.json");

    /** Where the status card sits. */
    public enum Corner {
        TOP_LEFT, TOP_RIGHT;

        public Corner next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public boolean showCard = true;
    public Corner cardCorner = Corner.TOP_LEFT;
    public boolean showSpeech = true;
    /** How long the bot's latest words stay under the card. */
    public int speechSeconds = 8;
    public boolean showToasts = true;

    public static ClientConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                ClientConfig config = GSON.fromJson(reader, ClientConfig.class);
                if (config != null) {
                    if (config.cardCorner == null) {
                        config.cardCorner = Corner.TOP_LEFT;
                    }
                    config.speechSeconds = Math.max(2, Math.min(30, config.speechSeconds));
                    return config;
                }
            } catch (IOException | JsonParseException e) {
                ShannonUIMod.LOGGER.warn("Could not read {}; using defaults", PATH, e);
            }
        }
        return new ClientConfig();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ShannonUIMod.LOGGER.warn("Could not save {}", PATH, e);
        }
    }
}
