package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

public class PalisadeBiteForm extends TimedForm {
    private static final int DURATION = 12;
    private static final double ATTACK_RANGE = 10.0;
    private static final double BITE_WIDTH = 3.0;
    private static final float DAMAGE_PER_TICK = 4.0f;

    private final Vec3 targetPoint;

    public PalisadeBiteForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        LivingEntity targetEntity = getLookedAtEntity(user, ATTACK_RANGE);

        if (targetEntity != null) {
            this.targetPoint = targetEntity.getEyePosition();
        } else {
            this.targetPoint = user.getEyePosition().add(user.getLookAngle().scale(ATTACK_RANGE));
        }
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        double currentWidth = BITE_WIDTH * (1.0 - progress);

        Vec3 sideDir = user.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();

        Vec3 leftSlashPos = this.targetPoint.add(sideDir.scale(currentWidth));
        Vec3 rightSlashPos = this.targetPoint.add(sideDir.scale(-currentWidth));

        level.sendParticles(ParticleTypes.CRIT, leftSlashPos.x, leftSlashPos.y, leftSlashPos.z, 5, 0.1, 0.1, 0.1, 0.1);
        level.sendParticles(ParticleTypes.CRIT, rightSlashPos.x, rightSlashPos.y, rightSlashPos.z, 5, 0.1, 0.1, 0.1, 0.1);

        AABB biteZone = new AABB(leftSlashPos, rightSlashPos).inflate(1.5);
        List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, biteZone, e -> e != user && e.isAlive());

        for (LivingEntity hitTarget : hitTargets) {
            if (user instanceof ServerPlayer player) {
                hitTarget.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
            } else {
                hitTarget.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
            }
        }
    }
}
