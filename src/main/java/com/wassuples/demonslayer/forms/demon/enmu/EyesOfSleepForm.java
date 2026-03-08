package com.wassuples.demonslayer.forms.demon.enmu;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.MotherEyeEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Enmu Blood Demon Art form: Eyes Of Sleep.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class EyesOfSleepForm extends ActiveForm {
    private static final double MAX_SPAWN_RANGE = 20.0;

    public EyesOfSleepForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_SPAWN_RANGE, 0, false);
        if (hitResult.getType() == HitResult.Type.MISS) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            ActiveFormManager.stop(user);
            return;
        }

        BlockPos spawnPos = hitResult.getBlockPos().above();

        MotherEyeEntity motherEye = ModEntityTypes.MOTHER_EYE.get().create(level);
        if (motherEye != null) {
            motherEye.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            motherEye.setCaster(user);
            level.addFreshEntity(motherEye);
            user.startRiding(motherEye, true);

            level.playSound(null, spawnPos, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.5f, 0.8f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
