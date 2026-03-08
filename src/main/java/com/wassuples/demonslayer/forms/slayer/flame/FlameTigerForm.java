package com.wassuples.demonslayer.forms.slayer.flame;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.isPlayerHoldingNichirin;

/**
 * Flame Breathing form: Flame Tiger.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FlameTigerForm extends ActiveForm {
    private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 1.0f);
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.1f, 0.1f), 1.2f);

    public FlameTigerForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    public void onTick() {
        if (!user.isSprinting() || (user instanceof Player player && !isPlayerHoldingNichirin(player))) {
            ActiveFormManager.stop(user);
            return;
        }

        int speedAmplifier = Math.min(ticksActive / 40, 4);
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, speedAmplifier, false, false, false));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, ticksActive / 60, false, false));
        user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));

        if (ticksActive % 15 == 0) {
            float damage = 3.0f + (ticksActive / 50f);
            dealAOEDamage(user, user.position(), 2.5, damage, 0.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        // Particle aura
        int particleCount = Math.min(10 + (ticksActive / 20), 40);
        for (int i = 0; i < particleCount; i++) {
            double angle = (user.tickCount + i * 22) * 0.08;
            double r_x = Math.cos(angle) * (1.2 + (Math.random() - 0.5) * 0.4);
            double r_z = Math.sin(angle) * (1.2 + (Math.random() - 0.5) * 0.4);
            double yOffset = Math.random() * 2.0;
            level.sendParticles(i % 4 == 0 ? BLACK_DUST : ORANGE_DUST, user.getX() + r_x, user.getY() + yOffset, user.getZ() + r_z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        user.removeEffect(MobEffects.DAMAGE_BOOST);
        user.removeEffect(MobEffects.FIRE_RESISTANCE);
        level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.0f);
    }
}
