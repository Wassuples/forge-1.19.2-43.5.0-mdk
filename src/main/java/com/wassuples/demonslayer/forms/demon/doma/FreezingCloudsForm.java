package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.custom.DomaArtEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Doma Blood Demon Art form: Freezing Clouds.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FreezingCloudsForm extends TimedForm {
    private static final int DURATION = 40;
    private static final double CLOUD_RANGE = 30.0;
    private static final double CLOUD_WIDTH = 10.0;
    private static final float DAMAGE = 2.0f;

    private static final int DEBUFF_DURATION = 100; // 5 seconds
    private static final int SLOWNESS_AMPLIFIER = 4;  // Slowness V
    private static final int WEAKNESS_AMPLIFIER = 1;  // Weakness II

    private final Set<UUID> hitEntities = new HashSet<>();

    public FreezingCloudsForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5f, 1.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.POWDER_SNOW_FALL, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.addFreshEntity(new DomaArtEntity(level, user, DomaArtEntity.Mode.FREEZING_CLOUDS, DURATION + 10));
        FormAnimations.play(user, FormAnimations.DOMA_WAVE);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) ticksActive / DURATION;
        double currentDistance = CLOUD_RANGE * progress;

        Vec3 lookDir = user.getLookAngle();
        Vec3 cloudCenter = user.position().add(lookDir.scale(currentDistance));
        Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

        // The rolling wall of mist is drawn by DomaArtEntity (replaces 48 particles per tick).
        AABB hitBox = new AABB(cloudCenter, cloudCenter).inflate(CLOUD_WIDTH, 2.0, CLOUD_WIDTH);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (target.distanceToSqr(cloudCenter) < (CLOUD_WIDTH * CLOUD_WIDTH)) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }

                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, SLOWNESS_AMPLIFIER));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_DURATION, WEAKNESS_AMPLIFIER));
                // We need to add a "Frozen" effect here that handles the visual overlay

                level.playSound(null, target.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                hitEntities.add(target.getUUID());
            }
        }
    }
}
