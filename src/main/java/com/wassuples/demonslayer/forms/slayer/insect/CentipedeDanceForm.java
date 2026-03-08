package com.wassuples.demonslayer.forms.slayer.insect;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Insect Breathing form: Centipede Dance.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CentipedeDanceForm extends TimedForm {
    private static final int DASH_DURATION = 25;
    private static final double DASH_DISTANCE = 18.0;
    private static final double ZIGZAG_AMPLITUDE = 1.5; // How wide the zig-zags are
    private static final double ZIGZAG_FREQUENCY = 12.0; // How fast the zig-zags are

    private static final float FINISHER_DAMAGE = 8.0f;
    private static final int FINISHER_POISON_DURATION = 120;
    private static final int FINISHER_POISON_AMPLIFIER = 4;  // Wisteria Poison V

    private static final DustParticleOptions BUTTERFLY_PARTICLE = new DustParticleOptions(new Vector3f(0.8f, 0.4f, 1.0f), 1.0f);

    private final Vec3 startPos;
    private final Vec3 endPos;
    private boolean hasHit = false;

    public CentipedeDanceForm(LivingEntity user, IForm form) {
        super(user, form, DASH_DURATION);
        this.startPos = user.position();
        this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 1.6f);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (this.hasHit) return;

        double dashProgress = Math.min((double) ticksActive / DASH_DURATION, 1.0);
        Vec3 forwardDir = endPos.subtract(startPos).normalize();
        Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 straightPos = startPos.lerp(endPos, dashProgress);
        double zigzagOffset = ZIGZAG_AMPLITUDE * Math.sin(dashProgress * Math.PI * ZIGZAG_FREQUENCY);
        Vec3 currentCurvePos = straightPos.add(sideDir.scale(zigzagOffset));

        double nextDashProgress = Math.min((double) (ticksActive + 1) / DASH_DURATION, 1.0);
        Vec3 nextStraightPos = startPos.lerp(endPos, nextDashProgress);
        double nextZigzagOffset = ZIGZAG_AMPLITUDE * Math.sin(nextDashProgress * Math.PI * ZIGZAG_FREQUENCY);
        Vec3 nextCurvePos = nextStraightPos.add(sideDir.scale(nextZigzagOffset));
        Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);

        user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);
        user.setDeltaMovement(velocityHint);
        user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

        level.sendParticles(BUTTERFLY_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getY() + 1, user.getZ(), 1, 0, 0, 0, 0);

        AABB hitBox = user.getBoundingBox().inflate(0.8);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

        if (!potentialTargets.isEmpty()) {
            this.hasHit = true;
            LivingEntity hitTarget = potentialTargets.get(0);

            if (user instanceof ServerPlayer player) {
                hitTarget.hurt(DamageSource.playerAttack(player), FINISHER_DAMAGE);
            } else {
                hitTarget.hurt(DamageSource.mobAttack(user), FINISHER_DAMAGE);
            }
            hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), FINISHER_POISON_DURATION, FINISHER_POISON_AMPLIFIER));
            level.playSound(null, hitTarget.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.sendParticles(ParticleTypes.EXPLOSION, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 2, 0, 0, 0, 0);

            ActiveFormManager.stop(user);
        }
    }
}
