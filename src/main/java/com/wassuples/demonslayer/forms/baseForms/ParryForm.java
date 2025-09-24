package com.wassuples.demonslayer.forms.baseForms;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import static com.wassuples.demonslayer.util.CombatHelper.isParryable;

public abstract class ParryForm extends TimedForm {
    public ParryForm(LivingEntity user, IForm form, int parryWindow) {
        super(user, form, parryWindow);
    }

    @Override
    public void onHurt(LivingHurtEvent event) {
        if (isParryable(event.getSource())) {
            event.setCanceled(true);
            onParrySuccess(event.getSource().getDirectEntity());
            ActiveFormManager.stop(user);
        }
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 2.0f);
    }

    protected abstract void onParrySuccess(Entity attacker);
}
