package com.wassuples.demonslayer.forms.demon.gyokko;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.KillerGoldfishEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gyokko Blood Demon Art form: Thousand Needle Fish Kill.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ThousandNeedleFishKillForm extends ActiveForm {
    private static final int FISH_COUNT = 3;
    private static final double SPAWN_RADIUS = 3.0;

    public ThousandNeedleFishKillForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);

        for (int i = 0; i < FISH_COUNT; i++) {
            double angle = (2 * Math.PI / FISH_COUNT) * i;
            double x = user.getX() + Math.cos(angle) * SPAWN_RADIUS;
            double z = user.getZ() + Math.sin(angle) * SPAWN_RADIUS;
            // Spawn at foot level so the fish land immediately; gravity does the rest.
            double y = user.getY();

            KillerGoldfishEntity fish = ModEntityTypes.KILLER_GOLDFISH.get().create(level);
            if (fish != null) {
                fish.moveTo(x, y, z, user.getYRot(), 0);
                fish.setOwner(user);
                level.addFreshEntity(fish);
            }
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
