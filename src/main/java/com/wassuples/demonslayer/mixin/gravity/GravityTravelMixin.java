package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class GravityTravelMixin {

    /**
     * Redirects the moveRelative call inside travel() to use gravity-relative directions.
     * This is the root fix for diagonal movement and into-wall movement on cardinal gravity.
     */
    @Redirect(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void demonslayer$gravityMoveRelative(LivingEntity instance, float speed, Vec3 relativeMovement) {
        instance.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();

            if (dir == GravityDirection.DOWN) {
                // Vanilla behaviour — untouched
                instance.moveRelative(speed, relativeMovement);
                return;
            }

            // Replicate vanilla's input normalisation from LivingEntity.getInputVector()
            float inputX = (float) relativeMovement.x; // strafe (A/D)
            float inputZ = (float) relativeMovement.z; // forward (W/S)

            float magnitudeSq = inputX * inputX + inputZ * inputZ;
            if (magnitudeSq < 1.0E-7F) return;

            float magnitude = Mth.sqrt(magnitudeSq);
            if (magnitude < 1.0F) magnitude = 1.0F;
            float scale = speed / magnitude;
            inputX *= scale;
            inputZ *= scale;

            float yawToUse = gravity.getGravityYaw();

            // Instead of vanilla's yaw-in-XZ rotation, project onto the gravity floor plane.
            // getLookAngle() returns the true 3D look direction, which getMovementVector projects
            // correctly onto whatever plane the gravity direction defines as the floor.
            Vec3 worldMovement = GravityUtil.getMovementVector(
                instance.getLookAngle(),
                yawToUse,
                new Vec3(inputX, 0.0, inputZ),
                dir
            );

            instance.setDeltaMovement(instance.getDeltaMovement().add(worldMovement));
        });
    }

    /**
     * Applies the missing Y-axis ground friction for cardinal (wall/ceiling) gravity directions.
     * Vanilla only scales X and Z by friction; for EAST/WEST/NORTH/SOUTH gravity, Y is a
     * lateral floor axis and must also receive friction when grounded.
     */
    @Inject(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("RETURN")
    )
    private void demonslayer$applyMissingYFriction(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        GravityDirection dir = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);

        // Only the four cardinal wall directions need this fix.
        // DOWN and UP both have XZ as the lateral plane, which vanilla handles correctly.
        if (dir == GravityDirection.DOWN || dir == GravityDirection.UP) return;
        if (!self.isOnGround()) return;

        // Locate the block the player is standing on in the gravity direction.
        net.minecraft.core.BlockPos groundPos = new net.minecraft.core.BlockPos(
            self.position().add(GravityUtil.getWorldDown(dir).scale(0.6))
        );
        float blockFriction = self.level.getBlockState(groundPos)
            .getFriction(self.level, groundPos, self);
        float friction = 0.91f * blockFriction; // mirrors vanilla's formula

        Vec3 vel = self.getDeltaMovement();
        // Apply friction to the Y component, which vanilla skips for all gravity directions.
        self.setDeltaMovement(vel.x, vel.y * friction, vel.z);
    }
}
