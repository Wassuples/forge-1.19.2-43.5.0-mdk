package com.wassuples.demonslayer.mixin.toon;

import com.wassuples.demonslayer.client.toon.ToonTunnelClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fake Tunnel, client side: the painted tunnel's blocks are hidden (air) on every client so people can see
 * through it, but for everyone except the caster they must still collide like the real wall the server has.
 * Without this, other players' clients would let them walk in and the server would rubber-band them out.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ToonTunnelCollisionMixin {
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void demonslayer$solidForNonOwners(BlockGetter level, BlockPos pos, CollisionContext context,
                                               CallbackInfoReturnable<VoxelShape> cir) {
        if (!ToonTunnelClient.hasActiveTunnels()) return; // hot path: one static read when no tunnel exists
        VoxelShape forced = ToonTunnelClient.collisionOverride(pos, context);
        if (forced != null) {
            cir.setReturnValue(forced);
        }
    }
}
