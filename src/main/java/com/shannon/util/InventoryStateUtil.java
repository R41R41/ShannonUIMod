package com.shannon.util;

import com.shannon.network.packet.InventoryState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.registry.Registries;

public class InventoryStateUtil {
    public static InventoryState createInventoryState(ServerPlayerEntity player) {
        InventoryState state = new InventoryState();

        // 1. まず部位ごとの装備をセット（EquipmentSlotベースのアクセス - 1.21.11対応）
        // feet (boots)
        ItemStack feetStack = player.getEquippedStack(EquipmentSlot.FEET);
        if (!feetStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(feetStack.getItem()).toString();
            item.count = String.valueOf(feetStack.getCount());
            item.displayName = feetStack.getName().getString();
            state.feet = item;
        }
        // legs (leggings)
        ItemStack legsStack = player.getEquippedStack(EquipmentSlot.LEGS);
        if (!legsStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(legsStack.getItem()).toString();
            item.count = String.valueOf(legsStack.getCount());
            item.displayName = legsStack.getName().getString();
            state.legs = item;
        }
        // chest (chestplate)
        ItemStack chestStack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chestStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(chestStack.getItem()).toString();
            item.count = String.valueOf(chestStack.getCount());
            item.displayName = chestStack.getName().getString();
            state.chest = item;
        }
        // head (helmet)
        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!headStack.isEmpty()) {
            InventoryState.Item item = new InventoryState.Item();
            item.name = Registries.ITEM.getId(headStack.getItem()).toString();
            item.count = String.valueOf(headStack.getCount());
            item.displayName = headStack.getName().getString();
            state.head = item;
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
            equipped.add(headStack);
        if (state.chest != null)
            equipped.add(chestStack);
        if (state.legs != null)
            equipped.add(legsStack);
        if (state.feet != null)
            equipped.add(feetStack);

        // 3. items生成（スロット0-35がメインインベントリ）
        state.items = new ArrayList<>();
        int mainSize = 36; // メインインベントリは36スロット
        for (int i = 0; i < mainSize; i++) {
            ItemStack stack = player.getInventory().getStack(i);
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
        for (int i = 0; i < mainSize; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                state.isFull = false;
                break;
            }
        }

        return state;
    }
}