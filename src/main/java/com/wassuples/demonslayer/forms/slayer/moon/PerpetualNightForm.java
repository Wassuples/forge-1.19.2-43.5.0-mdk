package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnCycloneParticles;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Perpetual Night.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class PerpetualNightForm extends TimedForm {
    private static final int DURATION = 100;
    private static final double MAX_RANGE = 20.0;
    private static final double STORM_RADIUS = 5.0;
    private static final float DAMAGE_PER_TICK = 4.0f;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
    private static final ParticleOptions SILVER_SPARKLE = ParticleTypes.CRIT;

    private final Vec3 stormCenter;

    public PerpetualNightForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);

        BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
        if (hitResult.getType() == HitResult.Type.MISS) {
            Vec3 endPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
            this.stormCenter = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(endPoint)));
        } else {
            this.stormCenter = Vec3.atCenterOf(hitResult.getBlockPos());
        }
    }

    @Override
    public void onStart() {
        level.playSound(null, this.stormCenter.x, this.stormCenter.y, this.stormCenter.z, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive % 8 == 0) {
            dealAOEDamage(user, this.stormCenter, STORM_RADIUS, DAMAGE_PER_TICK, 0.1f);
            level.playSound(null, this.stormCenter.x, this.stormCenter.y, this.stormCenter.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 2.0f);
        }

        spawnCycloneParticles(level, PURPLE_SLASH, SILVER_SPARKLE, this.stormCenter.add(0, 1, 0), STORM_RADIUS, 4.0, 10, ticksActive);

        if (ticksActive % 3 == 0) {
            Vec3 randomCenter = this.stormCenter.add(
                    (level.random.nextDouble() - 0.5) * STORM_RADIUS * 2,
                    level.random.nextDouble() * 4.0,
                    (level.random.nextDouble() - 0.5) * STORM_RADIUS * 2
            );
            float randomYaw = level.random.nextFloat() * 360;
            float randomPitch = -90 + level.random.nextFloat() * 180;

            spawnParticleArcInWorld(level, randomCenter, 2.5, 140, 15, randomYaw, randomPitch, PURPLE_SLASH);
        }
    }
}
