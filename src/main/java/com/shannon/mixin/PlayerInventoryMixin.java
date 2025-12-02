package com.shannon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.shannon.ShannonUIMod;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    // insertStack - アイテムをインベントリに挿入（拾った時など）
    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void onInsertStackReturn(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (inv.player != null && inv.player.getName().getString().contains(ShannonUIMod.TARGET_PLAYER_NAME)) {
            if (inv.player.getServer() != null && !inv.player.getWorld().isClient) {
                net.minecraft.server.network.ServerPlayerEntity serverPlayer = (net.minecraft.server.network.ServerPlayerEntity) inv.player;
                ShannonUIMod.sendInventoryStateToAll(serverPlayer);
            }
        }
    }

    // setStack - スロットにアイテムをセット（コマンドでクリアした時など）
    @Inject(method = "setStack", at = @At("RETURN"))
    private void onSetStackReturn(int slot, ItemStack stack, CallbackInfo ci) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (inv.player != null && inv.player.getName().getString().contains(ShannonUIMod.TARGET_PLAYER_NAME)) {
            if (inv.player.getServer() != null && !inv.player.getWorld().isClient) {
                net.minecraft.server.network.ServerPlayerEntity serverPlayer = (net.minecraft.server.network.ServerPlayerEntity) inv.player;
                ShannonUIMod.sendInventoryStateToAll(serverPlayer);
            }
        }
    }

    // removeStack - アイテムを削除（投げた時など）
    @Inject(method = "removeStack(II)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    private void onRemoveStackReturn(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (inv.player != null && inv.player.getName().getString().contains(ShannonUIMod.TARGET_PLAYER_NAME)) {
            if (inv.player.getServer() != null && !inv.player.getWorld().isClient) {
                net.minecraft.server.network.ServerPlayerEntity serverPlayer = (net.minecraft.server.network.ServerPlayerEntity) inv.player;
                ShannonUIMod.sendInventoryStateToAll(serverPlayer);
            }
        }
    }
}
