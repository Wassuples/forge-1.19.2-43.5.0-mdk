package com.wassuples.demonslayer.forms.slayer.insect;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ButterflyDanceCapriceForm extends TimedForm {
    private static final int DASH_DURATION = 15;
    private static final double DASH_SPEED = 1.5;
    private static final float STING_DAMAGE = 1.0f;
    private static final int POISON_DURATION = 140;
    private static final int POISON_AMPLIFIER = 1;

    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
    private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

    private final Vec3 direction;
    private boolean hasHit = false;

    public ButterflyDanceCapriceForm(LivingEntity user, IForm form) {
        super(user, form, DASH_DURATION);
        this.direction = user.getLookAngle();
    }

    @Override
    public void onStart() {
        user.setDeltaMovement(user.getDeltaMovement().add(0, 0.6, 0));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 2.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        user.setDeltaMovement(this.direction.x * DASH_SPEED, user.getDeltaMovement().y, this.direction.z * DASH_SPEED);
        user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

        Vec3 forwardDir = user.getDeltaMovement().normalize();
        Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
        double serpentAngle = ticksActive * 1.3;
        double serpentRadius = 0.9;
        Vec3 offset = sideDir.scale(Math.sin(serpentAngle) * serpentRadius).add(0, Math.cos(serpentAngle) * serpentRadius, 0);
        Vec3 particlePos = user.position().add(offset);
        level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK, particlePos.x, particlePos.y + 1, particlePos.z, 1, 0, 0, 0, 0);

        if (!this.hasHit) {
            AABB hitBox = user.getBoundingBox().inflate(1.0);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

            if (!potentialTargets.isEmpty()) {
                this.hasHit = true;
                LivingEntity hitTarget = potentialTargets.get(0);

                if (user instanceof ServerPlayer player) {
                    hitTarget.hurt(DamageSource.playerAttack(player), STING_DAMAGE);
                } else {
                    hitTarget.hurt(DamageSource.mobAttack(user), STING_DAMAGE);
                }
                hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));

                level.playSound(null, hitTarget.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.8f);
                if (hitTarget.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 20, 0.4, 0.4, 0.4, 0.5);
                }
                ActiveFormManager.stop(user);
            }
        }
    }
}
