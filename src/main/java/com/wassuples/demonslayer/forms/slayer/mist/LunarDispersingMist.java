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

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

/**
 * Mist Breathing form: Lunar Dispersing Mist.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class LunarDispersingMist extends TimedForm {
    private static final int DURATION = 40; // 2 seconds of air time
    private static final float LEAP_STRENGTH = 1.2f;
    private static final float FORWARD_STRENGTH = 0.7f;
    private static final double AOE_RADIUS = 5.0;
    private static final float DAMAGE_PER_TICK = 2.0f;
    private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

    public LunarDispersingMist(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        // Launch the player upwards and forwards into the fight.
        Vec3 forward = user.getLookAngle().multiply(FORWARD_STRENGTH, 0, FORWARD_STRENGTH);
        Vec3 leap = forward.add(0, LEAP_STRENGTH, 0);
        user.setDeltaMovement(user.getDeltaMovement().add(leap));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    @Override
    public void onTick() {
        super.onTick(); // This handles stopping the form after its duration.

        // --- Continuous AOE Barrage ---
        // Deal damage every tick to all nearby enemies.
        dealAOEDamage(user, user.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.2f);

        // --- Visuals: The Obscuring Clouds and Slashes ---
        // 1. Spawn a large, dense sphere of mist around the player.
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
            double offsetY = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
            double offsetZ = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
            Vec3 particlePos = user.position().add(offsetX, 1.0, offsetZ);

            if (user.position().distanceToSqr(particlePos) <= (AOE_RADIUS * AOE_RADIUS)) {
                level.sendParticles(ParticleTypes.CLOUD, particlePos.x, user.getY() + offsetY, particlePos.z, 1, 0, 0, 0, 0.1);
            }
        }

        // 2. Every 4 ticks, create a visual "slash" effect.
        if (ticksActive % 4 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f);
            // The sweep particle creates a great 360-degree slash visual.
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getEyeY(), user.getZ(), 3, 0, 0, 0, 0);
        }
    }
}
