package com.wassuples.demonslayer.mixin.toon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.client.toon.ToonShadowClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shadow Toss: whoever has thrown their shadow away doesn't cast one until they get it back. */
@Mixin(EntityRenderDispatcher.class)
public abstract class ToonShadowSuppressMixin {
    @Inject(method = "renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
            at = @At("HEAD"), cancellable = true)
    private static void demonslayer$hideTossedShadow(PoseStack pose, MultiBufferSource buffers, Entity entity, float weight,
                                                     float partialTick, LevelReader level, float size, CallbackInfo ci) {
        if (ToonShadowClient.isShadowTossed(entity)) {
            ci.cancel();
        }
    }
}
