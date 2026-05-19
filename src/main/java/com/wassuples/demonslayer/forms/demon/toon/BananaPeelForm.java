package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class BananaPeelForm extends ActiveForm {
    public BananaPeelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            ToonBananaPeelEntity peel = new ToonBananaPeelEntity(level, user);
            peel.setPos(user.getEyePosition());
            peel.setDeltaMovement(user.getLookAngle().normalize().scale(0.6));
            level.addFreshEntity(peel);
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.7f, 1.8f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

