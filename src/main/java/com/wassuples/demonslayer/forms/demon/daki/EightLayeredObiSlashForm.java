package com.wassuples.demonslayer.forms.demon.daki;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.custom.ObiSashEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.util.FormAnimations;
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
    public void onStart() {
        // Eight sashes from the obi bow at Daki's back; each reaches full length on its damage tick.
        level.addFreshEntity(new ObiSashEntity(level, user, (float) RANGE));
        FormAnimations.play(user, FormAnimations.DAKI_OBI);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
            // Visuals are drawn by ObiSashEntity; a few silk shreds where the slash ends.
            Vec3 end = user.getEyePosition().add(user.getLookAngle().scale(RANGE));
            level.sendParticles(OBI_PINK, end.x, end.y, end.z, 4, 0.3, 0.3, 0.3, 0.02);
            dealThrustDamage(user, RANGE, DAMAGE_PER_SLASH);
        }
    }
}
