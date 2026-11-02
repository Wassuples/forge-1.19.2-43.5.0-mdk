package com.wassuples.demonslayer.forms.demon.rui;

import com.wassuples.demonslayer.entity.custom.ThreadBasketEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

/**
 * Rui Blood Demon Art form: Murderous Eye Basket. Rui strings a cat's cradle between his hands; a circle of
 * blood threads opens under the target's feet, rises into a sphere around them, and shrinks until it cuts.
 * All behaviour lives in {@link ThreadBasketEntity}; the form ends immediately.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 */
public class MurderousEyeBasketForm extends ActiveForm {
    private static final double MAX_RANGE = 20.0;

    public MurderousEyeBasketForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        Vec3 targetPos = null;
        LivingEntity lookedAtEntity = getLookedAtEntity(user, MAX_RANGE);
        if (lookedAtEntity != null) {
            targetPos = lookedAtEntity.position();
        } else {
            HitResult hit = user.pick(MAX_RANGE, 0, false);
            if (hit.getType() != HitResult.Type.MISS) {
                targetPos = hit.getLocation();
            }
        }

        if (targetPos != null) {
            ThreadBasketEntity basket = new ThreadBasketEntity(level);
            basket.setOwner(user);
            basket.placeOnGround(targetPos);
            // Sphere centred on the target's body; the threads hold them while the circle rises.
            if (lookedAtEntity != null) {
                basket.setCentreHeight(lookedAtEntity.getBbHeight() * 0.5f);
                lookedAtEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, basket.closeTick(), 3, false, false, true));
            }
            FormAnimations.play(user, FormAnimations.RUI_CRADLE);
            level.addFreshEntity(basket);
            level.playSound(null, basket.blockPosition(), SoundEvents.SPIDER_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, basket.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.4f, 0.5f);
        } else {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
