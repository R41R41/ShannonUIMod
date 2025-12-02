package com.shannon.util;

import com.shannon.network.packet.InventoryState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.registry.Registries;

public class InventoryStateUtil {
    public static InventoryState createInventoryState(ServerPlayerEntity player) {
        InventoryState state = new InventoryState();

        // 1. まず部位ごとの装備をセット
        // 防具（部位ごとに分けて格納）
        ItemStack[] armorStacks = player.getInventory().armor.toArray(new ItemStack[0]);
        if (armorStacks.length == 4) {
            // feet (boots)
            if (!armorStacks[0].isEmpty()) {
                InventoryState.Item item = new InventoryState.Item();
                item.name = Registries.ITEM.getId(armorStacks[0].getItem()).toString();
                item.count = String.valueOf(armorStacks[0].getCount());
                item.displayName = armorStacks[0].getName().getString();
                state.feet = item;
            }
            // legs (leggings)
            if (!armorStacks[1].isEmpty()) {
                InventoryState.Item item = new InventoryState.Item();
                item.name = Registries.ITEM.getId(armorStacks[1].getItem()).toString();
                item.count = String.valueOf(armorStacks[1].getCount());
                item.displayName = armorStacks[1].getName().getString();
                state.legs = item;
            }
            // chest (chestplate)
            if (!armorStacks[2].isEmpty()) {
                InventoryState.Item item = new InventoryState.Item();
                item.name = Registries.ITEM.getId(armorStacks[2].getItem()).toString();
                item.count = String.valueOf(armorStacks[2].getCount());
                item.displayName = armorStacks[2].getName().getString();
                state.chest = item;
            }
            // head (helmet)
            if (!armorStacks[3].isEmpty()) {
                InventoryState.Item item = new InventoryState.Item();
                item.name = Registries.ITEM.getId(armorStacks[3].getItem()).toString();
                item.count = String.valueOf(armorStacks[3].getCount());
                item.displayName = armorStacks[3].getName().getString();
                state.head = item;
            }
        }

        // メインハンド
        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(mainHandStack.getItem()).toString();
            item.count = String.valueOf(mainHandStack.getCount());
            item.displayName = mainHandStack.getName().getString();
            state.mainHand = item;
        }

        // オフハンド
        ItemStack offHandStack = player.getOffHandStack();
        if (!offHandStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(offHandStack.getItem()).toString();
            item.count = String.valueOf(offHandStack.getCount());
            item.displayName = offHandStack.getName().getString();
            state.offHand = item;
        }

        // 2. 装備中のItemStackをリストアップ
        List<ItemStack> equipped = new ArrayList<>();
        if (state.mainHand != null)
            equipped.add(mainHandStack);
        if (state.offHand != null)
            equipped.add(offHandStack);
        if (state.head != null)
            equipped.add(armorStacks[3]);
        if (state.chest != null)
            equipped.add(armorStacks[2]);
        if (state.legs != null)
            equipped.add(armorStacks[1]);
        if (state.feet != null)
            equipped.add(armorStacks[0]);

        // 3. items生成
        state.items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty()) {
                InventoryState.Item item = new InventoryState.Item();
                item.name = Registries.ITEM.getId(stack.getItem()).toString();
                item.count = String.valueOf(stack.getCount());
                item.displayName = stack.getName().getString();
                state.items.add(item);
            }
        }

        // インベントリが満タンか
        state.isFull = true;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isEmpty()) {
                state.isFull = false;
                break;
            }
        }

        return state;
    }
}