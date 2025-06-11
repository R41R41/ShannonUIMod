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
    @Inject(method = "markDirty", at = @At("HEAD"))
    private void onMarkDirty(CallbackInfo ci) {
        send();
    }

    @Inject(method = "removeStack", at = @At("HEAD"))
    private void onRemoveStack(CallbackInfoReturnable<ItemStack> cir) {
        send();
    }

    @Inject(method = "setStack", at = @At("HEAD"))
    private void onSetStack(CallbackInfo ci) {
        send();
    }

    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void onSetSelectedSlot(int slot, CallbackInfo ci) {
        send();
    }

    private void send() {
        // 0.1秒だけ待機
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PlayerInventory inv = (PlayerInventory) (Object) this;
        if (inv.player != null && inv.player.getName().getString().contains(ShannonUIMod.TARGET_PLAYER_NAME)) {
            ShannonUIMod.sendInventoryStateOfSh4nnonToAll();
        }
    }
}
