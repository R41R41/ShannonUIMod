package com.shannon.server;

import com.shannon.ShannonUIMod;
import com.shannon.config.ModConfig;
import com.shannon.model.InventoryState;
import com.shannon.model.JourneyState;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Notes when the bot first reaches each milestone on the way to the Ender Dragon, and its deaths.
 *
 * <p>The milestones are read from what the bot carries and where it is, as the bot backend measures
 * them, so the mod needs nothing new from the backend. The record belongs to the world: it is kept in
 * the world's save folder, survives restarts, and a new world starts a new one.
 */
public final class JourneyTracker {
    private static final String FILE = "shannonuimod-journey.json";
    private static final Identifier KILL_DRAGON = Identifier.of("minecraft", "end/kill_dragon");

    private final StateManager states;
    private JourneyState journey = new JourneyState();
    private Path file;

    public JourneyTracker(StateManager states) {
        this.states = states;
    }

    /** Reads this world's record, or starts an empty one. */
    public void load(MinecraftServer server) {
        file = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(FILE);
        journey = new JourneyState();
        if (Files.isRegularFile(file)) {
            try {
                JourneyState read = SyncJson.GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JourneyState.class);
                if (read != null) {
                    journey = read;
                    if (journey.marks == null) {
                        journey.marks = new ArrayList<>();
                    }
                }
            } catch (IOException | RuntimeException e) {
                ShannonUIMod.LOGGER.warn("Could not read {}; starting a new journey record", file, e);
            }
        }
        states.publish(StateChannels.JOURNEY, journey);
    }

    public void unload() {
        file = null;
    }

    /** Looks at the bot; called from the observer's regular look with the inventory it just read. */
    public void observe(MinecraftServer server, ServerPlayerEntity bot, InventoryState inventory) {
        if (file == null) {
            return;
        }
        long now = server.getOverworld().getTime();
        boolean changed = false;
        if (journey.startTick < 0) {
            journey.startTick = now;
            changed = true;
        }
        List<String> carried = carried(inventory);
        String dimension = bot.getEntityWorld().getRegistryKey().getValue().toString();
        for (JourneyState.Milestone milestone : JourneyState.MILESTONES) {
            if (journey.reachedAt(milestone.id()) < 0 && reached(milestone.id(), carried, dimension, server, bot)) {
                journey.marks.add(new JourneyState.Mark(milestone.id(), now));
                changed = true;
            }
        }
        if (changed) {
            commit();
        }
    }

    /** Counts a death of the bot. Registered on Fabric's after-death event, which fires for every living entity. */
    public void onDeath(LivingEntity entity) {
        if (file == null || !ModConfig.TARGET_PLAYER_NAME.equals(entity.getName().getString())
                || !(entity instanceof ServerPlayerEntity bot)) {
            return;
        }
        journey.deaths++;
        journey.lastDeathTick = bot.getEntityWorld().getServer().getOverworld().getTime();
        commit();
    }

    private void commit() {
        states.publish(StateChannels.JOURNEY, journey);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, SyncJson.GSON.toJson(journey), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ShannonUIMod.LOGGER.warn("Could not save {}", file, e);
        }
    }

    private static List<String> carried(InventoryState inventory) {
        List<String> items = new ArrayList<>();
        List<InventoryState.Stack> all = new ArrayList<>(inventory.main);
        all.add(inventory.head);
        all.add(inventory.chest);
        all.add(inventory.legs);
        all.add(inventory.feet);
        all.add(inventory.offHand);
        for (InventoryState.Stack stack : all) {
            if (stack != null && stack.item != null) {
                items.add(stack.item.startsWith("minecraft:") ? stack.item.substring(10) : stack.item);
            }
        }
        return items;
    }

    private static boolean reached(String id, List<String> carried, String dimension, MinecraftServer server,
                                   ServerPlayerEntity bot) {
        return switch (id) {
            case "bed" -> any(carried, item -> item.endsWith("_bed"));
            case "bucket" -> any(carried, item -> item.matches("(?:water_|lava_|milk_|powder_snow_)?bucket"));
            case "iron_armor" -> any(carried, item -> item.matches("iron_(?:helmet|chestplate|leggings|boots)"));
            case "nether_portal" -> dimension.equals("minecraft:the_nether");
            case "the_end" -> dimension.equals("minecraft:the_end");
            case "dragon" -> killedDragon(server, bot);
            default -> carried.contains(id);
        };
    }

    private static boolean any(List<String> carried, Predicate<String> test) {
        for (String item : carried) {
            if (test.test(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean killedDragon(MinecraftServer server, ServerPlayerEntity bot) {
        AdvancementEntry entry = server.getAdvancementLoader().get(KILL_DRAGON);
        return entry != null && bot.getAdvancementTracker().getProgress(entry).isDone();
    }
}
