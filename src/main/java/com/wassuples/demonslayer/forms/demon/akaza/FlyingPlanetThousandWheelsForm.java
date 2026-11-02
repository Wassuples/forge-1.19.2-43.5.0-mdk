package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.entity.custom.AkazaArtEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Akaza Blood Demon Art form: Flying Planet Thousand Wheels.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FlyingPlanetThousandWheelsForm extends TimedForm {
    private static final int DURATION = 12;
    private static final double ATTACK_RADIUS = 3.5;
    private static final double ATTACK_HEIGHT = 4.0;
    private static final float DAMAGE = 20.0f;
    private static final float LAUNCH_STRENGTH = 1.8f;
    private static final DustParticleOptions VORTEX_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.6f, 1.0f), 1.0f);
    private final Set<UUID> hitEntities = new HashSet<>();
    private AkazaArtEntity art;

    public FlyingPlanetThousandWheelsForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        this.art = new AkazaArtEntity(level, user, AkazaArtEntity.Mode.WHEELS, DURATION + 6);
        level.addFreshEntity(this.art);
        FormAnimations.play(user, FormAnimations.AKAZA_WHEEL);
    }

    @Override
    public void onStop() {
        if (art != null) art.finish(6);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) ticksActive / DURATION;
        double currentHeight = ATTACK_HEIGHT * progress;

        Vec3 attackCenter = user.position()
                .add(user.getLookAngle().scale(1.5 + progress))
                .add(0, currentHeight, 0);

        // The rising wheel of kicks is drawn by AkazaArtEntity.

        // "...strikes Akaza's target from below and launches them both upward."
        if (ticksActive == 3) {
            Vec3 v = user.getDeltaMovement();
            user.setDeltaMovement(v.x * 0.5, 0.95, v.z * 0.5);
            user.hurtMarked = true;
        }

        AABB hitBox = new AABB(attackCenter, attackCenter).inflate(ATTACK_RADIUS, 1.0, ATTACK_RADIUS);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            Vec3 directionToTarget = target.position().subtract(user.position()).normalize();
            double dotProduct = user.getLookAngle().dot(directionToTarget);

            if (dotProduct > 0) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }
                target.setDeltaMovement(0, LAUNCH_STRENGTH, 0);
                target.hurtMarked = true;

                hitEntities.add(target.getUUID());
            }
        }
    }
}
