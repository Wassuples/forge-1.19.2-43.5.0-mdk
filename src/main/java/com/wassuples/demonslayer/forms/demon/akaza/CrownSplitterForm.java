package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

/**
 * Akaza Blood Demon Art form: Crown Splitter.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CrownSplitterForm extends TimedForm {
    private static final double ATTACK_RANGE = 5.0;
    private static final float PRIMARY_DAMAGE = 40.0f;
    private static final float LAUNCH_STRENGTH_VERTICAL = 1f;
    private static final float LAUNCH_STRENGTH_HORIZONTAL = 2f;
    private static final double SHOCKWAVE_RADIUS = 5.0;
    private static final float SHOCKWAVE_DAMAGE = 15.0f;
    private static final float SHOCKWAVE_KNOCKBACK = 1.0f;
    private static final double LUNGE_SPEED = 1.5;
    private boolean isLunge = false;
    private Vec3 lungeDirection;

    public CrownSplitterForm(LivingEntity user, IForm form) {
        super(user, form, 10);
    }

    @Override
    public void onStart() {
        LivingEntity target = getLookedAtEntity(user, ATTACK_RANGE);

        if (target != null) {
            this.isLunge = false;
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.2f, 1.2f);

            Vec3 strikePos = target.position().subtract(user.getLookAngle().scale(1.5));
            double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) strikePos.x, (int) strikePos.z);
            user.teleportTo(strikePos.x, groundY, strikePos.z);

            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), PRIMARY_DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), PRIMARY_DAMAGE);
            }

            Vec3 horizontalPush = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize()
                    .scale(LAUNCH_STRENGTH_HORIZONTAL);

            target.setDeltaMovement(horizontalPush.add(0, LAUNCH_STRENGTH_VERTICAL, 0));
            target.hurtMarked = true;

            Vec3 shockwaveCenter = target.position();
            level.sendParticles(ParticleTypes.SONIC_BOOM, shockwaveCenter.x, shockwaveCenter.y + 1.0, shockwaveCenter.z, 1, 0, 0, 0, 0);

            List<LivingEntity> shockwaveTargets = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(shockwaveCenter, shockwaveCenter).inflate(SHOCKWAVE_RADIUS),
                    e -> e != user && e != target && e.isAlive());

            for (LivingEntity secondaryTarget : shockwaveTargets) {
                if (user instanceof ServerPlayer player) {
                    secondaryTarget.hurt(DamageSource.playerAttack(player), SHOCKWAVE_DAMAGE);
                } else {
                    secondaryTarget.hurt(DamageSource.mobAttack(user), SHOCKWAVE_DAMAGE);
                }
                Vec3 knockbackDir = secondaryTarget.position().subtract(shockwaveCenter).normalize();
                secondaryTarget.knockback(SHOCKWAVE_KNOCKBACK, knockbackDir.x, knockbackDir.z);
            }

        } else {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
            this.isLunge = true;
            Vec3 direction = user.getLookAngle();
            this.lungeDirection = new Vec3(direction.x, 0, direction.z).normalize();
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.isLunge) {
            user.setDeltaMovement(this.lungeDirection.scale(LUNGE_SPEED));
            user.hurtMarked = true;
        }
    }
}
