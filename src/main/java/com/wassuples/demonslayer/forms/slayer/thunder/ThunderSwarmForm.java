package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Thunder Breathing form: Thunder Swarm.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ThunderSwarmForm extends TimedForm {
    private static final int NUMBER_OF_DASHES = 8;
    private static final int TICKS_PER_PHASE = 4; // An attack every 0.2 seconds
    private static final double DASH_DISTANCE = 3.0;
    private static final float DAMAGE_PER_HIT = 7.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    private LivingEntity target;

    public ThunderSwarmForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_DASHES * TICKS_PER_PHASE);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        this.target = getLookedAtEntity(user, 16.0);

        if (this.target == null) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            ActiveFormManager.stop(user);
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (target == null || !target.isAlive() || target.distanceTo(user) > 32) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive > 0 && ticksActive % TICKS_PER_PHASE == 0) {
            // Generate a random 3D position around the target
            Vec3 teleportPos;
            int attempts = 0;
            do {
                // Generate a random direction.
                double x = level.random.nextGaussian();
                double y = level.random.nextGaussian();
                double z = level.random.nextGaussian();
                Vec3 randomDir = new Vec3(x, y, z).normalize();

                // Calculate the position around the target's center (their eyes).
                teleportPos = target.getEyePosition().add(randomDir.scale(DASH_DISTANCE));
                attempts++;
            } while (!level.getBlockState(new BlockPos(teleportPos)).isAir() && attempts < 5); // Safety check

            // The Blink
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getX(), user.getY() + 1, user.getZ(), 30, 0.3, 0.5, 0.3, 0.5);
            user.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getX(), user.getY() + 1, user.getZ(), 30, 0.3, 0.5, 0.3, 0.5);

            // The Attack
            user.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
            dealDirectionalDamage(user, 4.0, DAMAGE_PER_HIT, 0.85);
            spawnParticleArc(user, YELLOW_LIGHTNING, 3.0, 120, 15);
        }
    }
}
