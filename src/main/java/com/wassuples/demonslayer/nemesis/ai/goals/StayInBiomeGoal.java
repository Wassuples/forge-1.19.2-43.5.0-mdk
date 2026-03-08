package com.wassuples.demonslayer.nemesis.ai.goals;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal that keeps a Nemesis demon within its assigned home biome.
 *
 * <p>If the demon wanders too far from its {@link com.wassuples.demonslayer.nemesis.trait.DemonTrait#getHomeBiome()
 * home biome}, this goal activates and pathfinds back toward a valid position. This creates
 * territorial behavior where demons defend specific areas of the world.
 */
public class StayInBiomeGoal extends Goal {
    private BlockPos homePosition;
    private final NemesisDemonEntity mob;
    private final double speedModifier;
    private ResourceLocation homeBiome;
    private int timeOutsideBiome = 0;
    private static final int RETREAT_TRIGGER_TIME = 60;

    public StayInBiomeGoal(NemesisDemonEntity pMob, double pSpeedModifier) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.homeBiome == null) {
            this.mob.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
                this.homeBiome = trait.getTerritory();
                this.homePosition = trait.getHomePosition();
            });
            if (this.homeBiome == null || this.homePosition == null) return false;
        }

        Holder<Biome> currentBiomeHolder = this.mob.level.getBiome(this.mob.blockPosition());
        boolean isOutsideBiome = currentBiomeHolder.unwrapKey()
                .map(key -> !key.location().equals(this.homeBiome))
                .orElse(true);

        if (isOutsideBiome) {
            this.timeOutsideBiome++;
        } else {
            this.timeOutsideBiome = 0;
        }

        return this.timeOutsideBiome > RETREAT_TRIGGER_TIME;
    }

    @Override
    public boolean canContinueToUse() {
        Holder<Biome> currentBiomeHolder = this.mob.level.getBiome(this.mob.blockPosition());
        return currentBiomeHolder.unwrapKey()
                .map(key -> !key.location().equals(this.homeBiome))
                .orElse(true) && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        Vec3 retreatPos = findRandomPosInHomeBiome();
        if (retreatPos != null) {
            this.mob.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.timeOutsideBiome = 0;
    }

    @Nullable
    private Vec3 findRandomPosInHomeBiome() {
        for (int i = 0; i < 15; i++) {
            Vec3 randomPos = DefaultRandomPos.getPos(this.mob, 16, 8);
            if (randomPos != null) {
                Holder<Biome> biomeAtPos = this.mob.level.getBiome(new BlockPos(randomPos));
                boolean isHomeBiome = biomeAtPos.unwrapKey()
                        .map(key -> key.location().equals(this.homeBiome))
                        .orElse(false);

                if (isHomeBiome) {
                    return randomPos;
                }
            }
        }
        return DefaultRandomPos.getPosTowards(this.mob, 16, 8, Vec3.atBottomCenterOf(this.homePosition), Math.PI / 2);
    }
}
