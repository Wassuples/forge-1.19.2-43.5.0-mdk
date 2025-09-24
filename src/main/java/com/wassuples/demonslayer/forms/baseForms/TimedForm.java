package com.wassuples.demonslayer.forms.baseForms;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.world.entity.LivingEntity;

public abstract class TimedForm extends ActiveForm {
    protected final int duration;

    public TimedForm(LivingEntity user, IForm form, int durationTicks) {
        super(user, form);
        this.duration = durationTicks;
    }

    @Override
    public void onTick() {
        if (ticksActive >= duration) {
            ActiveFormManager.stop(user, false);
        }
    }
}
