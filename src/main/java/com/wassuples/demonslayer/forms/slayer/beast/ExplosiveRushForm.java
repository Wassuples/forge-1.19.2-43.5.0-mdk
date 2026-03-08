package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

/**
 * Beast Breathing form: Explosive Rush.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ExplosiveRushForm extends TimedForm {
    private static final int DURATION = 20;
    private static final double DASH_SPEED = 2.5;
    private static final float DAMAGE_PER_TICK = 3.0f;
    private static final double DAMAGE_RADIUS = 2.5;

    private final Vec3 direction;

    public ExplosiveRushForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.2f, 1.0f);
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION + 5, 4, false, false, true));
    }

    @Override
    public void onTick() {
        super.onTick();

        user.setDeltaMovement(this.direction.scale(DASH_SPEED));
        user.hurtMarked = true;

        dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.3f);

        level.sendParticles(ParticleTypes.POOF, user.getX(), user.getY() + 1, user.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1, user.getZ(), 10, 0.5, 0.5, 0.5, 0.2);

    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}
