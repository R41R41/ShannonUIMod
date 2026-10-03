package com.shannon.ui.hud;

import com.shannon.model.BotVitals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/** Where the bot is relative to the local player. */
public record BotLocator(double distance, float relativeYaw) {

    /**
     * Locates the bot: from its loaded entity when the client can see it, otherwise from the
     * position the server sent. Returns {@code null} when it is offline or in another dimension.
     */
    public static BotLocator locate(MinecraftClient client, String botName, BotVitals vitals) {
        if (client.player == null || client.world == null) {
            return null;
        }
        double x;
        double z;
        double y;
        PlayerEntity entity = findEntity(client, botName);
        if (entity != null) {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        } else {
            if (vitals == null || !vitals.online) {
                return null;
            }
            String here = client.world.getRegistryKey().getValue().toString();
            if (vitals.dimension != null && !vitals.dimension.equals(here)) {
                return null;
            }
            x = vitals.x + 0.5;
            y = vitals.y;
            z = vitals.z + 0.5;
        }
        double dx = x - client.player.getX();
        double dy = y - client.player.getY();
        double dz = z - client.player.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        // Minecraft yaw: 0 faces +Z and grows turning right.
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float relative = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        return new BotLocator(distance, relative);
    }

    /** The bot's player entity when the client has it loaded, or {@code null}. */
    static PlayerEntity findEntity(MinecraftClient client, String botName) {
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getName().getString().equals(botName)) {
                return player;
            }
        }
        return null;
    }
}
