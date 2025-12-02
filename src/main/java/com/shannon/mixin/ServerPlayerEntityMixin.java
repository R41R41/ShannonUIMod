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
        // プレイヤー参加時に全ての状態を送信
        ShannonUIMod.sendInventoryStateOfSh4nnonToAll();
        ShannonUIMod.sendPlayerStatusToAll();
        // TaskTreeとSkillsはStateManagerが自動でブロードキャスト済み
    }
}
