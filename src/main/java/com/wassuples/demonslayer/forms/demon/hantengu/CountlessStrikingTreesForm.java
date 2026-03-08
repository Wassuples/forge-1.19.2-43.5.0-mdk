package com.wassuples.demonslayer.forms.demon.hantengu;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

/**
 * Hantengu Blood Demon Art form: Countless Striking Trees.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CountlessStrikingTreesForm extends TimedForm {
    private static final int STRIKE_TICK_RATE = 8;
    private static final double AOE_RADIUS = 35.0;
    private static final double DAMAGE_RADIUS = 3.0;
    private static final float DAMAGE = 18.0f;

    public CountlessStrikingTreesForm(LivingEntity user, IForm form) {
        super(user, form, 200);
    } // 200 ticks = 10 seconds

    @Override
    public void onStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (ticksActive % 20 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
        }
        if (ticksActive > 0 && ticksActive % STRIKE_TICK_RATE == 0) {
            Vec3 strikeTargetPos;
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class,
                    user.getBoundingBox().inflate(AOE_RADIUS),
                    e -> e != user && e.isAlive() && !(e instanceof Player && ((Player) e).isCreative()));

            if (!potentialTargets.isEmpty()) {
                LivingEntity randomTarget = potentialTargets.get(level.random.nextInt(potentialTargets.size()));
                strikeTargetPos = randomTarget.position();
            } else {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double distance = 5 + level.random.nextDouble() * (AOE_RADIUS - 5);
                double x = user.getX() + Math.cos(angle) * distance;
                double z = user.getZ() + Math.sin(angle) * distance;
                strikeTargetPos = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, user.getY(), z)));
            }
            triggerDragonStrike(strikeTargetPos);
        }
    }

    private void triggerDragonStrike(Vec3 position) {
        // This could be made more complex later, e.g., with a telegraph particle first
        // For now, it's an instant eruption.

        level.playSound(null, position.x, position.y, position.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.5f, 1.0f);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);

        final BlockParticleOption woodParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_WOOD.defaultBlockState());
        for (int i = 0; i < 50; i++) {
            double motionY = 0.5 + level.random.nextDouble() * 0.5;
            level.sendParticles(woodParticle, position.x, position.y, position.z, 3, 0.8, 1.5, 0.8, motionY);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y + 1.0, position.z, 1, 0, 0, 0, 0);
        dealAOEDamage(user, position, DAMAGE_RADIUS, DAMAGE, 1.5f);
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
