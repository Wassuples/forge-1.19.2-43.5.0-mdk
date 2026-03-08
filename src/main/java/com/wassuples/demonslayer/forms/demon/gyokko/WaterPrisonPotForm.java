package com.wassuples.demonslayer.forms.demon.gyokko;


import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.WaterPrisonEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;

/**
 * Gyokko Blood Demon Art form: Water Prison Pot.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WaterPrisonPotForm extends ActiveForm {
    private static final double MAX_RANGE = 25.0;

    public WaterPrisonPotForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        LivingEntity target = getLookedAtEntity(user, MAX_RANGE);

        if (target != null) {
            WaterPrisonEntity prison = ModEntityTypes.WATER_PRISON_POT.get().create(level);
            if (prison != null) {
                prison.setPos(target.position());
                applyImmobility(target, 220);
                prison.setTrappedEntity(target);

                level.addFreshEntity(prison);
                level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 1.0f);
            }
        } else {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
