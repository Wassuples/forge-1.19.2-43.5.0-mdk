package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.effects.HeatLightningEffectData;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.HeatLightningCracksS2CPacket;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;

/**
 * Thunder Breathing 5: Heat Lightning
 *
 * Design constraints:
 * - Must be usable without a target.
 * - Targets any LivingEntity (not players-only).
 * - Visuals are NOT particles; it's a visible-to-everyone cracked-skin overlay.
 * - Big burst damage + long DoT via MobEffect + short stun (immobile + blindness).
 */
public class HeatLightningForm extends TimedForm {

    // Short stance / windup before the slash resolves.
    private static final int DURATION = 12;

    private static final float BURST_DAMAGE = 22.0f;
    private static final int DOT_DURATION_TICKS = 20 * 8;
    private static final int DOT_AMPLIFIER = 0;
    private static final int STUN_TICKS = 40;
    private static final double LOOK_TARGET_RANGE = 6.0;

    private static final double FRONT_STRIKE_DIST = 3.0;
    private static final double FRONT_RADIUS = 2.6;
    private static final double FRONT_Y = 1.5;

    private boolean struck = false;

    public HeatLightningForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        // Light windup; user can still move.
        level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive == 3) {
            // Add a small telegraph visual during windup
            DustParticleOptions yellow = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);
            level.sendParticles(yellow, user.getX(), user.getY() + 1.0, user.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        }

        if (!struck && ticksActive >= 6) { // use ticksActive from parent class
            struck = true;
            doStrike();
        }
    }

    private void doStrike() {
        // Slash sound on user
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.6f);

        // Visual slash effect from user
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.6, 0);
        Vec3 look = user.getLookAngle().normalize();
        Vec3 endPos = userPos.add(look.scale(4.0));

        // Upward arc simulation (particles)
        DustParticleOptions arcColor = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.6f), 1.0f);
        for (int i = 0; i < 15; i++) {
            double t = i / 15.0;
            double yOffset = t * 2.0; // rising motion
            Vec3 p = userPos.add(look.scale(t * 3.0)).add(0, yOffset, 0);
            level.sendParticles(arcColor, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.05);
        }

        LivingEntity looked = getLookedAtEntity(user, LOOK_TARGET_RANGE);

        List<LivingEntity> targets;
        if (looked != null && looked.isAlive() && looked != user) {
            targets = List.of(looked);
        } else {
            Vec3 center = user.position().add(0, user.getBbHeight() * 0.6, 0).add(user.getLookAngle().normalize().scale(FRONT_STRIKE_DIST));
            AABB box = new AABB(center, center).inflate(FRONT_RADIUS, FRONT_Y, FRONT_RADIUS);
            targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != user);
        }

        // Even if no targets, the form is still valid.
        if (targets.isEmpty()) {
            return;
        }

        for (LivingEntity target : targets) {
            // Burst
            target.hurt(getDamageSource(user, false, true), calculateFinalDamage(user, BURST_DAMAGE));

            // DoT as MobEffect + caster attribution
            HeatLightningEffectData.setCaster(target, user);
            target.addEffect(new MobEffectInstance(ModEffects.HEAT_LIGHTNING.get(), DOT_DURATION_TICKS, DOT_AMPLIFIER, false, false, true));

            // Stun: immobile + blind
            applyImmobility(target, STUN_TICKS);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUN_TICKS, 0, false, false, true));

            // Make cracks visible to everyone tracking that entity.
            if (!level.isClientSide && level instanceof ServerLevel) {
                ModMessages.sendToClients(new HeatLightningCracksS2CPacket(target.getId(), DOT_DURATION_TICKS));
            }

            // Hit particles on target
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getEyeY(), target.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY(), target.getZ(), 15, 0.5, 0.5, 0.5, 0.2);

            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    @Override
    public void onStop() {
        // no-op
    }
}
