package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class EyePopForm extends ActiveForm {
    public EyePopForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_EYE_POP);
            Vec3 look = user.getLookAngle().normalize();
            ToonEyeProjectileEntity eye = new ToonEyeProjectileEntity(level, user);
            eye.setPos(user.getEyePosition().add(look.scale(1.15)).add(0, -0.08, 0));
            eye.setDeltaMovement(look.scale(4.2));
            level.addFreshEntity(eye);
            ToonForceHelper.spawnToonText(level, user.getEyePosition().add(look.scale(1.4)),
                    net.minecraft.network.chat.Component.literal("POP!").withStyle(net.minecraft.ChatFormatting.AQUA), 18);
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.0f, 2.0f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

