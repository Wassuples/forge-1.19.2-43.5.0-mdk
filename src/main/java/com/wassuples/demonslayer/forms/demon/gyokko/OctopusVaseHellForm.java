package com.wassuples.demonslayer.forms.demon.gyokko;


import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.OctopusPotEntity;
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
 * Gyokko Blood Demon Art form: Octopus Vase Hell.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class OctopusVaseHellForm extends ActiveForm {
    private static final double MAX_SPAWN_RANGE = 20.0;

    public OctopusVaseHellForm(LivingEntity user, IForm form) {
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
        OctopusPotEntity pot = ModEntityTypes.OCTOPUS_POT.get().create(level);
        if (pot != null) {
            pot.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            pot.setOwner(user);
            level.addFreshEntity(pot);
            level.playSound(null, spawnPos, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 2.0f, 0.7f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
