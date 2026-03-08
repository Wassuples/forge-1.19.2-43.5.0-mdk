package com.wassuples.demonslayer.forms.slayer.flame;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Flame Breathing form: Unknowing Fire.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class UnknowingFireForm extends TimedForm {
    private final Vec3 direction;
    private static final int DASH_DURATION = 8;
    private static final int SLASH_DELAY = 3;

    public UnknowingFireForm(LivingEntity user, IForm form) {
        super(user, form, DASH_DURATION + SLASH_DELAY + 2);
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (ticksActive < DASH_DURATION) {
            user.setDeltaMovement(direction.scale(2.5));
            user.hurtMarked = true;
            level.sendParticles(ParticleTypes.SMOKE, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0.1);

            dealAOEDamage(user, user.position(), 1.5, 2.0f, 0.1f);

        } else if (ticksActive == DASH_DURATION) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;
        } else if (ticksActive == DASH_DURATION + SLASH_DELAY) {
            float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90.0F;
            user.setYRot(yaw);
            user.setYHeadRot(yaw);
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            dealDirectionalDamage(user, 3.0, 8.0f, 0.7);
            spawnParticleArc(user, ParticleTypes.FLAME, 2.0, 140, 30);
        }
    }
}
