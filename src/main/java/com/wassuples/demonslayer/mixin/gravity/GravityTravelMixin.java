package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Core gravity-relative movement mixin.  Lives in the common "mixins" section so that it
 * applies on BOTH the client and the dedicated server — this is the root fix for Issue 1
 * (vertical movement nullified).  Previously, this was client-only, so the server ran vanilla
 * moveRelative and never added Y velocity for e.g. EAST gravity.  The server-side velocity
 * then decayed to zero via Phase.END friction, making all movement along the absolute Y axis
 * appear completely nullified.
 */
@Mixin(LivingEntity.class)
public abstract class GravityTravelMixin {

    // ── Sneak-at-edge state (Issue 3) ────────────────────────────────────────
    @Unique private Vec3    demonslayer$preTravelPos      = null;
    @Unique private boolean demonslayer$wasTravelGrounded = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // Issue 1 & 2 — gravity-relative moveRelative redirect
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Replaces the vanilla {@code moveRelative} call inside {@code travel()} with a version
     * that projects input onto the correct gravity-relative axes.
     *
     * <p>Key changes over the old implementation:
     * <ul>
     *   <li>The look direction is derived from {@code getLookAngle()} (synced by movement
     *       packets) instead of {@code gravity.getGravityYaw()} (client-only capability field),
     *       so the calculation is identical on client and server.</li>
     *   <li>Non-player entities that lack the capability fall through to vanilla behaviour.</li>
     *   <li>The Y component of {@code relativeMovement} (Space / Shift during creative flight)
     *       is now included in the 3-D magnitude normalisation and mapped to the gravity-up
     *       axis, fixing Issue 2 (creative flight ignoring the vertical input axis).</li>
     * </ul>
     */
    @Redirect(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void demonslayer$gravityMoveRelative(LivingEntity instance, float speed, Vec3 relativeMovement) {
        // Entities without the capability (mobs, etc.) keep vanilla behaviour.
        var capOpt = instance.getCapability(PlayerGravity.GRAVITY_CAPABILITY);
        if (!capOpt.isPresent()) {
            instance.moveRelative(speed, relativeMovement);
            return;
        }

        GravityDirection dir = capOpt.map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN);

        // DOWN gravity → vanilla unchanged.
        if (dir == GravityDirection.DOWN) {
            instance.moveRelative(speed, relativeMovement);
            return;
        }

        // ── 3-D normalisation (mirrors vanilla LivingEntity.getInputVector) ──────────
        float inputX = (float) relativeMovement.x; // strafe   (A / D)
        float inputY = (float) relativeMovement.y; // vertical (Space / Shift in flight)
        float inputZ = (float) relativeMovement.z; // forward  (W / S)

        float magnitudeSq = inputX * inputX + inputY * inputY + inputZ * inputZ;
        if (magnitudeSq < 1.0E-7F) return; // no input

        float magnitude = Mth.sqrt(magnitudeSq);
        if (magnitude < 1.0F) magnitude = 1.0F; // prevent amplification of short vectors
        float scale = speed / magnitude;
        inputX *= scale;
        inputY *= scale;
        inputZ *= scale;

        // ── Gravity-relative axes from the live look vector ─────────────────────────
        // getLookAngle() is kept consistent between client and server via movement packets,
        // so using it here — instead of the client-only gravityYaw field — makes the
        // server compute exactly the same movement as the client.
        Vec3 up      = GravityUtil.getWorldUp(dir);
        Vec3 forward = GravityUtil.getForwardOnGravityPlane(instance.getLookAngle(), dir);
        // left = up × forward — matches the "relLeft" convention in GravityUtil.getMovementVector
        Vec3 left    = up.cross(forward);

        Vec3 worldMovement =
                left.scale(inputX)           // A / D     → lateral on the gravity floor
                .add(forward.scale(inputZ))  // W / S     → forward on the gravity floor
                .add(up.scale(inputY));      // Spc / Sft → gravity-up  (flight vertical)

        instance.setDeltaMovement(instance.getDeltaMovement().add(worldMovement));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Issue 1 — missing Y-axis ground friction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vanilla only multiplies X and Z by block-friction after each ground tick.
     * For EAST / WEST / NORTH / SOUTH gravity, Y is a lateral floor axis and must receive
     * the same treatment.
     *
     * <p>Now that this mixin runs on both sides, Phase.END in ModEvents and ClientEvents no
     * longer applies its own {@code .scale(0.91)} to lateral velocity — that was double-friction
     * and made custom-gravity movement noticeably slower than vanilla.
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

        // DOWN and UP both have X/Z as their lateral plane — vanilla handles those already.
        if (dir == GravityDirection.DOWN || dir == GravityDirection.UP) return;
        // Flying players use vanilla's air damping on both gravity-lateral axes. World Y
        // has already received vanilla's 0.98 factor, so convert it to the 0.91 factor
        // vanilla applied to the other lateral world axes instead of applying ground friction.
        if (self instanceof Player player
                && player.getAbilities().flying
                && !player.isPassenger()) {
            if (!self.isInWater() && !self.isInLava() && !self.isFallFlying()) {
                Vec3 velocity = self.getDeltaMovement();
                self.setDeltaMovement(velocity.x, velocity.y * ((double) 0.91F / 0.98D), velocity.z);
            }
            return;
        }
        if (!self.isOnGround()) return;

        net.minecraft.core.BlockPos groundPos = new net.minecraft.core.BlockPos(
            self.position().add(GravityUtil.getWorldDown(dir).scale(0.6))
        );
        float blockFriction = self.level.getBlockState(groundPos)
            .getFriction(self.level, groundPos, self);
        float friction = 0.91f * blockFriction; // mirrors vanilla's formula

        Vec3 vel = self.getDeltaMovement();
        // Apply the same block-friction-scaled damping to Y that vanilla applies to X/Z.
        self.setDeltaMovement(vel.x, vel.y * friction, vel.z);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Issue 3 — sneak-at-edge for custom gravity
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Before {@code travel()} moves the entity, record the position and whether the player
     * was standing on the custom-gravity surface while crouching.
     */
    @Inject(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD")
    )
    private void demonslayer$capturePreTravelState(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;

        GravityDirection dir = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);

        // Only players sneak; vanilla already handles DOWN gravity edge-cling.
        if (dir == GravityDirection.DOWN
                || !(self instanceof Player player)
                || !player.isCrouching()) {
            demonslayer$preTravelPos      = null;
            demonslayer$wasTravelGrounded = false;
            return;
        }

        // Don't interfere with a deliberate jump-off (surfaceDetachActive = player jumping).
        boolean surfaceDetachActive = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::isSurfaceDetachActive)
            .orElse(false);
        if (surfaceDetachActive) {
            demonslayer$preTravelPos      = null;
            demonslayer$wasTravelGrounded = false;
            return;
        }

        boolean grounded = GravityUtil.isStandingOnCustomSurface(self, dir);
        demonslayer$wasTravelGrounded = grounded;
        demonslayer$preTravelPos      = grounded ? self.position() : null;
    }

    /**
     * After {@code travel()} has applied physics and moved the entity, check whether a
     * sneaking player drifted off the edge of the custom-gravity surface.  If so, teleport
     * them back to the pre-travel position and zero lateral velocity — reproducing vanilla's
     * sneak-at-edge behaviour for any gravity direction.
     */
    @Inject(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("RETURN")
    )
    private void demonslayer$sneakAtEdgeRevert(Vec3 travelVector, CallbackInfo ci) {
        if (!demonslayer$wasTravelGrounded || demonslayer$preTravelPos == null) return;

        LivingEntity self = (LivingEntity)(Object)this;
        GravityDirection dir = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);

        if (dir == GravityDirection.DOWN) {
            demonslayer$preTravelPos      = null;
            demonslayer$wasTravelGrounded = false;
            return;
        }

        // If still on the custom surface, the move was safe — nothing to undo.
        if (!GravityUtil.isStandingOnCustomSurface(self, dir)) {
            // Movement carried the player off the edge while crouching — roll it back.
            Vec3 pre = demonslayer$preTravelPos;
            self.setPos(pre.x, pre.y, pre.z);

            // Zero lateral velocity so the player doesn't slide off on the next frame.
            Vec3 down    = GravityUtil.getWorldDown(dir);
            Vec3 vel     = self.getDeltaMovement();
            double downC = vel.dot(down);
            self.setDeltaMovement(down.scale(downC)); // keep only the gravity-axis component
        }

        demonslayer$preTravelPos      = null;
        demonslayer$wasTravelGrounded = false;
    }
}
