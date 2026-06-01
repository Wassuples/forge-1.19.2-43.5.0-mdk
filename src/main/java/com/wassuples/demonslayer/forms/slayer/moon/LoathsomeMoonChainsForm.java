package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamageInYaw;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Loathsome Moon, Chains.
 */
public class LoathsomeMoonChainsForm extends TimedForm {
    // Moon3: A deliberate pause of silence before the chain of continuous cuts erupts.
    private static final int WINDUP_TICKS = 25;  // ~1.25s of eerie stillness
    private static final int CUT_DURATION = 50;  // longer continuous cut phase
    private static final int CUT_INTERVAL = 2;   // faster, more continuous cuts
    private static final double RANGE = 8.5;
    private static final float DAMAGE_PER_CUT = 7.0f;

    private static final DustParticleOptions PURPLE_SLASH =
            new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.3f);

    public LoathsomeMoonChainsForm(LivingEntity user, IForm form) {
        super(user, form, WINDUP_TICKS + CUT_DURATION);
    }

    @Override
    public void onStart() {
        // The brief silence before the storm — a single ominous sound, then nothing.
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SKELETON_HURT, SoundSource.PLAYERS, 0.8f, 0.65f);
    }

    @Override
    public void onTick() {
        super.onTick();

        // During the pause: faint ambient particles build tension, but no sound and no damage.
        if (this.ticksActive < WINDUP_TICKS) {
            if (this.ticksActive % 5 == 0) {
                Vec3 center = user.position().add(0, user.getBbHeight() * 0.5, 0);
                for (int i = 0; i < 3; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double r = 0.6 + level.random.nextDouble() * 1.2;
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            center.x + Math.cos(angle) * r, center.y + level.random.nextDouble() * 0.5, center.z + Math.sin(angle) * r,
                            1, 0.05, 0.05, 0.05, 0.005);
                }
            }
            return;
        }

        int cutTick = this.ticksActive - WINDUP_TICKS;
        if (cutTick % CUT_INTERVAL != 0) {
            return;
        }

        float baseYaw = user.getYRot();
        float yawOffset = switch ((cutTick / CUT_INTERVAL) % 5) {
            case 0 -> -22.0f;
            case 1 -> 12.0f;
            case 2 -> 28.0f;
            case 3 -> -10.0f;
            default -> 0.0f;
        };

        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85f, 0.9f + level.random.nextFloat() * 0.25f);
        dealDirectionalDamageInYaw(user, RANGE, DAMAGE_PER_CUT, 0.28f, baseYaw + yawOffset);

        Vec3 center = user.position().add(0, user.getBbHeight() * 0.75, 0);
        spawnParticleArcInWorld(level, center.add(0, 0.55, 0), 4.8, 150, 42, baseYaw + yawOffset, 8, PURPLE_SLASH);
        spawnParticleArcInWorld(level, center.add(0, -0.25, 0), 4.2, 130, 36, baseYaw - yawOffset * 0.6f, -8, PURPLE_SLASH);

        for (int i = 0; i < 4; i++) {
            Vec3 sparkle = center.add(
                    (level.random.nextDouble() - 0.5) * 5.0,
                    (level.random.nextDouble() - 0.5) * 1.8,
                    (level.random.nextDouble() - 0.5) * 5.0
            );
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, sparkle.x, sparkle.y, sparkle.z, 1, 0, 0, 0, 0);
        }
    }
}
