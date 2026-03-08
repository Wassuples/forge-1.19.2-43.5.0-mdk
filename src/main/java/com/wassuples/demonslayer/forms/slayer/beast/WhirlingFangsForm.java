package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Beast Breathing form: Whirling Fangs.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WhirlingFangsForm extends ActiveForm {
    private static final double DEFENSE_RADIUS = 3.0;
    private static final float DAMAGE_PER_TICK = 2.5f;
    private static final int BREATH_COST_PER_TICK = 1;

    private float rotationAngle = 0;

    public WhirlingFangsForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        if (ticksActive % 4 == 0) {
            user.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() >= BREATH_COST_PER_TICK) {
                    breath.subBreath(BREATH_COST_PER_TICK);
                } else {
                    ActiveFormManager.stop(user);
                }
            });
        }

        AABB defenseBox = user.getBoundingBox().inflate(DEFENSE_RADIUS);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, defenseBox,
                p -> p.getOwner() != user && !(p.getOwner() instanceof TamableAnimal tamable && tamable.getOwner() == user));

        for (Projectile projectile : projectiles) {
            level.playSound(null, projectile.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 2.0f);
            level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 15, 0.2, 0.2, 0.2, 0.5);
            projectile.discard();
        }

        if (ticksActive % 5 == 0) {
            dealAOEDamage(user, user.position(), DEFENSE_RADIUS, DAMAGE_PER_TICK, 0.3f);
        }

        this.rotationAngle += 50.0f;
        spawnParticleArcInWorld(level, user.getEyePosition(), DEFENSE_RADIUS - 0.5, 160, 10, this.rotationAngle, 0, ParticleTypes.SWEEP_ATTACK);
        spawnParticleArcInWorld(level, user.getEyePosition(), DEFENSE_RADIUS - 0.5, 160, 10, -this.rotationAngle, 0, ParticleTypes.CRIT);
    }
}
