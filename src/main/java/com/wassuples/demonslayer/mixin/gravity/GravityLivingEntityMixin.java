package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class GravityLivingEntityMixin {

    @Shadow
    protected abstract float getJumpPower();

    @Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
    protected void onJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        entity.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() == GravityDirection.DOWN) return;

            // Avoid re-triggering jump impulse while a custom-gravity jump is still detaching.
            if (gravity.isSurfaceDetachActive()) {
                ci.cancel();
                return;
            }

            Vec3 down    = GravityUtil.getWorldDown(gravity.getGravity());
            Vec3 lateral = GravityUtil.projectOntoPlane(entity.getDeltaMovement(), down);

            float jumpPower = this.getJumpPower();

            // Replicate vanilla's sprint-jump horizontal bonus
            if (entity.isSprinting()) {
                Vec3 forward = GravityUtil.getForwardOnGravityPlane(entity.getLookAngle(), gravity.getGravity());
                lateral = lateral.add(forward.scale(0.2));
            }

            entity.setDeltaMovement(lateral.add(GravityUtil.getWorldUp(gravity.getGravity()).scale(jumpPower)));
            gravity.beginSurfaceDetach(15);
            entity.setOnGround(false);
            entity.hasImpulse = true;
            ci.cancel();
        });
    }
}
