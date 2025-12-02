package com.shannon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.shannon.ShannonUIMod;
import com.shannon.config.ModConfig;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

@Mixin(LivingEntity.class)
public class PlayerStatusMixin {
    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity player && player.getGameProfile() != null) {
            if (player.getName().getString().contains(ModConfig.TARGET_PLAYER_NAME)) {
                ShannonUIMod.sendPlayerStatusToAll();
            }
        }
    }

    @Inject(method = "setHealth", at = @At("TAIL"))
    private void onSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity player && player.getGameProfile() != null) {
            if (player.getName().getString().contains(ModConfig.TARGET_PLAYER_NAME)) {
                ShannonUIMod.sendPlayerStatusToAll();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity player && player.getGameProfile() != null) {
            if (player.getName().getString().contains(ModConfig.TARGET_PLAYER_NAME)) {
                int currentHunger = player.getHungerManager().getFoodLevel();
                if (currentHunger < 20) { // 空腹度が最大でない場合
                    ShannonUIMod.sendPlayerStatusToAll();
                }
            }
        }
    }
}