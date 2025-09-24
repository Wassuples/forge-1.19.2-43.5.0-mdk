package com.wassuples.demonslayer.forms.slayer.insect;


import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

public class DragonflyDanceForm extends TimedForm {
    // --- Constants ---
    private static final int STRIKES = 6;
    private static final int TICKS_PER_STRIKE = 3; // A very fast stab every 0.15 seconds
    private static final double HEXAGON_DISTANCE = 1.5; // How far in front of the player the hexagon is
    private static final double HEXAGON_RADIUS = 0.6;
    private static final float DAMAGE_PER_STRIKE = 2.0f;
    private static final int POISON_DURATION = 60; // 3 seconds per sting
    private static final int POISON_AMPLIFIER = 0; // Wisteria Poison I

    // --- State ---
    private int strikeIndex = 0;

    public DragonflyDanceForm(LivingEntity user, IForm form) {
        super(user, form, STRIKES * TICKS_PER_STRIKE + 2);
    }

    @Override
    public void onTick() {
        super.onTick(); // Handles stopping the form

        // Trigger a strike every few ticks
        if (ticksActive > 0 && ticksActive % TICKS_PER_STRIKE == 0 && strikeIndex < STRIKES) {

            // --- NEW: Calculate the strike position dynamically ---
            // 1. Get the player's CURRENT position and view direction.
            Vec3 playerLook = user.getLookAngle();
            Vec3 playerEyePos = user.getEyePosition();

            // 2. Calculate the center of the hexagon in front of the player.
            Vec3 hexagonCenter = playerEyePos.add(playerLook.scale(HEXAGON_DISTANCE));
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 side = playerLook.cross(up).normalize();

            // 3. Calculate the angle for the CURRENT strike in the hexagon pattern.
            double angle = (2 * Math.PI / STRIKES) * this.strikeIndex;
            Vec3 strikePos = hexagonCenter.add(
                    side.scale(Math.cos(angle) * HEXAGON_RADIUS)
            ).add(
                    up.scale(Math.sin(angle) * HEXAGON_RADIUS)
            );
            user.swing(InteractionHand.MAIN_HAND);

            dealAOEDamage(user, strikePos, 0.8, DAMAGE_PER_STRIKE, 0.1f);

            List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, new AABB(strikePos, strikePos).inflate(0.8));
            for (LivingEntity target : hitTargets) {
                if (target != user) {
                    target.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));
                }
            }

            level.playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 2.0f);
            level.sendParticles(ParticleTypes.CRIT, strikePos.x, strikePos.y, strikePos.z, 15, 0.1, 0.1, 0.1, 0.5);

            this.strikeIndex++;
        }
    }
}
