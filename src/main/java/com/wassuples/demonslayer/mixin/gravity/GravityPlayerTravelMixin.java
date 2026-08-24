package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDebug;
import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes creative-mode flight for non-DOWN gravity directions (Issue 2).
 *
 * <h3>The Problem</h3>
 * Vanilla's {@code Player.travel()} flying path ends with:
 * <pre>  setDeltaMovement(vec3.x, d0 * 0.6, vec3.z);</pre>
 * This decays Y velocity to 60 % each tick, giving smooth ascent/descent for DOWN gravity.
 * For custom gravity (e.g. EAST) the "up" axis is X, not Y.  After
 * {@link GravityTravelMixin}'s redirect maps Space/Shift input to the X axis,
 * {@code Player.travel()} never resets that X component, so the player accelerates
 * indefinitely in the gravity-up direction instead of reaching a constant flying speed.
 *
 * <h3>The Fix</h3>
 * <ol>
 *   <li>At the HEAD of {@code Player.travel()}, capture the gravity-up velocity component.</li>
 *   <li>At the RETURN of {@code Player.travel()}, decay that component to
 *       {@code prevGravUp * 0.6} — exactly what vanilla does for Y, applied to whichever
 *       world axis is "up" for the active gravity direction.</li>
 * </ol>
 *
 * <p>This mixin lives in the common "mixins" section so it applies on both the client and
 * the dedicated server, keeping flight behaviour consistent across both.
 */
@Mixin(Player.class)
public abstract class GravityPlayerTravelMixin {

    @Unique
    private double demonslayer$preFlightGravUpVel = Double.NaN;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void demonslayer$captureFlightGravUp(Vec3 travelVector, CallbackInfo ci) {
        Player self = (Player)(Object)this;

        // Only applies during creative / spectator flight.
        if (!self.getAbilities().flying || !self.getAbilities().mayfly) {
            demonslayer$preFlightGravUpVel = Double.NaN;
            return;
        }

        GravityDirection dir = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);

        // DOWN gravity: vanilla already decays Y correctly — don't interfere.
        if (dir == GravityDirection.DOWN) {
            demonslayer$preFlightGravUpVel = Double.NaN;
            return;
        }

        Vec3 up = GravityUtil.getWorldUp(dir);
        Vec3 velBefore = self.getDeltaMovement();
        demonslayer$preFlightGravUpVel = velBefore.dot(up);

        // ── DEBUG (Issue 2) ────────────────────────────────────────────────────────────────
        if (Math.abs(velBefore.y) > 1.0E-4 || Math.abs(demonslayer$preFlightGravUpVel) > 1.0E-4) {
            GravityDebug.log("FLIGHT-HEAD",
                "{} dir={} velBeforeVanillaTravel={} literalY={} gravUpVel={}",
                self.level.isClientSide() ? "CLIENT" : "SERVER", dir, velBefore, velBefore.y,
                demonslayer$preFlightGravUpVel);
        }
        // ── END DEBUG ─────────────────────────────────────────────────────────────
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void demonslayer$decayFlightGravUp(Vec3 travelVector, CallbackInfo ci) {
        if (Double.isNaN(demonslayer$preFlightGravUpVel)) return;

        Player self = (Player)(Object)this;
        if (!self.getAbilities().flying || !self.getAbilities().mayfly) {
            demonslayer$preFlightGravUpVel = Double.NaN;
            return;
        }

        GravityDirection dir = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);
        if (dir == GravityDirection.DOWN) {
            demonslayer$preFlightGravUpVel = Double.NaN;
            return;
        }

        Vec3   up             = GravityUtil.getWorldUp(dir);
        Vec3   vel            = self.getDeltaMovement();
        double currentGravUp  = vel.dot(up);
        double targetGravUp   = demonslayer$preFlightGravUpVel * 0.6; // mirror vanilla's d0 * 0.6

        // ── DEBUG (Issue 2) ── this is the critical line: vel/vel.y here already reflects
        // whatever vanilla's own Player.travel() body (including its hardcoded literal-Y
        // "d0 * 0.6" decay) just did, BEFORE our correction below runs. If literalY here is
        // far smaller than what GravityTravelMixin's MOVE log just added this tick, vanilla
        // is clobbering it. ──────────────────────────────────────────────────────────────────────────────
        GravityDebug.log("FLIGHT-RET",
            "{} dir={} velAfterVanillaTravelBody={} literalY={} gravUpVelNow={} gravUpVelTarget={}",
            self.level.isClientSide() ? "CLIENT" : "SERVER", dir, vel, vel.y, currentGravUp, targetGravUp);

        double delta = targetGravUp - currentGravUp;
        if (Math.abs(delta) > 1.0E-7) {
            self.setDeltaMovement(vel.add(up.scale(delta)));
        }

        GravityDebug.log("FLIGHT-RET", "{} velAfterOurCorrection={}",
            self.level.isClientSide() ? "CLIENT" : "SERVER", self.getDeltaMovement());
        // ── END DEBUG ─────────────────────────────────────────────────────────────

        demonslayer$preFlightGravUpVel = Double.NaN;
    }
}
