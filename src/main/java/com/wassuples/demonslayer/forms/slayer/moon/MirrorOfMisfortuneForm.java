package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Mirror Of Misfortune.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class MirrorOfMisfortuneForm extends TimedForm {
    private static final int DURATION = 40;
    private static final double MAX_RANGE = 30.0;
    private static final double START_WIDTH = 4.0;
    private static final double END_WIDTH = 14.0;

    private static final float BASE_DAMAGE_PER_TICK = 1.5f;
    private static final float DAMAGE_RAMP_PER_TICK = 0.1f;
    private static final double PUSH_SPEED = MAX_RANGE / DURATION;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.2f);
    private final Vec3 direction;

    public MirrorOfMisfortuneForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.2f, 0.9f);
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        double currentDistance = MAX_RANGE * progress;
        double currentWidth = Mth.lerp(progress, START_WIDTH, END_WIDTH);
        Vec3 waveCenter = user.position().add(this.direction.scale(currentDistance));

        AABB damageBox = new AABB(waveCenter, waveCenter).inflate(currentWidth, 3.0, currentWidth);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageBox, e -> e != user && e.isAlive());

        float currentDamage = BASE_DAMAGE_PER_TICK + (this.ticksActive * DAMAGE_RAMP_PER_TICK);

        for (LivingEntity target : potentialTargets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), currentDamage);
            } else {
                target.hurt(DamageSource.mobAttack(user), currentDamage);
            }

            Vec3 pushVector = this.direction.scale(PUSH_SPEED);
            target.setDeltaMovement(pushVector.x, 0.1, pushVector.z);
            target.hurtMarked = true;
        }

        spawnParticleArcInWorld(level, waveCenter.add(0, 1.5, 0), currentWidth, 120, (int) (currentWidth * 5), user.getYRot(), 0, PURPLE_SLASH);
        for (int i = 0; i < 3; i++) {
            Vec3 randomPos = waveCenter.add(
                    (level.random.nextDouble() - 0.5) * currentWidth * 2,
                    (level.random.nextDouble() - 0.5) * 3.0,
                    (level.random.nextDouble() - 0.5) * currentWidth * 2
            );
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, randomPos.x, randomPos.y, randomPos.z, 1, 0, 0, 0, 0);
        }
    }
}
