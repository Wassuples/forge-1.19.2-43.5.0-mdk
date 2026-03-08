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
            // If this instance is running as the active form, stop normally.
            // If it's being ticked as a sub-form inside another active form (e.g. SUN_13), do nothing here;
            // the parent form controls step transitions.
            ActiveForm active = ActiveFormManager.getActiveForm(user);
            if (active == this) {
                ActiveFormManager.stop(user, false);
            }
        }
    }
}
