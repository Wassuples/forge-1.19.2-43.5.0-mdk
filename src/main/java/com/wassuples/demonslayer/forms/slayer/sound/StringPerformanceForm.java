package com.wassuples.demonslayer.forms.slayer.sound;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.rotateVec;

/**
 * Sound Breathing form: String Performance.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class StringPerformanceForm extends TimedForm {
    private static final int DURATION = 60; // 3 seconds of charging
    private static final float FORWARD_SPEED = 1.3f;
    private static final float DAMAGE = 3.5f;
    private static final double RADIUS = 3.0;
    private static final float KNOCKBACK = 0.4f;

    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("a8a0e842-7a88-4a41-8f43-059935677a21");


    private float rotationAngle = 0;

    public StringPerformanceForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            attribute.removeModifier(KNOCKBACK_RESISTANCE_UUID);
            AttributeModifier modifier = new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "String Performance Knockback Immunity", 1.0, AttributeModifier.Operation.ADDITION);
            attribute.addTransientModifier(modifier);
        }

        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        Vec3 lookVec = user.getLookAngle();
        user.setDeltaMovement(lookVec.x * FORWARD_SPEED, user.getDeltaMovement().y, lookVec.z * FORWARD_SPEED);
        user.hurtMarked = true;
        dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK);

        this.rotationAngle += 45.0f;
        Vec3 discCenter = user.getEyePosition().add(user.getLookAngle().scale(1.5));

        // Spawn a ring of particles.
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * (360.0 / 6) + this.rotationAngle);
            Vec3 particleOffset = new Vec3(Math.cos(angle) * (RADIUS - 1.0), Math.sin(angle) * (RADIUS - 1.0), 0);
            particleOffset = rotateVec(particleOffset, user.getXRot(), user.getYRot());
            Vec3 particlePos = discCenter.add(particleOffset);

            level.sendParticles(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onStop() {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            // We remove the specific modifier we added by its unique ID.
            attribute.removeModifier(KNOCKBACK_RESISTANCE_UUID);
        }
    }
}
