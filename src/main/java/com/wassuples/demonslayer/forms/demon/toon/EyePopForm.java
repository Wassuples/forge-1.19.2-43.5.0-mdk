package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class EyePopForm extends ActiveForm {
    public EyePopForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            ToonEyeProjectileEntity eye = new ToonEyeProjectileEntity(level, user);
            eye.setPos(user.getEyePosition());
            eye.setDeltaMovement(user.getLookAngle().normalize().scale(1.2));
            level.addFreshEntity(eye);
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

