package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
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

@Mixin(Camera.class)
public abstract class GravityCameraMixin {

    @Shadow
    private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow private Entity entity;

    @Inject(
        method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V",
            ordinal = 0,          // target only the FIRST setPosition call (the eye anchor)
            shift = At.Shift.AFTER
        )
    )
    private void demonslayer$fixCameraAnchor(BlockGetter area, Entity entity,
                                             boolean thirdPerson, boolean thirdPersonReverse,
                                             float partialTick, CallbackInfo ci) {
        if (this.entity == null) return;

        this.entity.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = GravityUtil.getEffectiveGravity(this.entity);
            if (dir == GravityDirection.DOWN) return;

            float eyeH = Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight);
            Vec3 interpolatedAnchor = new Vec3(
                Mth.lerp(partialTick, this.entity.xo, this.entity.getX()),
                Mth.lerp(partialTick, this.entity.yo, this.entity.getY()),
                Mth.lerp(partialTick, this.entity.zo, this.entity.getZ())
            );

            Vec3 desiredEye = interpolatedAnchor.add(GravityUtil.getWorldUp(dir).scale(eyeH));
            this.setPosition(desiredEye.x, desiredEye.y, desiredEye.z);
        });
    }
}
