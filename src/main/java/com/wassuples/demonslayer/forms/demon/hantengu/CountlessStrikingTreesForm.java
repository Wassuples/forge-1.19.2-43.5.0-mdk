package com.wassuples.demonslayer.forms.demon.hantengu;

import com.wassuples.demonslayer.entity.custom.HantenguDrumsEntity;
import com.wassuples.demonslayer.entity.custom.WoodDragonStrikeEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.util.FormAnimations;
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

import javax.annotation.Nullable;
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

    private static final int DURATION = 200; // 10 seconds
    /** Distance from the strike point at which the dragon's neck tears out of the ground. */
    private static final double ROOT_DISTANCE = 3.5;

    @Nullable
    private HantenguDrumsEntity drums;

    public CountlessStrikingTreesForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
        this.drums = new HantenguDrumsEntity(level, user, DURATION);
        level.addFreshEntity(this.drums);
        FormAnimations.play(user, FormAnimations.HANTENGU_DRUM);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (ticksActive % 20 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
            if (drums != null) drums.beat();
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
        // A wooden dragon tears out of the ground beside the target and slams down onto it (WoodDragonStrikeEntity);
        // the damage stays instant, the head lands within two ticks.
        Vec3 away = new Vec3(user.getX() - position.x, 0, user.getZ() - position.z);
        if (away.lengthSqr() < 16.0) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        Vec3 root = position.add(away.normalize().scale(ROOT_DISTANCE));
        level.addFreshEntity(new WoodDragonStrikeEntity(level, position, root));
        if (drums != null) drums.beat();

        level.playSound(null, position.x, position.y, position.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.5f, 1.0f);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);

        final BlockParticleOption woodParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_WOOD.defaultBlockState());
        // A few splinters and clods where the head lands (the dragon itself is the visual).
        level.sendParticles(woodParticle, position.x, position.y + 0.3, position.z, 14, 0.7, 0.3, 0.7, 0.25);
        dealAOEDamage(user, position, DAMAGE_RADIUS, DAMAGE, 1.5f);
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        if (drums != null) {
            drums.discard();
            drums = null;
        }
        FormAnimations.play(user, FormAnimations.HANTENGU_END);
    }
}
