package com.wassuples.demonslayer.forms.slayer.wind;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnVortexParticles;

/**
 * Wind Breathing form: Idaten Typhoon.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class IdatenTyphoonForm extends ActiveForm {
    private final BlockPos targetPos;
    private static final int VORTEX_DURATION = 20;

    public IdatenTyphoonForm(LivingEntity user, IForm form) {
        super(user, form);
        this.targetPos = user.blockPosition();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
        user.setDeltaMovement(user.getLookAngle().scale(-1.8).add(0, 1.2, 0));
        user.hurtMarked = true;
    }

    @Override
    public void onTick() {
        if (ticksActive <= VORTEX_DURATION) {
            // Vortex logic...
            double radius = 10.0;
            AABB effectZone = new AABB(targetPos).inflate(radius, 3, radius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());
            for (LivingEntity target : targets) {
                Vec3 pullDir = Vec3.atCenterOf(targetPos).subtract(target.position()).normalize();
                target.push(pullDir.x * 0.5, pullDir.y * 0.5 + 0.2, pullDir.z * 0.5);
            }
            spawnVortexParticles(level, targetPos, radius, 50, ticksActive);
        } else if (ticksActive == VORTEX_DURATION + 1) {
            // Explosion logic...
            dealAOEDamage(user, Vec3.atCenterOf(targetPos), 10.0, 40.0f, 2.0f);
            level.playSound(null, targetPos, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 3, 0, 0, 0, 0);

            ActiveFormManager.stop(user);
        }
    }
}
