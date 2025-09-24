package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class SliceNDiceForm extends TimedForm {
    private static final int NUMBER_OF_HITS = 4;
    private static final int TICKS_PER_HIT = 6; // A very fast combo
    private static final float DAMAGE_PER_HIT = 9.0f;
    private static final double ADVANCE_SPEED = 0.6;

    public SliceNDiceForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_HITS * TICKS_PER_HIT);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.HOGLIN_ANGRY, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_PER_HIT == 0) {
            Vec3 forward = user.getLookAngle().scale(ADVANCE_SPEED);
            user.setDeltaMovement(forward.x, 0, forward.z);
            user.hurtMarked = true;

            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);

            dealDirectionalDamage(user, 5.0, DAMAGE_PER_HIT, 0.7);

            float roll = (ticksActive / TICKS_PER_HIT) % 2 == 0 ? 45.0f : -45.0f;

            spawnParticleArc(user, ParticleTypes.CRIT, 3.0, 140, 20, roll);
            Vec3 sideOffset = user.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.5);
            spawnParticleArcInWorld(level, user.getEyePosition().add(sideOffset), 3.0, 140, 20, user.getYRot(), user.getXRot() - roll, ParticleTypes.CRIT);
        }
    }
}
