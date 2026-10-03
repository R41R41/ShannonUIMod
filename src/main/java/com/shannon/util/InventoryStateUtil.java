package com.shannon.util;

import com.shannon.model.InventoryState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

/** Reads a player's inventory into an {@link InventoryState}. */
public final class InventoryStateUtil {
    /** Hotbar plus the three rows above it. */
    private static final int MAIN_SLOTS = 36;

    private InventoryStateUtil() {
    }

    public static InventoryState createInventoryState(ServerPlayerEntity player) {
        InventoryState state = new InventoryState();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < MAIN_SLOTS; slot++) {
            state.main.add(toStack(inventory.getStack(slot)));
        }
        state.selectedSlot = inventory.getSelectedSlot();
        state.head = toStack(player.getEquippedStack(EquipmentSlot.HEAD));
        state.chest = toStack(player.getEquippedStack(EquipmentSlot.CHEST));
        state.legs = toStack(player.getEquippedStack(EquipmentSlot.LEGS));
        state.feet = toStack(player.getEquippedStack(EquipmentSlot.FEET));
        state.offHand = toStack(player.getOffHandStack());
        return state;
    }

    public static String itemId(ItemStack stack) {
        return stack.isEmpty() ? null : Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static InventoryState.Stack toStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return new InventoryState.Stack(itemId(stack), stack.getCount());
    }
}
