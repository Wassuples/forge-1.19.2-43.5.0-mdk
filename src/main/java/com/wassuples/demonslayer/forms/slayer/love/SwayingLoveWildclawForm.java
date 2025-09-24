package com.wassuples.demonslayer.forms.slayer.love;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealVerticalColumnDamage;

public class SwayingLoveWildclawForm extends TimedForm {
    private static final int DURATION = 60; // Tornado lasts for 3 seconds
    private static final float LEAP_STRENGTH = 1.1f;
    private static final double MAX_RANGE = 20.0;
    private static final double TORNADO_RADIUS = 3.5;
    private static final double TORNADO_HEIGHT = 7.0;
    private static final float DAMAGE_PER_PULSE = 7.0f;
    private static final int DAMAGE_TICK_RATE = 10; // A damage pulse every half-second

    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
    private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

    private final Vec3 tornadoCenter;

    public SwayingLoveWildclawForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);

        if (hitResult.getType() == HitResult.Type.MISS) {
            this.tornadoCenter = null; // Mark as failed if aiming at the sky or too far.
        } else {
            this.tornadoCenter = Vec3.atBottomCenterOf(hitResult.getBlockPos());
        }
    }

    @Override
    public void onStart() {
        if (this.tornadoCenter == null) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            ActiveFormManager.stop(user);
            return;
        }
        user.setDeltaMovement(user.getDeltaMovement().add(0, LEAP_STRENGTH, 0));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.tornadoCenter == null) return;

        if (ticksActive % DAMAGE_TICK_RATE == 0) {
            dealVerticalColumnDamage(user, this.tornadoCenter, TORNADO_RADIUS, TORNADO_HEIGHT, DAMAGE_PER_PULSE);
            level.playSound(null, tornadoCenter.x, tornadoCenter.y, tornadoCenter.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0f, 1.8f);
        }

        for (int i = 0; i < 15; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double radius = level.random.nextDouble() * TORNADO_RADIUS;
            double y = level.random.nextDouble() * TORNADO_HEIGHT;
            Vec3 particlePos = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius).add(this.tornadoCenter);

            Vec3 centerToParticle = particlePos.subtract(this.tornadoCenter);
            Vec3 tangentialVel = new Vec3(-centerToParticle.z, 0, centerToParticle.x).normalize().scale(0.4);
            Vec3 upwardVel = new Vec3(0, 0.2, 0);

            ParticleOptions particle = (i % 2 == 0) ? PINK_SPARK : GREEN_SPARK;
            level.sendParticles(particle, particlePos.x, particlePos.y, particlePos.z, 1, tangentialVel.x, upwardVel.y, tangentialVel.z, 0);

            if (level.random.nextInt(10) == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
