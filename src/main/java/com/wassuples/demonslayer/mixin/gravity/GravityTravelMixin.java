package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies vanilla travel with local yaw; velocity and all subsequent physics stay local. */
@Mixin(LivingEntity.class)
public abstract class GravityTravelMixin {
    @Unique private GravityDirection demonslayer$headingGravity = GravityDirection.DOWN;
    @Unique private float demonslayer$movementYaw;

    @Redirect(
        method = {"travel(Lnet/minecraft/world/phys/Vec3;)V",
            "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"},
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    private void demonslayer$moveRelativeInPlayerFrame(LivingEntity instance, float speed, Vec3 input) {
        if (!(instance instanceof Player) || instance.isPassenger() || instance.isSpectator()) {
            demonslayer$headingGravity = GravityDirection.DOWN;
            demonslayer$movementYaw = 0.0F;
            instance.moveRelative(speed, input);
            return;
        }
        GravityDirection direction = instance.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN);
        if (direction == GravityDirection.DOWN) {
            demonslayer$headingGravity = GravityDirection.DOWN;
            demonslayer$movementYaw = 0.0F;
            instance.moveRelative(speed, input);
            return;
        }

        if (demonslayer$headingGravity != direction) {
            demonslayer$movementYaw = 0.0F;
            demonslayer$headingGravity = direction;
        }
        float localYaw = GravityUtil.getMovementYaw(
            instance.getLookAngle(), direction, demonslayer$movementYaw);
        demonslayer$movementYaw = localYaw;
        Vec3 acceleration = GravityUtil.getInputVector(input, speed, localYaw);
        instance.setDeltaMovement(instance.getDeltaMovement().add(acceleration));
    }

    @Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 demonslayer$lookInLocalFrame(LivingEntity instance) {
        Vec3 worldLook = instance.getLookAngle();
        GravityDirection direction = instance instanceof Player && !instance.isPassenger() && !instance.isSpectator()
            ? instance.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
                .map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN)
            : GravityDirection.DOWN;
        return GravityUtil.worldToLocal(worldLook, direction);
    }
}
