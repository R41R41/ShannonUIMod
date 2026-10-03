package com.shannon.server;

import com.shannon.config.ModConfig;
import com.shannon.model.BotVitals;
import com.shannon.model.InventoryState;
import com.shannon.state.StateManager;
import com.shannon.sync.StateChannels;
import com.shannon.sync.SyncJson;
import com.shannon.util.InventoryStateUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Watches the bot's player on the server and publishes its vitals and inventory when they change.
 *
 * <p>Polling a single player twice a second replaces mixins into entity and inventory code, so
 * nothing in vanilla classes is touched and other mods that change those classes are unaffected.
 */
public final class BotObserver {
    /** Ticks between two looks at the bot. */
    private static final int INTERVAL_TICKS = 10;

    private final StateManager states;
    private int ticks;
    private String lastVitals;
    private String lastInventory;
    private int hurtCount;
    private String hurtBy;
    private String hurtCause;

    public BotObserver(StateManager states) {
        this.states = states;
    }

    /** Called at the end of every server tick. */
    public void tick(MinecraftServer server) {
        if (++ticks < INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        ServerPlayerEntity bot = server.getPlayerManager().getPlayer(ModConfig.TARGET_PLAYER_NAME);
        BotVitals vitals = bot == null ? offline() : readVitals(bot);
        vitals.hurtCount = hurtCount;
        vitals.hurtBy = hurtBy;
        vitals.hurtCause = hurtCause;
        String vitalsJson = SyncJson.GSON.toJson(vitals);
        if (!vitalsJson.equals(lastVitals)) {
            lastVitals = vitalsJson;
            states.publish(StateChannels.VITALS, vitals);
        }
        if (bot != null) {
            InventoryState inventory = InventoryStateUtil.createInventoryState(bot);
            String inventoryJson = SyncJson.GSON.toJson(inventory);
            if (!inventoryJson.equals(lastInventory)) {
                lastInventory = inventoryJson;
                states.publish(StateChannels.INVENTORY, inventory);
            }
        }
    }

    /**
     * Notes that {@code entity} took damage. Registered on Fabric's after-damage event, which
     * fires for every living entity, so it returns at once for anything but the bot.
     */
    public void onDamage(LivingEntity entity, DamageSource source, float damageTaken) {
        if (damageTaken <= 0 || !ModConfig.TARGET_PLAYER_NAME.equals(entity.getName().getString())) {
            return;
        }
        Entity attacker = source.getAttacker();
        hurtCount++;
        hurtBy = attacker != null ? attacker.getType().getTranslationKey() : null;
        hurtCause = source.getType().msgId();
    }

    /** Forgets what was sent, so the next look publishes again. */
    public void reset() {
        lastVitals = null;
        lastInventory = null;
    }

    private static BotVitals offline() {
        BotVitals vitals = new BotVitals();
        vitals.name = ModConfig.TARGET_PLAYER_NAME;
        vitals.online = false;
        return vitals;
    }

    private static BotVitals readVitals(ServerPlayerEntity bot) {
        BotVitals vitals = new BotVitals();
        vitals.name = bot.getName().getString();
        vitals.online = true;
        vitals.health = round(bot.getHealth());
        vitals.maxHealth = round(bot.getMaxHealth());
        vitals.food = bot.getHungerManager().getFoodLevel();
        vitals.air = bot.getAir();
        vitals.maxAir = bot.getMaxAir();
        // Whole blocks are enough for the HUD and keep the bot from resending while it stands still.
        vitals.x = Math.floor(bot.getX());
        vitals.y = Math.floor(bot.getY());
        vitals.z = Math.floor(bot.getZ());
        ServerWorld world = bot.getEntityWorld();
        vitals.dimension = world.getRegistryKey().getValue().toString();
        vitals.biome = world.getBiome(bot.getBlockPos()).getKey()
                .map(key -> key.getValue().toString())
                .orElse(null);
        vitals.mainHand = InventoryStateUtil.itemId(bot.getMainHandStack());
        return vitals;
    }

    private static float round(float value) {
        return Math.round(value * 2f) / 2f;
    }
}
