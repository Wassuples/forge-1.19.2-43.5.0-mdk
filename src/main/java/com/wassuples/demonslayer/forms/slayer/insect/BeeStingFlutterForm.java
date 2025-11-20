package com.wassuples.demonslayer.forms.slayer.insect;

import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BeeStingFlutterForm extends TimedForm {
    private enum Phase {DASHING, RECOILING}

    private static final int DURATION = 10;
    private static final double DASH_SPEED = 3.0;
    private static final double RECOIL_STRENGTH = 1.5;
    private static final float STING_DAMAGE = 1.0f;
    private static final int POISON_DURATION = 80;
    private static final int POISON_AMPLIFIER = 3;

    private Phase currentPhase = Phase.DASHING;
    private final Vec3 direction;

    public BeeStingFlutterForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.direction = user.getLookAngle();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        switch (this.currentPhase) {
            case DASHING:
                user.setDeltaMovement(this.direction.x * DASH_SPEED, user.getDeltaMovement().y, this.direction.z * DASH_SPEED);
                user.hurtMarked = true;
//                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

                level.sendParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.1);

                AABB hitBox = user.getBoundingBox().inflate(0.8);
                List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                if (!potentialTargets.isEmpty()) {
                    LivingEntity hitTarget = potentialTargets.get(0);

                    if (user instanceof ServerPlayer player) {
                        hitTarget.hurt(DamageSource.playerAttack(player), STING_DAMAGE);
                    } else {
                        hitTarget.hurt(DamageSource.mobAttack(user), STING_DAMAGE);
                    }
                    hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));

                    level.playSound(null, hitTarget.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.2f, 1.0f);
                    level.sendParticles(ParticleTypes.CRIT, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 30, 0.4, 0.4, 0.4, 0.5);

                    Vec3 recoilVec = user.position().subtract(hitTarget.position()).normalize().scale(RECOIL_STRENGTH);
                    user.setDeltaMovement(recoilVec.x, 0.4, recoilVec.z);
                    user.hurtMarked = true;
//                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

                    // Transition to next phase
                    this.currentPhase = Phase.RECOILING;
                }
                break;

            case RECOILING:
                break;
        }
    }
}
