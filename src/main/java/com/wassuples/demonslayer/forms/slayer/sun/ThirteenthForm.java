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

import static com.wassuples.demonslayer.styles.BreathingForms.*;

public class ThirteenthForm extends ActiveForm {
    private static final int BREATH_DRAIN_PER_SECOND = 30;

    /**
     * This form chains the first 12 Sun forms.
     * NOTE: We MUST keep sub-form instances alive across ticks, otherwise all per-form state resets every tick
     * (impact centers, moving vortex centers, trail buffers, etc.), causing wrong positions and "frozen" projectiles.
     */
    private static final int TOTAL_FORMS = 12;

    private int currentFormIndex = -1;
    private int ticksInCurrentForm = 0;

    private ActiveForm currentSubForm;

    private static final int[] FORM_DURATIONS = {
            5,  // Dance (SUN_1) - instant action
            20, // Clear Blue Sky
            20, // Raging Sun
            25, // Burning Bones
            25, // Setting Sun
            15, // Solar Heat Haze
            15, // Beneficent Radiance
            10, // Sunflower Thrust (release phase)
            20, // Dragon Dance
            30, // Fire Wheel
            20, // Fake Rainbow
            20  // Flame Dance
    };

    @FunctionalInterface
    private interface FormFactory {
        ActiveForm create(LivingEntity user, IForm form);
    }

    private static final FormFactory[] FORM_FACTORIES = new FormFactory[]{
            (u, f) -> null, // index 0 handled specially (SUN_1)
            ClearBlueSkyForm::new,
            RagingSunForm::new,
            BurningBonesForm::new,
            SettingSunForm::new,
            SolarHeatHazeForm::new,
            BeneficentRadianceForm::new,
            SunflowerThrustForm::new,
            SunHaloDragonForm::new,
            FireWheelForm::new,
            FakeRainbowForm::new,
            FlameDanceForm::new
    };

    private static final IForm[] SUN_FORMS = {
            SUN_1, SUN_2, SUN_3, SUN_4, SUN_5, SUN_6, SUN_7, SUN_8, SUN_9, SUN_10, SUN_11, SUN_12
    };

    public ThirteenthForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 1.0f);
        // Start immediately at the first step.
        startNextSubForm();
    }

    @Override
    public void onTick() {
        // Temporary buffs while the chain is active.
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 2));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1));
        user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 2));
        user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 1));

        // Breath drain.
        if (ticksActive % 20 == 0) {
            user.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() < BREATH_DRAIN_PER_SECOND) {
                    ActiveFormManager.stop(user);
                } else {
                    breath.subBreath(BREATH_DRAIN_PER_SECOND);
                }
            });
        }

        // Tick the current sub-form exactly once per tick.
        ticksInCurrentForm++;
        if (currentSubForm != null) {
            // IMPORTANT: use sub-form ticking so TimedForm/ChargeableForm stop() calls don't stop SUN_13.
            currentSubForm.tickAsSubForm();
        }

        // Advance when the step duration elapsed.
        if (currentFormIndex >= 0 && currentFormIndex < FORM_DURATIONS.length) {
            if (ticksInCurrentForm >= FORM_DURATIONS[currentFormIndex]) {
                startNextSubForm();
            }
        }
    }

    @Override
    public void onStop() {
        // Make sure we end any currently running sub-form cleanly.
        if (currentSubForm != null) {
            currentSubForm.onStop();
            currentSubForm = null;
        }
    }

    private void startNextSubForm() {
        // Stop previous sub-form first.
        if (currentSubForm != null) {
            currentSubForm.onStop();
            currentSubForm = null;
        }

        currentFormIndex++;
        ticksInCurrentForm = 0;

        if (currentFormIndex >= TOTAL_FORMS) {
            ActiveFormManager.stop(user);
            return;
        }

        IForm toRun = SUN_FORMS[currentFormIndex];

        // Form 1 is implemented as a simple immediate action.
        if (currentFormIndex == 0) {
            toRun.performAction(user, level);
            return;
        }

        ActiveForm created = FORM_FACTORIES[currentFormIndex].create(user, toRun);
        if (created == null) {
            ActiveFormManager.stop(user);
            return;
        }

        // Special-case: SUN_8 is a ChargeableForm and needs its release phase started.
        if (created instanceof SunflowerThrustForm thrust) {
            thrust.release(true);
        }

        this.currentSubForm = created;
        this.currentSubForm.onStart();
    }
}
