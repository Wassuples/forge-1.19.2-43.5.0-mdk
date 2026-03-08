package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Moon Breathing form: Waning Moonswaths.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WaningMoonswathsForm extends TimedForm {
    private static final int NUMBER_OF_STRIKES = 5;
    private static final int TICKS_PER_STRIKE = 6;
    private static final float DAMAGE = 16.0f;
    private static final double DISTANCE_FROM_TARGET = 3.5;
    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);

    @Nullable
    private final LivingEntity target;

    public WaningMoonswathsForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_STRIKES * TICKS_PER_STRIKE);
        this.target = getLookedAtEntity(user, 20.0);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_PER_STRIKE == 0) {
            Vec3 teleportDestination;
            Vec3 startPos = user.position();

            if (this.target != null && this.target.isAlive() && this.target.distanceTo(user) < 32.0) {
                Vec3 randomDir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).normalize();
                teleportDestination = target.getEyePosition().add(randomDir.scale(DISTANCE_FROM_TARGET));

            } else {
                Vec3 forwardDir = user.getLookAngle();
                Vec3 randomOffset = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).normalize().scale(2.0);
                Vec3 randomDir = forwardDir.add(randomOffset).normalize();
                teleportDestination = user.position().add(randomDir.scale(DISTANCE_FROM_TARGET * 1.5));
            }

            level.sendParticles(ParticleTypes.PORTAL, startPos.x, startPos.y + 1, startPos.z, 40, 0.5, 0.5, 0.5, 0.5);

            BlockPos teleportPosBlock = new BlockPos(teleportDestination);
            if (!level.getBlockState(teleportPosBlock).isAir() || !level.getBlockState(teleportPosBlock.above()).isAir()) {
                teleportDestination = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, teleportPosBlock));
            }
            user.teleportTo(teleportDestination.x, teleportDestination.y, teleportDestination.z);

            if (this.target != null && this.target.isAlive()) {
                user.lookAt(EntityAnchorArgument.Anchor.EYES, this.target.getEyePosition());
            }

            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
            dealDirectionalDamage(user, 5.0, DAMAGE, 0.8);
            spawnParticleArc(user, PURPLE_SLASH, 4.0, 140, 30);
        }
    }
}
