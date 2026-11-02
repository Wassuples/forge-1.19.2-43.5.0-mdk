package com.wassuples.demonslayer.forms.demon.rui;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CuttingThreadCageEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

/**
 * Rui Blood Demon Art form: Cutting Thread Cage. Throws a dome of threads around the looked-at target (or
 * the looked-at spot), which traps and contracts. All behaviour lives in {@link CuttingThreadCageEntity}.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 */
public class CuttingThreadCageForm extends TimedForm {
    private static final int DURATION = 5;
    private static final double RANGE = 16.0;

    public CuttingThreadCageForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        Vec3 targetPos = null;
        LivingEntity lookedAtEntity = getLookedAtEntity(user, RANGE);
        if (lookedAtEntity != null) {
            targetPos = lookedAtEntity.position();
        } else {
            HitResult hit = user.pick(RANGE, 0, false);
            if (hit.getType() != HitResult.Type.MISS) {
                targetPos = hit.getLocation();
            }
        }

        if (targetPos == null) {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.8f, 0.6f);
            ActiveFormManager.stop(user);
            return;
        }

        CuttingThreadCageEntity cage = ModEntityTypes.CUTTING_THREAD_CAGE.get().create(level);
        if (cage != null) {
            cage.setOwner(user);
            cage.placeOnGround(targetPos);
            level.addFreshEntity(cage);
            level.playSound(null, cage.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.2f, 1.4f);
            level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}
