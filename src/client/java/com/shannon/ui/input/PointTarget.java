package com.shannon.ui.input;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * The block or creature the player is looking at, found farther than vanilla's reach so the
 * player can point at things across a field.
 */
public record PointTarget(Kind kind, Text name, String id, BlockPos pos, ItemStack icon, boolean hostile) {
    /** How far the player can point. */
    public static final double RANGE = 64;

    public enum Kind {
        BLOCK, ENTITY
    }

    /** What the player looks at, or {@code null} when it is only sky. */
    public static PointTarget find(MinecraftClient client, float tickProgress) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return null;
        }
        HitResult blockHit = camera.raycast(RANGE, tickProgress, false);
        Vec3d eye = camera.getCameraPosVec(tickProgress);
        Vec3d look = camera.getRotationVec(tickProgress);
        double reach = blockHit.getType() == HitResult.Type.MISS ? RANGE : blockHit.getPos().distanceTo(eye);
        Vec3d end = eye.add(look.multiply(reach));
        Box box = camera.getBoundingBox().stretch(look.multiply(reach)).expand(1);
        EntityHitResult entityHit = ProjectileUtil.raycast(camera, eye, end, box,
                entity -> !entity.isSpectator() && entity.canHit() && entity != client.player, reach * reach);
        if (entityHit != null) {
            Entity entity = entityHit.getEntity();
            return new PointTarget(Kind.ENTITY, entity.getName(), Registries.ENTITY_TYPE.getId(entity.getType()).toString(),
                    entity.getBlockPos(), ItemStack.EMPTY, entity instanceof Monster);
        }
        if (blockHit instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.world.getBlockState(hit.getBlockPos());
            if (state.isAir()) {
                return null;
            }
            return new PointTarget(Kind.BLOCK, state.getBlock().getName(), Registries.BLOCK.getId(state.getBlock()).toString(),
                    hit.getBlockPos(), new ItemStack(state.getBlock().asItem()), false);
        }
        return null;
    }

    /** {@code x, y, z} as the bot reads coordinates. */
    public String coordinates() {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
