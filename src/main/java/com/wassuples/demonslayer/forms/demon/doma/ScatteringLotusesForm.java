package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.custom.LotusShardProjectileEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Doma Blood Demon Art form: Scattering Lotuses.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ScatteringLotusesForm extends ActiveForm {
    private static final int PROJECTILE_COUNT = 10;
    private static final float SPREAD = 12.0f; // Higher number = wider spread
    private static final float PROJECTILE_SPEED = 2.0f;

    public ScatteringLotusesForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);

        for (int i = 0; i < PROJECTILE_COUNT; i++) {
            LotusShardProjectileEntity shard = new LotusShardProjectileEntity(level, user);
            Vec3 lookAngle = user.getLookAngle();

            shard.shoot(lookAngle.x, lookAngle.y, lookAngle.z, PROJECTILE_SPEED, SPREAD);
            level.addFreshEntity(shard);
        }

        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
