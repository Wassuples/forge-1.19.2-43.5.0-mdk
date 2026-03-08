package com.wassuples.demonslayer.forms.demon.doma;

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

import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Doma Blood Demon Art form: Barren Hanging Garden.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class BarrenHangingGardenForm extends TimedForm {
    private static final int NUMBER_OF_WAVES = 8;
    private static final int TICKS_BETWEEN_WAVES = 2;
    private static final double WAVE_RADIUS = 6.0;
    private static final float DAMAGE_PER_WAVE = 8.0f;
    private static final float KNOCKBACK_PER_WAVE = 0.8f;

    private static final int CHILL_DURATION = 60; // 3 seconds of slowness
    private static final int CHILL_AMPLIFIER = 1; // Slowness II

    public BarrenHangingGardenForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_WAVES * TICKS_BETWEEN_WAVES);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_WAVES == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.8f);

            spawnParticleArcInWorld(level, user.position(), WAVE_RADIUS, 360, 40, 0, 0, ParticleTypes.SNOWFLAKE);
            spawnParticleArcInWorld(level, user.position(), WAVE_RADIUS, 360, 5, 0, 0, ParticleTypes.SWEEP_ATTACK);

            AABB effectZone = user.getBoundingBox().inflate(WAVE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_WAVE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_WAVE);
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_DURATION, CHILL_AMPLIFIER));

                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.knockback(KNOCKBACK_PER_WAVE, knockbackDir.x, knockbackDir.z);
            }
        }
    }
}
