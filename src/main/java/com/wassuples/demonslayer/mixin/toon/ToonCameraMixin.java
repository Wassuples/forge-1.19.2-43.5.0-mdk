package com.wassuples.demonslayer.mixin.toon;

import com.wassuples.demonslayer.client.toon.ToonCameraAnimator;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Toon Force knock-down animations carry the first-person camera with the body. */
@Mixin(Camera.class)
public abstract class ToonCameraMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;

    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void demonslayer$toonKnockdownCamera(BlockGetter level, Entity entity, boolean detached,
                                                 boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        float eye = Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight);
        Vec3 adjusted = ToonCameraAnimator.adjustCameraPosition(entity, detached, partialTick, eye);
        if (adjusted != null) {
            this.setPosition(adjusted);
        }
    }
}
