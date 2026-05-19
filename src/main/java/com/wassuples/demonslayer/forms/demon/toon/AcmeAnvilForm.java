package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.CombatHelper;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class AcmeAnvilForm extends TimedForm {
    private static final int WHISTLE_TICK = 10;
    private static final int IMPACT_TICK = 20;
    private static final int TOTAL_TICKS = 24;
    private static final float DAMAGE = 10.0f;
    private LivingEntity target;

    public AcmeAnvilForm(LivingEntity user, IForm formType) {
        super(user, formType, TOTAL_TICKS);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.target = CombatHelper.getLookedAtEntity(user, 12.0);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (target == null || !target.isAlive()) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive == WHISTLE_TICK) {
            level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 2.0f);
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getEyeY() + 1.2, target.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
        }

        if (ticksActive == IMPACT_TICK) {
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.8f);
            target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, DAMAGE));
            ToonForceHelper.applyFlattened(target, 40);
            ToonForceHelper.tryApplyDizzy(target, user, 0.35f);
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getEyeY(), target.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        }
    }
}

