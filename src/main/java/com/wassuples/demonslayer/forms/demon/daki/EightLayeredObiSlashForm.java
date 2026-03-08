package com.wassuples.demonslayer.forms.demon.daki;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealThrustDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

/**
 * Daki Blood Demon Art form: Eight Layered Obi Slash.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class EightLayeredObiSlashForm extends TimedForm {
    private static final int NUMBER_OF_SLASHES = 8;
    private static final int TICKS_BETWEEN_SLASHES = 4; // Slightly slower than mist for a heavier feel
    private static final float DAMAGE_PER_SLASH = 5.0f; // Total damage = 40
    private static final double RANGE = 12.0; // Long-ranged attack

    private static final DustParticleOptions OBI_PINK = new DustParticleOptions(new Vector3f(0.9f, 0.4f, 0.8f), 1.0f);
    private static final DustParticleOptions OBI_PATTERN = new DustParticleOptions(new Vector3f(0.1f, 0.1f, 0.1f), 1.2f);


    public EightLayeredObiSlashForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);

            final int segments = 20;
            Vec3 eyePos = user.getEyePosition();
            Vec3 lookDir = user.getLookAngle();
            Vec3 origin = eyePos.subtract(lookDir.scale(1.5));
            Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

            for (int i = 0; i <= segments; i++) {
                double progress = (double) i / segments;
                Vec3 pathPoint = origin.add(lookDir.scale(progress * RANGE));

                double wind_amplitude = 1.2;
                double wind_frequency = 2.0 + level.random.nextDouble();
                double windOffset = Math.sin(progress * Math.PI * wind_frequency) * wind_amplitude;
                Vec3 particlePos = pathPoint.add(sideDir.scale(windOffset));

                level.sendParticles(OBI_PINK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                if (i % 3 == 0) {
                    level.sendParticles(OBI_PATTERN, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            }
            dealThrustDamage(user, RANGE, DAMAGE_PER_SLASH);
        }
    }
}