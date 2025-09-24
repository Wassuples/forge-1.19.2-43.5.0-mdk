package com.wassuples.demonslayer.forms.demon.gyutaro;

import com.wassuples.demonslayer.entity.custom.BloodSickleProjectileEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

public class FlyingBloodSicklesForm extends TimedForm {
    private static final int SICKLE_COUNT = 6;
    private static final int TICKS_BETWEEN_SICKLES = 3;
    private final LivingEntity target;

    public FlyingBloodSicklesForm(LivingEntity user, IForm form) {
        super(user, form, SICKLE_COUNT * TICKS_BETWEEN_SICKLES);
        this.target = getLookedAtEntity(user, 32.0);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SICKLES == 0) {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 2.0f);
            BloodSickleProjectileEntity sickle = new BloodSickleProjectileEntity(level, user, this.target);
            Vec3 spread = new Vec3(
                    level.random.nextGaussian() * 0.1,
                    level.random.nextGaussian() * 0.1,
                    level.random.nextGaussian() * 0.1
            );

            sickle.shoot(user.getLookAngle().x, user.getLookAngle().y, user.getLookAngle().z, 1.5f, 5.0f);
            sickle.setDeltaMovement(sickle.getDeltaMovement().add(spread));

            level.addFreshEntity(sickle);
        }
    }
}
