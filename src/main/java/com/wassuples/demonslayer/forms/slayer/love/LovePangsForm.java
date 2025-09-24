package com.wassuples.demonslayer.forms.slayer.love;

import com.mojang.math.Vector3f;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

public class LovePangsForm extends TimedForm {
    private static final double MAX_RANGE = 12.0;
    private static final double WHIP_SPEED = 2.0; // The speed in blocks per tick
    private static final float DAMAGE_PER_HIT = 2.5f;
    private static final int HIT_COUNT = 6; // Total damage = 15
    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
    private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

    private final Vec3 direction;
    private Vec3 whipTipPos;
    private boolean hasHit = false;

    public LovePangsForm(LivingEntity user, IForm form) {
        super(user, form, (int) (MAX_RANGE / WHIP_SPEED));
        this.direction = user.getLookAngle();
        this.whipTipPos = user.getEyePosition();
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.ARROW_SHOOT, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.hasHit) return;
        this.whipTipPos = this.whipTipPos.add(this.direction.scale(WHIP_SPEED));
        level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK,
                this.whipTipPos.x, this.whipTipPos.y, this.whipTipPos.z,
                2, 0.1, 0.1, 0.1, 0);

        AABB hitBox = new AABB(this.whipTipPos, this.whipTipPos).inflate(1.2);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

        if (!potentialTargets.isEmpty()) {
            // We have a hit!
            this.hasHit = true;
            LivingEntity hitTarget = potentialTargets.get(0);
            for (int i = 0; i < HIT_COUNT; i++) {
                if (user instanceof ServerPlayer player) {
                    hitTarget.hurt(DamageSource.playerAttack(player), DAMAGE_PER_HIT);
                } else {
                    hitTarget.hurt(DamageSource.mobAttack(user), DAMAGE_PER_HIT);
                }
            }

            level.playSound(null, hitTarget.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.5f);
            if (hitTarget.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            }
            ActiveFormManager.stop(user);
        }
    }
}
