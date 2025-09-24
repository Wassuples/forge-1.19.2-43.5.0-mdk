package com.wassuples.demonslayer.forms.slayer.sun;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.List;

import static com.wassuples.demonslayer.styles.BreathingForms.*;

public class ThirteenthForm extends ActiveForm {
    private static final int BREATH_DRAIN_PER_SECOND = 30;
    private static final int TOTAL_FORMS = 12;

    private int currentFormIndex = 0;
    private int ticksInCurrentForm = 0;

    private static final int[] FORM_DURATIONS = {
            5, // Dance
            20, // Clear Blue Sky
            20, // Raging Sun
            30, // Burning Bones
            25, // Setting Sun
            15, // Solar Heat Haze
            25, // Beneficent Radiance
            40, // Sunflower Thrust
            40, // Dragon Dance
            30, // Fire Wheel
            10, // Fake Rainbow
            20  // Flame Dance
    };

    private static final List<IForm> SUN_FORMS = Arrays.asList(
            SUN_1, SUN_2, SUN_3, SUN_4, SUN_5, SUN_6, SUN_7, SUN_8, SUN_9, SUN_10, SUN_11, SUN_12
    );

    public ThirteenthForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() { return true; }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 1.0f);
        executeFormLogic(0);
    }

    @Override
    public void onTick() {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1));
        user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 2));
        user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 1));

        if (ticksActive % 20 == 0) {
            user.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() < BREATH_DRAIN_PER_SECOND) {
                    ActiveFormManager.stop(user);
                } else {
                    breath.subBreath(BREATH_DRAIN_PER_SECOND);
                }
            });
        }

        ticksInCurrentForm++;
        executeFormLogic(currentFormIndex);

        if (ticksInCurrentForm >= FORM_DURATIONS[currentFormIndex]) {
            currentFormIndex++;
            ticksInCurrentForm = 0;

            if (currentFormIndex >= TOTAL_FORMS) {
                ActiveFormManager.stop(user);
            }
        }
    }

    private void executeFormLogic(int formIndex) {
        switch (formIndex) {
            case 0: // Dance
                if (ticksInCurrentForm == 1) {
                    SUN_1.performAction(user, level);
                }
                break;
            case 1: // Clear Blue Sky
                new ClearBlueSkyForm(user, SUN_2).onTick();
                break;
            case 2: // Raging Sun
                new RagingSunForm(user, SUN_3).onTick();
                break;
            case 3: // Burning Bones
                new BurningBonesForm(user, SUN_4).onTick();
                break;
            case 4: // Setting Sun
                new SettingSunForm(user, SUN_5).onTick();
                break;
            case 5: // Solar Heat Haze
                new SolarHeatHazeForm(user, SUN_6).onTick();
                break;
            case 6: // Beneficent Radiance
                new BeneficentRadianceForm(user, SUN_7).onTick();
                break;
            case 7: // Sunflower Thrust
                if (ticksInCurrentForm == 1) {
                    SunflowerThrustForm form = new SunflowerThrustForm(user, SUN_8);
                    form.onChargeRelease(true);
                    form.onReleaseTick();
                }
                break;
            case 8: // Dragon Dance
                new SunHaloDragonForm(user, SUN_9).onTick();
                break;
            case 9: // Fire Wheel
                new FireWheelForm(user, SUN_10).onTick();
                break;
            case 10: // Fake Rainbow
                new FakeRainbowForm(user, SUN_11).onTick();
                break;
            case 11: // Flame Dance
                new FlameDanceForm(user, SUN_12).onTick();
                break;
        }
    }
}
