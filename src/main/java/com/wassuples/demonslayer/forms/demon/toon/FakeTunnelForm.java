package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.world.entity.LivingEntity;

public class FakeTunnelForm extends ActiveForm {
    public FakeTunnelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        // TODO: Rebuild Fake Tunnel once wall placement, pass-through rules, and readable counterplay are designed.
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

