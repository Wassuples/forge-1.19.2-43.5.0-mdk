package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class RumbleAndFlashForm extends TimedForm {
    private static final int DURATION = 80; // 4 seconds channeling
    private static final double MAX_RANGE = 32.0;
    private static final int TICKS_PER_BOLT = 15; // every 0.75 seconds
    private static final float DAMAGE_PER_BOLT = 8.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    private LivingEntity target;

    public RumbleAndFlashForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    // This method was missing and is essential for cleanup.
    @Override
    public void onStop() {
        // Free the player from their stance when the form ends.
        removeImmobility(user);
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    @Override
    public void onStart() {
        this.target = getLookedAtEntity(user, MAX_RANGE);

        if (this.target == null) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            ActiveFormManager.stop(user);
            return;
        }

        applyImmobility(user, this.duration + 5);
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (target == null || !target.isAlive() || target.distanceTo(user) > MAX_RANGE + 4.0) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive % 4 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getRandomX(0.8), user.getRandomY(), user.getRandomZ(0.8), 2, 0, 0, 0, 0);
        }

        if (ticksActive > 0 && ticksActive % TICKS_PER_BOLT == 0) {
            Vec3 skyPos = target.getEyePosition().add(0, 15, 0);
            target.hurt(DamageSource.LIGHTNING_BOLT, DAMAGE_PER_BOLT);

            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 2.0f);
            spawnLineParticles(level, YELLOW_LIGHTNING, skyPos, target.getEyePosition(), 30);
        }
    }
}
