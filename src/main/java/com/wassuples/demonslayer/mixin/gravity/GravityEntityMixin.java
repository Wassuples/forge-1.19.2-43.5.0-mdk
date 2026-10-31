package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class GravityEntityMixin {
    @Shadow public abstract void setPosRaw(double x, double y, double z);

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement()Lnet/minecraft/core/BlockPos;",
        at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityFrictionPosition(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity)(Object)this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                cir.setReturnValue(new BlockPos(self.position().add(
                    GravityUtil.getWorldDown(gravity.getGravity()).scale(0.5000001D))));
            }
        });
    }

    @Inject(method = "getOnPosLegacy()Lnet/minecraft/core/BlockPos;",
        at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityLandingPosition(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity)(Object)this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            if (gravity.getGravity() != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                cir.setReturnValue(new BlockPos(self.position().add(
                    GravityUtil.getWorldDown(gravity.getGravity()).scale(0.20000000298023224D))));
            }
        });
    }

    /**
     * After vanilla recalculates the bounding box from the new position,
     * override it with our gravity-aware shape.
     * Entity construction also calls setPos, before ServerPlayer.gameMode exists.
     * Check for active custom gravity before calling player state methods.
     */
    @Inject(method = "setPos(DDD)V", at = @At("RETURN"))
    private void demonslayer$fixBoundingBoxAfterSetPos(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                self.setBoundingBox(GravityUtil.makeBoundingBox(self, dir));
            }
        });
    }

    @Inject(method = "getBoundingBoxForPose(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityPoseAabb(Pose pose, CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                cir.setReturnValue(GravityUtil.makeBoundingBox(self.position(), self.getDimensions(pose), dir));
            }
        });
    }

    @Inject(method = "getEyeY()D", at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityEyeY(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                double halfHeight = self.getDimensions(self.getPose()).height * 0.5D;
                double eyeOffset = self.getEyeHeight() - halfHeight;
                Vec3 eyePos = self.getBoundingBox().getCenter().add(GravityUtil.getWorldUp(dir).scale(eyeOffset));
                cir.setReturnValue(eyePos.y);
            }
        });
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                double halfHeight = self.getDimensions(self.getPose()).height * 0.5D;
                double eyeOffset = self.getEyeHeight() - halfHeight;
                cir.setReturnValue(self.getBoundingBox().getCenter().add(GravityUtil.getWorldUp(dir).scale(eyeOffset)));
            }
        });
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void demonslayer$gravityEyePositionPartial(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir != GravityDirection.DOWN) {
                if (self instanceof Player && (self.isPassenger() || self.isSpectator())) return;
                double halfHeight = self.getDimensions(self.getPose()).height * 0.5D;
                double eyeOffset = self.getEyeHeight() - halfHeight;
                Vec3 interpolatedCenter = self.getPosition(partialTicks)
                    .add(self.getBoundingBox().getCenter().subtract(self.position()));
                cir.setReturnValue(interpolatedCenter.add(GravityUtil.getWorldUp(dir).scale(eyeOffset)));
            }
        });
    }
}
