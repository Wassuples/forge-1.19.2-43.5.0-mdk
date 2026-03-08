package com.wassuples.demonslayer.forms.slayer.love;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

/**
 * Love Breathing form: Cat Legged Winds.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CatLeggedWindsForm extends TimedForm {
    private enum Phase {OUTWARD, INWARD}

    private static final double MAX_RANGE = 10.0;
    private static final double WHIP_SPEED = 2.0; // The speed in blocks per tick
    private static final float DAMAGE = 9.0f;
    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
    private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

    private Phase currentPhase = Phase.OUTWARD;
    private final Vec3 direction;
    private Vec3 whipTipPos;
    private final List<UUID> hitInPhase = new ArrayList<>();

    public CatLeggedWindsForm(LivingEntity user, IForm form) {
        super(user, form, (int) (MAX_RANGE / WHIP_SPEED) * 2);
        this.direction = user.getLookAngle();
        this.whipTipPos = user.getEyePosition();
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.currentPhase == Phase.OUTWARD) {
            this.whipTipPos = this.whipTipPos.add(this.direction.scale(WHIP_SPEED));

            if (user.position().distanceToSqr(this.whipTipPos) >= MAX_RANGE * MAX_RANGE) {
                this.currentPhase = Phase.INWARD;
                this.hitInPhase.clear();
            }
        } else {
            Vec3 returnDirection = user.getEyePosition().subtract(this.whipTipPos).normalize();
            this.whipTipPos = this.whipTipPos.add(returnDirection.scale(WHIP_SPEED));
        }

        level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK,
                this.whipTipPos.x, this.whipTipPos.y, this.whipTipPos.z,
                3, 0.1, 0.1, 0.1, 0);

        AABB hitBox = new AABB(this.whipTipPos, this.whipTipPos).inflate(1.5);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != user && e.isAlive() && !hitInPhase.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
            }
            this.hitInPhase.add(target.getUUID());
            level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
