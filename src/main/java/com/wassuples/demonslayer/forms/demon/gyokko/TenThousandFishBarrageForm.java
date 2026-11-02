package com.wassuples.demonslayer.forms.demon.gyokko;

import com.wassuples.demonslayer.entity.custom.FishSchoolEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleStreamWithWeave;

/**
 * Gyokko Blood Demon Art form: Ten Thousand Fish Barrage.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class TenThousandFishBarrageForm extends ActiveForm {
    private static final float DAMAGE_PER_HIT = 7.0f;
    private static final double RANGE = 25.0;
    private static final double IMPACT_RADIUS = 2.0;

    public TenThousandFishBarrageForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        Vec3 targetPoint;
        LivingEntity lookedAtEntity = getLookedAtEntity(user, RANGE);
        if (lookedAtEntity != null) {
            targetPoint = lookedAtEntity.position().add(0, lookedAtEntity.getBbHeight() / 2.0, 0);
        } else {
            targetPoint = user.getEyePosition().add(user.getLookAngle().scale(RANGE));
        }
        List<Vec3> origins = List.of(
                new Vec3(0.6, 1.4, 0.4), new Vec3(-0.6, 1.4, 0.4),
                new Vec3(0.8, 1.0, 0.6), new Vec3(-0.8, 1.0, 0.6),
                new Vec3(1.0, 0.7, 0.8), new Vec3(-1.0, 0.7, 0.8)
        );
        level.playSound(null, user.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 1.5f, 0.8f);
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        FormAnimations.play(user, FormAnimations.GYOKKO_VASE);
        List<Vec3[]> streams = new java.util.ArrayList<>();
        for (Vec3 localOrigin : origins) {
            Vec3 startPoint = user.position().add(localOrigin.yRot(-user.getYRot() * Mth.DEG_TO_RAD));
            double spread = 4.0;
            Vec3 finalTargetPoint = targetPoint.add(
                    (level.random.nextDouble() - 0.5) * spread,
                    (level.random.nextDouble() - 0.5) * spread,
                    (level.random.nextDouble() - 0.5) * spread
            );
            streams.add(new Vec3[]{startPoint, finalTargetPoint});
            dealAOEDamage(user, finalTargetPoint, IMPACT_RADIUS, DAMAGE_PER_HIT, 0.5f);
        }
        // The shoal of goldfish (replaces the 360-particle dust streams).
        level.addFreshEntity(new FishSchoolEntity(level, user.position(), streams));
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
