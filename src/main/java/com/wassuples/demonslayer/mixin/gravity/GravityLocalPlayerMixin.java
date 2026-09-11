package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
            if (GravityUtil.getEffectiveGravity(player) != GravityDirection.DOWN) {
                this.autoJumpTime = 0;
            }
        });
    }

    @Redirect(
        method = "aiStep()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void demonslayer$remapCreativeFlightInput(LocalPlayer player, Vec3 requestedVelocity) {
        GravityDirection direction = player.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);
        if (direction == GravityDirection.DOWN) {
            player.setDeltaMovement(requestedVelocity);
            return;
        }

        Vec3 currentVelocity = player.getDeltaMovement();
        double verticalInput = requestedVelocity.y - currentVelocity.y;
        player.setDeltaMovement(currentVelocity
            .add(GravityUtil.getWorldUp(direction).scale(verticalInput)));
    }

    @Inject(method = "canAutoJump()Z", at = @At("HEAD"), cancellable = true)
    private void demonslayer$disableAutoJump(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (GravityUtil.getEffectiveGravity(player) != GravityDirection.DOWN) {
                cir.setReturnValue(false);
            }
        });
    }

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void demonslayer$disableClosestSpacePush(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (GravityUtil.getEffectiveGravity(player) != GravityDirection.DOWN) {
                ci.cancel();
            }
        });
    }

}
