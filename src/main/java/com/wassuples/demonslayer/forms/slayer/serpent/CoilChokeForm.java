package com.wassuples.demonslayer.forms.slayer.serpent;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

public class CoilChokeForm extends TimedForm {
    private static final int DURATION = 60;
    private static final float DAMAGE_PER_PULSE = 3.0f;
    private static final int DAMAGE_TICK_RATE = 5;
    private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);

    private static final double ORBIT_RADIUS = 3.0;
    private static final float ORBIT_SPEED = 20.0f;

    private static final double DASH_DISTANCE = 12.0;
    private static final double COIL_START_RADIUS = 2.5;
    private static final float COIL_ROTATION_SPEED = 40.0f;

    @Nullable
    private final LivingEntity target;
    private float currentAngle;
    private final Vec3 startPos;
    private final Vec3 direction;

    public CoilChokeForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.target = getLookedAtEntity(user, 10.0);
        this.startPos = user.position();
        this.direction = user.getLookAngle();
        this.currentAngle = user.getYRot();
    }

    @Override
    public void onStart() {
        if (!user.isOnGround()) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            ActiveFormManager.stop(user);
            return;
        }
        if (this.target != null) {
            // Root the player for the orbital assault.
            applyImmobility(user, this.duration + 5);
        }
        // Use a better sound that isn't the Enderman stare.
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.target != null) {
            // TARGETED ORBITAL ASSAULT
            if (!this.target.isAlive() || this.target.distanceTo(user) > 32) {
                ActiveFormManager.stop(user);
                return;
            }

            // --- 1. Hybrid Movement Logic ---
            // Calculate the IDEAL position for the CURRENT and NEXT ticks on the circle.
            this.currentAngle += ORBIT_SPEED;
            double currentAngleRad = Math.toRadians(this.currentAngle);
            double nextAngleRad = Math.toRadians(this.currentAngle + ORBIT_SPEED);

            Vec3 currentOffset = new Vec3(Math.cos(currentAngleRad), 0, Math.sin(currentAngleRad)).scale(ORBIT_RADIUS);
            Vec3 nextOffset = new Vec3(Math.cos(nextAngleRad), 0, Math.sin(nextAngleRad)).scale(ORBIT_RADIUS);

            Vec3 currentOrbitPos = this.target.position().add(currentOffset);
            Vec3 nextOrbitPos = this.target.position().add(nextOffset);

            user.setPos(currentOrbitPos.x, currentOrbitPos.y, currentOrbitPos.z);
            Vec3 velocityHint = nextOrbitPos.subtract(currentOrbitPos);
            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

        } else {
            // UNTARGETED DASH
            double progress = (double) ticksActive / DURATION;
            double nextProgress = Math.min((double) (ticksActive + 1) / DURATION, 1.0);

            // --- 2. Hybrid Movement Logic (Same pattern as ShiftingFlowSlash) ---
            // Calculate IDEAL position for CURRENT tick
            Vec3 forwardPos = this.startPos.add(this.direction.scale(progress * DASH_DISTANCE));
            double currentCoilRadius = COIL_START_RADIUS * (1.0 - progress);
            this.currentAngle += COIL_ROTATION_SPEED;
            double angleRad = Math.toRadians(this.currentAngle);
            Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 offset = sideDir.scale(Math.cos(angleRad) * currentCoilRadius);
            Vec3 currentCurvePos = forwardPos.add(offset);
            user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

            // Calculate IDEAL position for NEXT tick
            Vec3 nextForwardPos = this.startPos.add(this.direction.scale(nextProgress * DASH_DISTANCE));
            double nextCoilRadius = COIL_START_RADIUS * (1.0 - nextProgress);
            double nextAngleRad = Math.toRadians(this.currentAngle + COIL_ROTATION_SPEED);
            Vec3 nextOffset = sideDir.scale(Math.cos(nextAngleRad) * nextCoilRadius);
            Vec3 nextCurvePos = nextForwardPos.add(nextOffset);

            // Set velocity hint for the client
            Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);
            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        }

        // --- Shared Damage and Visuals (Unchanged) ---
        if (ticksActive % DAMAGE_TICK_RATE == 0) {
            dealAOEDamage(user, user.position(), 3.0, DAMAGE_PER_PULSE, 0.2f);
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
        }
        level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
