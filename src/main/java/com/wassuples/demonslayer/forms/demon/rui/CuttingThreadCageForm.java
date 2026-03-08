package com.wassuples.demonslayer.forms.demon.rui;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CuttingThreadCageEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

/**
 * Rui Blood Demon Art form: Cutting Thread Cage.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CuttingThreadCageForm extends TimedForm {
    private static final int DURATION = 5; // A very short form, as the cage does the work

    public CuttingThreadCageForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        Vec3 targetPos = null;
        LivingEntity lookedAtEntity = getLookedAtEntity(user, 16.0D);

        if (lookedAtEntity != null) {
            targetPos = lookedAtEntity.position();
        } else {
            BlockHitResult hitResult = (BlockHitResult) user.pick(16.0D, 0, false);
            if (hitResult.getType() != HitResult.Type.MISS) {
                targetPos = hitResult.getLocation();
            }
        }

        if (targetPos != null) {
            user.level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.8F);

            CuttingThreadCageEntity cage = ModEntityTypes.CUTTING_THREAD_CAGE.get().create(level);
            if (cage != null) {
                cage.setPos(targetPos);
                level.addFreshEntity(cage);
            }
        } else {
            ActiveFormManager.stop(user);
        }
    }

    @Override
    public void onTick() {
        super.onTick(); // Handles stopping the form
    }
}
