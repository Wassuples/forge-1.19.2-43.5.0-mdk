package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealThrustDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class ThunderclapAndFlashForm extends TimedForm {
    private static final int CHARGE_PAUSE_DURATION = 10;
    private static final double TELEPORT_RANGE = 27.0;
    private static final float ATTACK_DAMAGE = 18.0f;
    private static final float PATH_DAMAGE = 8.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);
    private final Vec3 direction;

    public ThunderclapAndFlashForm(LivingEntity user, IForm form) {
        super(user, form, CHARGE_PAUSE_DURATION + 2);
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.2f);
    }

    private void performSlash() {
        user.setDeltaMovement(Vec3.ZERO);
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        dealThrustDamage(user, 5.0, ATTACK_DAMAGE);
        spawnParticleArc(user, YELLOW_LIGHTNING, 3.0, 140, 40);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive <= CHARGE_PAUSE_DURATION) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;

            if (level.random.nextFloat() < 0.6f) {
                Vec3 particlePos = user.position().add(Math.random() - 0.5, user.getBbHeight() * Math.random(), Math.random() - 0.5);
                level.sendParticles(YELLOW_LIGHTNING, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        } else {
            Vec3 startPos = user.position();
            Vec3 endPos = startPos.add(this.direction.scale(TELEPORT_RANGE));

            AABB pathHitbox = new AABB(startPos, endPos).inflate(2.0);
            List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, pathHitbox, e -> e != user && e.isAlive());
            for (LivingEntity target : hitTargets) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), PATH_DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), PATH_DAMAGE);
                }
            }

            for (double i = 0; i < TELEPORT_RANGE; i += 0.5) {
                Vec3 particlePos = startPos.add(this.direction.scale(i));
                level.sendParticles(YELLOW_LIGHTNING, particlePos.x, particlePos.y + 1.0, particlePos.z, 5, 0.2, 0.2, 0.2, 0);
            }

            user.teleportTo(endPos.x, endPos.y, endPos.z);
            performSlash();
            ActiveFormManager.stop(user);
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
