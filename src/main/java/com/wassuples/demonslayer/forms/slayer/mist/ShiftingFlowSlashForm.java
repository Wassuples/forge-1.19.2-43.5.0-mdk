package com.wassuples.demonslayer.forms.slayer.mist;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Mist Breathing form: Shifting Flow Slash.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ShiftingFlowSlashForm extends TimedForm {
    private static final int PAUSE_DURATION = 4;
    private static final int DASH_DURATION = 10;
    private static final double TARGETING_RANGE = 16.0;
    private static final double DASH_DISTANCE = 10.0; // The distance for a non-targeted dash
    private static final double S_CURVE_AMPLITUDE = 2.0;
    private static final float SLASH_DAMAGE = 14.0f;
    private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

    private final Vec3 startPos;
    private final Vec3 endPos;

    public ShiftingFlowSlashForm(LivingEntity user, IForm form) {
        super(user, form, PAUSE_DURATION + DASH_DURATION);
        @Nullable LivingEntity target = getLookedAtEntity(user, TARGETING_RANGE);
        this.startPos = user.position();

        if (target != null) {
            // If we have a target, the end position is in front of them.
            this.endPos = target.position().add(target.getLookAngle().scale(-1.5));
        } else {
            // If we have NO target, the end position is 10 blocks straight ahead.
            this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
        }
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        // 1: The "Low Stance" Pause
        if (ticksActive < PAUSE_DURATION) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;
            level.sendParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 0.1, user.getZ(), 2, 0.3, 0.0, 0.3, 0.02);
        }
        // 2: The S-Curve Dash
        else {
            int dashTicks = ticksActive - PAUSE_DURATION;
            double dashProgress = Math.min((double) dashTicks / DASH_DURATION, 1.0);

            Vec3 forwardDir = endPos.subtract(startPos).normalize();
            Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 straightPos = startPos.lerp(endPos, dashProgress);
            double sineAngle = dashProgress * Math.PI * 2.0;
            double sineOffset = S_CURVE_AMPLITUDE * Math.sin(sineAngle);
            Vec3 curvePos = straightPos.add(sideDir.scale(sineOffset));
            user.setPos(curvePos.x, curvePos.y, curvePos.z);
            Vec3 nextCurvePos = startPos.lerp(endPos, Math.min((double) (dashTicks + 1) / DASH_DURATION, 1.0)).add(sideDir.scale(S_CURVE_AMPLITUDE * Math.sin(Math.min((double) (dashTicks + 1) / DASH_DURATION, 1.0) * Math.PI * 2.0)));
            Vec3 velocityHint = nextCurvePos.subtract(curvePos);
            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//                user.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(user));

            dealAOEDamage(user, user.position(), 2.0, 2.0f, 0.1f);
            level.sendParticles(DENSE_MIST, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0);
            if (dashTicks >= DASH_DURATION) {
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.2f);
                dealDirectionalDamage(user, 3.0, SLASH_DAMAGE, 0.85);
                spawnParticleArc(user, DENSE_MIST, 2.5, 160, 30);
            }
        }
    }
}
