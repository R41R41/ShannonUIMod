package com.shannon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.shannon.ShannonUIMod;

import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayerEntity.class)
class ServerPlayerEntityMixin {
    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void onSpawn(CallbackInfo ci) {
        ShannonUIMod.sendInventoryStateOfSh4nnonToAll();
        ShannonUIMod.sendTaskTreeStateToAllPlayers();
        ShannonUIMod.sendConstantSkillsStateToAllPlayers();
        ShannonUIMod.sendPlayerStatusToAll();
    }
}
