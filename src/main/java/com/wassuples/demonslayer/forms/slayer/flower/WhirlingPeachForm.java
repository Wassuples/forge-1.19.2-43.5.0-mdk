package com.wassuples.demonslayer.forms.slayer.flower;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

public class WhirlingPeachForm extends TimedForm {
    private static final int DURATION = 20; // A quick 1-second evasive move
    private static final float LEAP_STRENGTH = 0.7f;
    private static final float BACKWARD_STRENGTH = 1.0f;
    private static final double AOE_RADIUS = 3.5;
    private static final float DAMAGE_PER_TICK = 3.0f;
    private static final float ROTATION_SPEED = 45.0f; // degrees per tick

    private static final DustParticleOptions PINK_PETAL = new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.8f), 1.2f);
    private static final DustParticleOptions GREEN_PETAL = new DustParticleOptions(new Vector3f(0.5f, 1.0f, 0.5f), 1.2f);

    private final float startYaw;

    public WhirlingPeachForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.startYaw = user.getYRot();
    }

    @Override
    public void onStart() {
        Vec3 backward = user.getLookAngle().scale(-1);
        Vec3 leap = new Vec3(backward.x * BACKWARD_STRENGTH, LEAP_STRENGTH, backward.z * BACKWARD_STRENGTH);
        user.setDeltaMovement(user.getDeltaMovement().add(leap));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        float newYaw = this.startYaw + (ticksActive * ROTATION_SPEED);
        user.setYRot(newYaw);
        user.setYHeadRot(newYaw);
        dealAOEDamage(user, user.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.3f);
        for (int i = 0; i < 10; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double radius = level.random.nextDouble() * AOE_RADIUS;
            Vec3 offset = new Vec3(Math.cos(angle) * radius, (level.random.nextDouble() - 0.5) * 2.5, Math.sin(angle) * radius);

            ParticleOptions particle = level.random.nextBoolean() ? PINK_PETAL : GREEN_PETAL;
            level.sendParticles(particle, user.getX() + offset.x, user.getY() + 1.0 + offset.y, user.getZ() + offset.z, 1, 0, 0, 0, 0);
            if (level.random.nextInt(5) == 0) {
                level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, user.getX() + offset.x, user.getY() + 2.0, user.getZ() + offset.z, 1, 0, -0.1, 0, 1);
            }
        }
    }
}
