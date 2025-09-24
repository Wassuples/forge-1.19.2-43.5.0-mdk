package com.wassuples.demonslayer.forms.slayer.sound;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class ResoundingSlashesForm extends TimedForm {
    // --- Constants ---
    private static final int DURATION = 100; // 5 seconds
    private static final int DAMAGE_TICK_RATE = 10; // A damage pulse every half-second
    private static final float DAMAGE = 5.0f;
    private static final double RADIUS = 3.5;
    private static final float KNOCKBACK = 1.2f;

    private float rotationAngle = 0;

    public ResoundingSlashesForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public void onStart() {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.duration + 5, 4));
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        AABB defenseBox = user.getBoundingBox().inflate(RADIUS);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, defenseBox);

        for (Projectile projectile : projectiles) {
            level.playSound(null, projectile.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 2.0f);
            level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.1, 0.1, 0.1, 0.5);
            projectile.discard();
        }

        if (ticksActive % DAMAGE_TICK_RATE == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
            dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK);
        }

        this.rotationAngle += 35.0f;
        spawnParticleArcInWorld(level, user.getEyePosition(), RADIUS - 0.5, 160, 15, this.rotationAngle, 0, ParticleTypes.SWEEP_ATTACK);
        spawnParticleArcInWorld(level, user.getEyePosition(), RADIUS - 0.5, 160, 15, this.rotationAngle + 180, 0, ParticleTypes.SWEEP_ATTACK);
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.8f);
    }
}
