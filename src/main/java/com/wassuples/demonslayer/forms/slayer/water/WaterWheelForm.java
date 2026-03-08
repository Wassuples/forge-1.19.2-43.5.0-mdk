package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

/**
 * Water Breathing form: Water Wheel.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WaterWheelForm extends TimedForm {
    public WaterWheelForm(LivingEntity user, IForm form) {
        super(user, form, 10);
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        Vec3 lookVec = user.getLookAngle();
        Vec3 forwardMotion = new Vec3(lookVec.x, 0, lookVec.z).normalize().scale(0.8);
        user.setDeltaMovement(user.getDeltaMovement().add(forwardMotion.x, 0.8, forwardMotion.z));
        user.hurtMarked = true;

        Vec3 playerCenter = user.position().add(0, user.getBbHeight() / 2.0, 0);
        double radius = 2.2;
        int particleCount = 60;

        Vec3 forwardVector = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI / particleCount) * i;
            Vec3 verticalOffset = new Vec3(0, 1, 0).scale(radius * Math.cos(angle));
            Vec3 longitudinalOffset = forwardVector.scale(radius * Math.sin(angle));
            Vec3 particlePos = playerCenter.add(verticalOffset).add(longitudinalOffset);
            level.sendParticles(ParticleTypes.SPLASH, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0.05);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0.05);
            }
        }
        dealAOEDamage(user, user.position(), 5.0, 8.0f, 0.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}
