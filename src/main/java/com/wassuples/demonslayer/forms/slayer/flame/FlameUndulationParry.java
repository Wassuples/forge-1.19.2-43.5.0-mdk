package com.wassuples.demonslayer.forms.slayer.flame;

import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class FlameUndulationParry extends ParryForm {
    public FlameUndulationParry(LivingEntity user, IForm form) {
        super(user, form, 30);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        applyImmobility(user, 10);
        performGenericAttack(user, SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
        dealAOEDamage(user, user.position(), 4.0, 10.0f, 0.8f);
        spawnParticleArc(user, ParticleTypes.FLAME, 4.0, 360, 60);
        level.sendParticles(ParticleTypes.LAVA, user.getX(), user.getY() + 0.5, user.getZ(), 20, 1.5, 0.5, 1.5, 0);
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
