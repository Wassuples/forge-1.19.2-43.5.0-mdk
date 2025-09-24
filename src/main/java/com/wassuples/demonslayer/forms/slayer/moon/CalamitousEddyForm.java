package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class CalamitousEddyForm extends TimedForm {
    private static final int DURATION = 80;
    private static final double VORTEX_RADIUS = 7.0;
    private static final float DAMAGE_PER_TICK = 2.0f;
    private static final double PULL_STRENGTH = 0.07;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);

    public CalamitousEddyForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        applyImmobility(user, DURATION + 5);
        level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        AABB vortexBox = user.getBoundingBox().inflate(VORTEX_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, vortexBox, e -> e != user && e.isAlive());

        for (LivingEntity target : targets) {
            if (ticksActive % 5 == 0) {
                if (user instanceof Player player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
                }
            }
            Vec3 pushVector = target.position().subtract(user.position()).normalize().scale(PULL_STRENGTH);
            target.setDeltaMovement(target.getDeltaMovement().add(pushVector));
        }
        float rotation1 = (float) (ticksActive * 12);
        float rotation2 = (float) (ticksActive * -15);
        float rotation3 = (float) (ticksActive * 9);

        spawnParticleArcInWorld(level, user.position().add(0, 1.2, 0), VORTEX_RADIUS, 360, 20, rotation1, 0, PURPLE_SLASH);
        spawnParticleArcInWorld(level, user.position().add(0, 1.2, 0), VORTEX_RADIUS, 360, 20, rotation2, 45, PURPLE_SLASH);
        spawnParticleArcInWorld(level, user.position().add(0, 1.2, 0), VORTEX_RADIUS, 360, 20, rotation3, -45, PURPLE_SLASH);
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
