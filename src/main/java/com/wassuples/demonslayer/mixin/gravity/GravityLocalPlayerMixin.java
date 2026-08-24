package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class GravityLocalPlayerMixin {
    @Shadow
    private int autoJumpTime;

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

    // NOTE: The old demonslayer$capturePreFlyVelocity and demonslayer$remapFlyingVertical
    // methods have been removed.  Creative-flight vertical remapping is now handled
    // correctly by GravityTravelMixin (which maps relativeMovement.y → gravity-up in the
    // moveRelative redirect) and GravityPlayerTravelMixin (which decays the gravity-up
    // velocity component to 60 % per tick, matching vanilla's Y decay).  The old approach
    // captured the wrong Y delta (vanilla's decay, not the input) and caused double-remapping.
}
