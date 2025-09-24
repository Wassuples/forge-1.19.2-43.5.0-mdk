package com.wassuples.demonslayer.forms.demon.hantengu;

import com.wassuples.demonslayer.entity.custom.WeepingSpearProjectileEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

public class WeepingSpearsForm extends ActiveForm {
    private static final int SPEAR_COUNT = 6;
    private static final float SPREAD = 3.0f;

    public WeepingSpearsForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5f, 0.8f);
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        for (int i = 0; i < SPEAR_COUNT; i++) {
            WeepingSpearProjectileEntity spear = new WeepingSpearProjectileEntity(level, user);
            Vec3 lookAngle = user.getLookAngle();
            spear.setPos(user.getEyePosition());
            spear.shoot(lookAngle.x, lookAngle.y, lookAngle.z, 2.5f, SPREAD);

            level.addFreshEntity(spear);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
