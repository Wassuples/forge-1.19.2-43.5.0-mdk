package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class DistantThunderForm extends TimedForm {
    private static final int DURATION = 100; // 5 seconds
    private static final double STORM_RADIUS = 8.0;
    private static final int BOLTS_PER_WAVE = 3;
    private static final int TICKS_PER_WAVE = 20; // A wave of lightning every second
    private static final float DAMAGE_PER_BOLT = 7.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    public DistantThunderForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        applyImmobility(user, this.duration + 5);
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
        // Logic for Firing Bolts
        if (ticksActive > 0 && ticksActive % TICKS_PER_WAVE == 0) {
            // Find all potential targets in the storm's radius.
            AABB searchBox = user.getBoundingBox().inflate(STORM_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != user && e.isAlive());

            if (targets.isEmpty()) return;

            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6f, 2.0f);

            // Fire off the set number of bolts for this wave.
            for (int i = 0; i < BOLTS_PER_WAVE; i++) {
                LivingEntity target = targets.get(level.random.nextInt(targets.size()));
                target.hurt(DamageSource.LIGHTNING_BOLT, DAMAGE_PER_BOLT);

                spawnLineParticles(level, YELLOW_LIGHTNING, user.getEyePosition(), target.getEyePosition(), 20);
            }
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
    }
}
