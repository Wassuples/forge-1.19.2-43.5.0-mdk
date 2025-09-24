package com.wassuples.demonslayer.forms.demon.rui;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.ThreadWheelEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class CuttingThreadRotationForm extends TimedForm {
    public CuttingThreadRotationForm(LivingEntity user, IForm form) {
        super(user, form, 5);
    }

    @Override
    public void onStart() {
        user.level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 1.2F, 0.7F);

        ThreadWheelEntity wheel = ModEntityTypes.THREAD_WHEEL.get().create(level);
        if (wheel != null) {
            Vec3 spawnPos = user.position().add(user.getLookAngle().scale(1.5));
            wheel.moveTo(spawnPos.x, spawnPos.y + 0.5, spawnPos.z, user.getYRot(), user.getXRot());

            final double SPEED = 0.4;
            wheel.setDirection(user.getLookAngle(), SPEED);

            level.addFreshEntity(wheel);
        }
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}
