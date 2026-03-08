package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Sun Breathing form: Sun Halo Dragon.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SunHaloDragonForm extends ActiveForm {
    private static final int BREATH_COST_PER_TICK = 2;
    private static final float DAMAGE_PER_TICK = 5.0f;
    private static final double DRAGON_BODY_RADIUS = 2.5;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);
    private static final ParticleOptions EMBERS = ParticleTypes.LAVA;

    private final Deque<Vec3> trailPositions = new ArrayDeque<>();
    private static final int TRAIL_LENGTH = 30;

    public SunHaloDragonForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2f, 1.2f);
    }

    @Override
    public void onTick() {
        // Require sprinting ONLY when this form is running as the player's active form.
        // When used as a sub-step (e.g., during SUN_13), we must not stop the parent chain.
        boolean isStandaloneActive = ActiveFormManager.getActiveForm(user) == this;
        if (isStandaloneActive && !user.isSprinting()) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive % 10 == 0) {
            user.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() >= BREATH_COST_PER_TICK) {
                    breath.subBreath(BREATH_COST_PER_TICK);
                } else if (isStandaloneActive) {
                    // If we're the actual active form, stop. If we're a sub-form, parent handles breath rules.
                    ActiveFormManager.stop(user);
                }
            });
        }

        this.trailPositions.add(user.position());
        if (this.trailPositions.size() > TRAIL_LENGTH) {
            this.trailPositions.removeFirst();
        }

        Set<LivingEntity> hitThisTick = new HashSet<>();
        AABB headAABB = user.getBoundingBox().inflate(DRAGON_BODY_RADIUS);
        List<LivingEntity> headTargets = level.getEntitiesOfClass(LivingEntity.class, headAABB, e -> e != user && e.isAlive());
        hitThisTick.addAll(headTargets);

        for (Vec3 trailPos : this.trailPositions) {
            AABB bodyAABB = new AABB(trailPos, trailPos).inflate(DRAGON_BODY_RADIUS);
            List<LivingEntity> bodyTargets = level.getEntitiesOfClass(LivingEntity.class, bodyAABB, e -> e != user && e.isAlive());
            hitThisTick.addAll(bodyTargets);
        }

        DamageSource damageSource = (user instanceof Player p) ? DamageSource.playerAttack(p).setIsFire() : DamageSource.mobAttack(user).setIsFire();
        for (LivingEntity target : hitThisTick) {
            target.hurt(damageSource, DAMAGE_PER_TICK);
        }

        for (Vec3 pos : this.trailPositions) {
            level.sendParticles(RED_HOT, pos.x, pos.y + 1.0, pos.z, 2, 0.5, 0.5, 0.5, 0.0);
            if (level.random.nextFloat() < 0.2f) {
                level.sendParticles(EMBERS, pos.x, pos.y + 1.0, pos.z, 1, 0.5, 0.5, 0.5, 0.0);
            }
        }
    }

    @Override
    public void onStop() {
        level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
