package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.entity.custom.AkazaArtEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

/**
 * Akaza Blood Demon Art form: Final Chaotic Afterglow.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FinalFormChaoticAfterglowForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double ATTACK_RADIUS = 16.0;
    private static final float DAMAGE = 240.0f;

    private static final DustParticleOptions BLUE_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.8f, 1.0f), 1.0f);
    private static final DustParticleOptions SILVER_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.95f), 1.0f);

    private AkazaArtEntity art;
    private boolean released = false;

    public FinalFormChaoticAfterglowForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        applyImmobility(user, DURATION);

        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.5f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
        this.art = new AkazaArtEntity(level, user, AkazaArtEntity.Mode.AFTERGLOW, DURATION + 30);
        level.addFreshEntity(this.art);
        FormAnimations.play(user, FormAnimations.AKAZA_FINAL);
    }

    @Override
    public void onTick() {
        super.onTick();

        // The gathering light is drawn by AkazaArtEntity (replaces 20 dust particles per tick).

        if (ticksActive == DURATION - 5) {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);

            AABB effectZone = user.getBoundingBox().inflate(ATTACK_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());
            DamageSource unblockableSource = DamageSource.mobAttack(user).setIsFall().setMagic();

            java.util.List<Vec3> struck = new java.util.ArrayList<>();
            for (LivingEntity target : targets) {
                target.hurt(unblockableSource, DAMAGE);
                if (struck.size() < 24) struck.add(target.position());
            }
            // Countless strikes converging on every target (replaces 150 line particles per target).
            if (art != null) art.impact(user.position(), struck, 28);
            level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5f, 0.6f);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.7f);
            released = true;
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        if (art != null && !released) {
            art.discard();
            FormAnimations.play(user, FormAnimations.AKAZA_END);
        }
    }
}
