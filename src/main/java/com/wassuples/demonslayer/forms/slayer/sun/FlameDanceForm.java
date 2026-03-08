package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Sun Breathing form: Flame Dance.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FlameDanceForm extends TimedForm {
    private static final int VERTICAL_SLASH_TICK = 4;
    private static final int HORIZONTAL_SLASH_TICK = 10;

    private static final float VERTICAL_DAMAGE = 20.0f;
    private static final float HORIZONTAL_DAMAGE = 35.0f;
    private static final float VERTICAL_KNOCKBACK = 1.2f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    public FlameDanceForm(LivingEntity user, IForm form) {
        super(user, form, HORIZONTAL_SLASH_TICK + 5);
    }

    @Override
    public boolean isInterruptible() { return false; }

    @Override
    public void onTick() {
        super.onTick();

        if (this.ticksActive == VERTICAL_SLASH_TICK) {
            level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 1.0f);

            dealDirectionalDamage(user, 5.0, VERTICAL_DAMAGE, 0.9);
            spawnParticleArc(user, RED_HOT, 4.0, 160, 40, 90.0f);
        }

        if (this.ticksActive == HORIZONTAL_SLASH_TICK) {
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);

            dealAOEDamage(user, user.position(), 6.0, HORIZONTAL_DAMAGE, 0.5f, true, false);
            spawnParticleArcInWorld(level, user.position().add(0, 1.2, 0), 6.0, 360, 80, 0, 90, RED_HOT);
        }
    }
}
