package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla creative-flight damping aligned with the player's gravity axis. */
@Mixin(Player.class)
public abstract class GravityPlayerTravelMixin {
    @Unique
    private double demonslayer$preTravelUpVelocity;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void demonslayer$capturePreTravelUpVelocity(Vec3 travelVector, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        GravityDirection direction = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);
        demonslayer$preTravelUpVelocity = self.getDeltaMovement()
            .dot(GravityUtil.getWorldUp(direction));
    }

    /**
     * Vanilla restores X/Z from the result of {@code LivingEntity.travel}, but replaces
     * world Y with its pre-travel value times {@code 0.6}. For sideways gravity that
     * discards movement along a valid floor axis. At the setter itself the complete
     * post-travel velocity is still available, so retain its gravity-lateral components
     * and apply the same damping only to gravity-up velocity.
     */
    @Redirect(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(DDD)V"
        )
    )
    private void demonslayer$applyGravityRelativeFlightDamping(Player instance,
                                                                double vanillaX,
                                                                double vanillaDampedY,
                                                                double vanillaZ) {
        GravityDirection direction = instance.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);
        if (direction == GravityDirection.DOWN) {
            instance.setDeltaMovement(vanillaX, vanillaDampedY, vanillaZ);
            return;
        }

        Vec3 postTravelVelocity = instance.getDeltaMovement();
        Vec3 up = GravityUtil.getWorldUp(direction);
        double dampedUpVelocity = demonslayer$preTravelUpVelocity * 0.6D;
        double correction = dampedUpVelocity - postTravelVelocity.dot(up);
        instance.setDeltaMovement(postTravelVelocity.add(up.scale(correction)));
    }
}
