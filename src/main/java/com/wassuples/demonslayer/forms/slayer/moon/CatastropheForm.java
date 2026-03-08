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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Catastrophe.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CatastropheForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double MAX_RADIUS = 20.0;
    private static final float DAMAGE = 40.0f;
    private static final float KNOCKBACK = 2.0f;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.5f);
    private final Set<UUID> hitEntities = new HashSet<>();

    public CatastropheForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);
        applyImmobility(user, 10);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        double currentRadius = MAX_RADIUS * progress;
        double previousRadius = MAX_RADIUS * ((double) (this.ticksActive - 1) / DURATION);

        AABB searchBox = user.getBoundingBox().inflate(currentRadius);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive() && !this.hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            double distanceToUser = target.distanceTo(user);
            if (distanceToUser <= currentRadius && distanceToUser > previousRadius) {
                if (user instanceof Player player) {
                    target.hurt(DamageSource.playerAttack(player).setExplosion(), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user).setExplosion(), DAMAGE);
                }

                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.knockback(KNOCKBACK, knockbackDir.x, knockbackDir.z);
                this.hitEntities.add(target.getUUID());
            }
        }

        for (int i = 0; i < 20; i++) {
            Vec3 randomDir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).normalize();
            Vec3 particlePos = user.position().add(randomDir.scale(currentRadius));

            float randomYaw = level.random.nextFloat() * 360;
            float randomPitch = -90 + level.random.nextFloat() * 180;
            spawnParticleArcInWorld(level, particlePos, 1.5 + progress * 2.0, 120, 10, randomYaw, randomPitch, PURPLE_SLASH);
        }
    }
}
