package com.shannon.util;

import com.shannon.model.AdvancementsState;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Reads a player's advancements as the trees vanilla shows, one category per root.
 *
 * <p>Tabs added by data packs and other mods are included the same way as vanilla ones. Call on
 * the server thread.
 */
public final class AdvancementCollector {
    private AdvancementCollector() {
    }

    public static AdvancementsState collect(MinecraftServer server, String playerName) {
        AdvancementsState state = new AdvancementsState();
        state.updatedAt = System.currentTimeMillis();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player == null) {
            state.playerName = playerName;
            return state;
        }
        state.playerName = player.getName().getString();
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();

        for (PlacedAdvancement root : server.getAdvancementLoader().getManager().getRoots()) {
            Optional<AdvancementDisplay> rootDisplay = root.getAdvancement().display();
            if (rootDisplay.isEmpty()) {
                continue; // Recipe unlocks and other invisible trees.
            }
            AdvancementsState.Category category = new AdvancementsState.Category();
            category.rootId = root.getAdvancementEntry().id().toString();
            setTitle(rootDisplay.get().getTitle(), text -> category.title = text, key -> category.titleKey = key);
            category.icon = Registries.ITEM.getId(rootDisplay.get().getIcon().getItem()).toString();

            Deque<PlacedAdvancement> pending = new ArrayDeque<>();
            pending.add(root);
            while (!pending.isEmpty()) {
                PlacedAdvancement placed = pending.poll();
                for (PlacedAdvancement child : placed.getChildren()) {
                    pending.add(child);
                }
                AdvancementsState.Advancement advancement = read(placed, tracker);
                if (advancement == null) {
                    continue;
                }
                category.total++;
                if (advancement.done) {
                    category.completed++;
                }
                category.advancements.add(advancement);
            }
            state.categories.add(category);
        }
        return state;
    }

    private static AdvancementsState.Advancement read(PlacedAdvancement placed, PlayerAdvancementTracker tracker) {
        AdvancementEntry entry = placed.getAdvancementEntry();
        Optional<AdvancementDisplay> maybeDisplay = entry.value().display();
        if (maybeDisplay.isEmpty()) {
            return null;
        }
        AdvancementDisplay display = maybeDisplay.get();
        AdvancementProgress progress = tracker.getProgress(entry);
        boolean done = progress != null && progress.isDone();
        if (display.isHidden() && !done) {
            return null;
        }
        AdvancementsState.Advancement advancement = new AdvancementsState.Advancement();
        advancement.id = entry.id().toString();
        PlacedAdvancement parent = placed.getParent();
        advancement.parentId = parent != null ? parent.getAdvancementEntry().id().toString() : null;
        setTitle(display.getTitle(), text -> advancement.title = text, key -> advancement.titleKey = key);
        setTitle(display.getDescription(), text -> advancement.description = text,
                key -> advancement.descriptionKey = key);
        advancement.icon = Registries.ITEM.getId(display.getIcon().getItem()).toString();
        advancement.frame = display.getFrame().asString();
        advancement.done = done;
        advancement.x = display.getX();
        advancement.y = display.getY();
        if (progress != null && !done) {
            int obtained = 0;
            int total = 0;
            for (String ignored : progress.getObtainedCriteria()) {
                obtained++;
                total++;
            }
            for (String ignored : progress.getUnobtainedCriteria()) {
                total++;
            }
            advancement.progress = total > 1 ? obtained + "/" + total : "";
        }
        return advancement;
    }

    /**
     * Sends a vanilla translation key when there is one, so the client shows it in the player's
     * language, and the resolved text otherwise.
     */
    private static void setTitle(Text text, java.util.function.Consumer<String> plain,
                                 java.util.function.Consumer<String> key) {
        plain.accept(text.getString());
        if (text.getContent() instanceof TranslatableTextContent translatable
                && translatable.getKey().startsWith("advancements.")) {
            key.accept(translatable.getKey());
        }
    }
}
