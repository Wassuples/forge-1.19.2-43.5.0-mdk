package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class GravityLocalPlayerMixin {
    @Shadow
    private int autoJumpTime;
    @Unique
    private Vec3 demonslayer$preAiVel = null;

    @Inject(method = "aiStep()V", at = @At("HEAD"))
    private void demonslayer$clearQueuedAutoJump(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                this.autoJumpTime = 0;
            }
        });
    }

    @Inject(method = "canAutoJump()Z", at = @At("HEAD"), cancellable = true)
    private void demonslayer$disableAutoJump(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                cir.setReturnValue(false);
            }
        });
    }

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void demonslayer$disableClosestSpacePush(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                ci.cancel();
            }
        });
    }

    // Capture velocity before aiStep so we can see what vanilla adds to Y during the flying block.
    @Inject(method = "aiStep()V", at = @At("HEAD"))
    private void demonslayer$capturePreFlyVelocity(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object)this;
        if (!self.getAbilities().flying || !self.getAbilities().mayfly) return;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                demonslayer$preAiVel = self.getDeltaMovement();
            }
        });
    }

    // After aiStep, remap vanilla's Y flying delta to the gravity-relative up axis.
    @Inject(method = "aiStep()V", at = @At("TAIL"))
    private void demonslayer$remapFlyingVertical(CallbackInfo ci) {
        if (demonslayer$preAiVel == null) return;
        Vec3 pre = demonslayer$preAiVel;
        demonslayer$preAiVel = null;

        LocalPlayer self = (LocalPlayer) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir == GravityDirection.DOWN) return;

            Vec3 current = self.getDeltaMovement();
            double yDelta = current.y - pre.y; // Exactly what vanilla added to Y
            if (Math.abs(yDelta) < 1.0E-7) return;

            // Undo vanilla's Y change, re-apply along gravity-relative "up"
            Vec3 up = GravityUtil.getWorldUp(dir);
            self.setDeltaMovement(
                current.subtract(0, yDelta, 0).add(up.scale(yDelta))
            );
        });
    }
}
