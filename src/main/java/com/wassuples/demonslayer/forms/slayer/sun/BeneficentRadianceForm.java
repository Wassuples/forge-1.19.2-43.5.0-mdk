package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Sun Breathing form: Beneficent Radiance.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class BeneficentRadianceForm extends TimedForm {
    private static final int DURATION = 25;
    private static final double ASCENT_SPEED = 1.2;
    private static final double VORTEX_RADIUS = 3.5;
    private static final float DAMAGE_PER_TICK = 5.0f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    public BeneficentRadianceForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        user.setDeltaMovement(0, ASCENT_SPEED, 0);
        user.hurtMarked = true;
        user.fallDistance = 0;

        AABB damageZone = user.getBoundingBox().inflate(VORTEX_RADIUS, 0, VORTEX_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());

        for (LivingEntity target : targets) {
            // Deal damage
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player).setIsFire(), DAMAGE_PER_TICK);
            } else {
                target.hurt(DamageSource.mobAttack(user).setIsFire(), DAMAGE_PER_TICK);
            }
            Vec3 pullVector = user.position().subtract(target.position()).normalize().scale(0.5);
            target.setDeltaMovement(pullVector.x, user.getDeltaMovement().y, pullVector.z);
            target.hurtMarked = true;
        }

        // Visuals
        float rotation = this.ticksActive * 40;
        spawnParticleArcInWorld(level, user.position().add(0, 1.0, 0), VORTEX_RADIUS, 360, 20, rotation, 90, RED_HOT);
        spawnParticleArcInWorld(level, user.position().add(0, 1.0, 0), VORTEX_RADIUS, 360, 10, -rotation, 90, ParticleTypes.LAVA);
    }
}
