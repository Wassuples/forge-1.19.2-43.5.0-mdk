package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class FakeTunnelForm extends ActiveForm {
    public FakeTunnelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            Vec3 forward = user.getLookAngle().normalize();
            Vec3 spawnPos = user.position().add(forward.scale(2.5)).add(0, 0.2, 0);
            ToonFakeTunnelEntity tunnel = new ToonFakeTunnelEntity(level, user, forward);
            tunnel.setPos(spawnPos);
            level.addFreshEntity(tunnel);
            level.playSound(null, user.blockPosition(), SoundEvents.ITEM_FRAME_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

