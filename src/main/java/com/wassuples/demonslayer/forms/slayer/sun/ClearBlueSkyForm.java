package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Sun Breathing form: Clear Blue Sky.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ClearBlueSkyForm extends TimedForm {
    private static final int DURATION = 20;
    private static final double MAX_RADIUS = 15.0;
    private static final float DAMAGE = 40.0f;
    private static final float KNOCKBACK = 1.8f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);
    private final Set<UUID> hitEntities = new HashSet<>();

    public ClearBlueSkyForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);

        user.setYRot(user.getYRot() + 360f);
        user.hurtMarked = true;
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        double currentRadius = MAX_RADIUS * progress;
        double previousRadius = MAX_RADIUS * ((double) (this.ticksActive - 1) / DURATION);

        AABB searchBox = user.getBoundingBox().inflate(currentRadius);

        List<Entity> potentialTargets = level.getEntities(user, searchBox,
                e -> e.isAlive() && !e.is(user) && !this.hitEntities.contains(e.getUUID()));

        for (Entity target : potentialTargets) {
            double distanceToUser = target.distanceTo(user);

            if (distanceToUser <= currentRadius && distanceToUser > previousRadius) {
                if (target instanceof ThrownNichirinEntity ||
                        target instanceof LotusShardProjectileEntity ||
                        target instanceof BloodSickleProjectileEntity ||
                        target instanceof BlindingMistCloudEntity ||
                        target instanceof CuttingThreadCageEntity ||
                        target instanceof KillerGoldfishEntity ||
                        target instanceof MistCloneEntity ||
                        target instanceof PoisonNeedleProjectileEntity ||
                        target instanceof ThreadWheelEntity ||
                        target instanceof WeepingSpearProjectileEntity ||
                        target instanceof WaterPrisonEntity) {
                    level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(), target.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                    target.discard();
                    continue;
                }

                if (target instanceof LivingEntity livingTarget) {
                    if (user instanceof Player player) {
                        livingTarget.hurt(DamageSource.playerAttack(player).setMagic(), DAMAGE);
                    } else {
                        livingTarget.hurt(DamageSource.mobAttack(user).setMagic(), DAMAGE);
                    }

                    Vec3 knockbackDir = livingTarget.position().subtract(user.position()).normalize();
                    livingTarget.knockback(KNOCKBACK, knockbackDir.x, knockbackDir.z);
                    this.hitEntities.add(target.getUUID());
                }
            }
        }

        spawnParticleArcInWorld(level, user.position().add(0, 0.2, 0), currentRadius, 360, 60, 0, 0, RED_HOT);
    }
}
