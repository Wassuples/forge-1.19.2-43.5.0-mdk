package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.WorldHelper.destroyBlocksInFrontCone;

/**
 * Thunder Breathing form: Distant Thunder.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class DistantThunderForm extends TimedForm {
    // Casting window. Player can move while casting.
    private static final int DURATION = 30; // 1.5s

    // Zone placement
    private static final double ZONE_FORWARD_OFFSET = 3.0;

    // Damage tuning: "HUGE damage" via repeated pulses (more consistent than one massive hit)
    private static final int PULSE_INTERVAL_TICKS = 4;
    private static final int PULSE_COUNT = 4; // total pulses = 4 over duration
    private static final double PULSE_RADIUS = 3.0;
    private static final float DAMAGE_PER_PULSE = 16.0f;
    private static final float KNOCKBACK = 1.2f;

    // Terrain destruction
    private static final double BREAK_RANGE = 4.0;
    private static final int BREAK_VERTICAL_RADIUS = 2;
    private static final float MAX_BREAK_HARDNESS = 6.0f;
    private static final float BREAK_CHANCE = 0.35f;
    private static final double MIN_FORWARD_DOT = 0.35; // ~70° cone

    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    public DistantThunderForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    private Vec3 getZoneCenter() {
        // Always in front of the user (updates as they rotate/move while casting).
        Vec3 look = user.getLookAngle().normalize();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = new Vec3(0, 0, 1);
        } else {
            horizontal = horizontal.normalize();
        }

        // Center the zone around chest-height so it catches mobs/players.
        return user.position().add(0, user.getBbHeight() * 0.6, 0).add(horizontal.scale(ZONE_FORWARD_OFFSET));
    }

    private void spawnZoneVfx(Vec3 center, boolean isPulseTick) {
        // A constant "charged" ring + heavier burst on pulse ticks.
        level.sendParticles(YELLOW_LIGHTNING, center.x, center.y, center.z, 6, 0.6, 0.6, 0.6, 0.0);

        if (level.random.nextFloat() < 0.6f) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 8, 0.8, 0.8, 0.8, 0.05);
        }

        if (isPulseTick) {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.2, 0.2, 0.2, 0.0);
            // A little extra density to sell "multitude of slashes"
            level.sendParticles((ParticleOptions) YELLOW_LIGHTNING, center.x, center.y, center.z, 25, 1.0, 1.0, 1.0, 0.02);
        }
    }

    @Override
    public void onStart() {
        // No forced movement / immobility: user can reposition while casting.
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();


        Vec3 center = getZoneCenter();
        boolean isPulseTick = false;

        // Pulses: big damage + block break. We cap to PULSE_COUNT pulses.
        int pulseIndex = (ticksActive - 1) / PULSE_INTERVAL_TICKS;
        if ((ticksActive - 1) % PULSE_INTERVAL_TICKS == 0 && pulseIndex < PULSE_COUNT) {
            isPulseTick = true;

            level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9f, 1.4f);

            dealAOEDamage(user, center, PULSE_RADIUS, DAMAGE_PER_PULSE, KNOCKBACK, false, true);

            // Break only some blocks in front of the user; never under them; and never behind a blocker.
            destroyBlocksInFrontCone(
                    level,
                    user.position(),
                    user.getLookAngle(),
                    BREAK_RANGE,
                    BREAK_VERTICAL_RADIUS,
                    true,
                    user,
                    MAX_BREAK_HARDNESS,
                    BREAK_CHANCE,
                    MIN_FORWARD_DOT
            );
        }

        spawnZoneVfx(center, isPulseTick);
    }

    @Override
    public void onStop() {
        Vec3 center = getZoneCenter();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
    }
}
