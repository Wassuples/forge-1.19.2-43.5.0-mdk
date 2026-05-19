package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BananaPeelForm extends ActiveForm {
    public BananaPeelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_BANANA);
            Vec3 look = user.getLookAngle().normalize();
            ToonBananaPeelEntity peel = new ToonBananaPeelEntity(level, user);
            peel.setPos(user.getEyePosition().add(look.scale(1.2)).add(0, -0.45, 0));
            peel.setDeltaMovement(look.scale(1.55).add(0, 0.22, 0));
            level.addFreshEntity(peel);
            ToonForceHelper.spawnToonText(level, user.getEyePosition().add(look.scale(1.3)),
                    Component.literal("YEET!").withStyle(ChatFormatting.YELLOW), 16);
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.7f, 1.8f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

