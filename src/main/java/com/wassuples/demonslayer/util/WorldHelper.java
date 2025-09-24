package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.gamerule.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class WorldHelper {
    /**
     * Carves a trench in the ground, setting it on fire.
     */
    public static void carveFieryTrench(ServerLevel level, Vec3 startPos, Vec3 endPos, int width, int depth) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) {
            return;
        }

        int segments = (int) startPos.distanceTo(endPos);
        if (segments <= 0 || width <= 0 || depth <= 0) return;

        Vec3 direction = endPos.subtract(startPos).normalize();

        for (int i = 0; i < segments; i++) {
            Vec3 currentPoint = startPos.add(direction.scale(i));
            BlockPos centerPos = new BlockPos(currentPoint);

            int radius = width / 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerPos.offset(x, 0, z));

                    for (int d = 1; d <= depth; d++) {
                        BlockPos currentTrenchPos = groundPos.below(d);
                        BlockState currentState = level.getBlockState(currentTrenchPos);

                        // This check correctly prevents breaking bedrock in the main loop
                        if (currentState.getDestroySpeed(level, currentTrenchPos) < 0) {
                            continue;
                        }

                        if (d < depth) {
                            level.setBlock(currentTrenchPos, Blocks.AIR.defaultBlockState(), 3);
                        } else {
                            level.setBlock(currentTrenchPos, level.random.nextFloat() > 0.6f ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
                        }

                        BlockPos newGroundPos = groundPos.below(depth);
                        BlockPos firePos = newGroundPos.above();
                        if (level.getBlockState(firePos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                    BlockPos pos1 = groundPos.below();
                    BlockPos pos2 = groundPos.below(2);

                    // Check if pos1 is unbreakable
                    if (level.getBlockState(pos1).getDestroySpeed(level, pos1) < 0) {
                        continue;
                    }

                    if (!level.getBlockState(pos1).isAir()) {
                        level.setBlock(pos1, Blocks.AIR.defaultBlockState(), 3);
                    }

                    // ** THE FIX IS HERE **
                    // Get the state of pos2
                    BlockState pos2State = level.getBlockState(pos2);
                    // Check if pos2 is unbreakable before changing it
                    if (pos2State.getDestroySpeed(level, pos2) >= 0 && !pos2State.isAir()) {
                        level.setBlock(pos2, level.random.nextFloat() > 0.6f ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
                    }

                    BlockPos firePos = pos2.above();
                    if (level.getBlockState(firePos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                        level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Destroys blocks in a radius around a center point.
     */
    public static void destroyBlocksInRadius(ServerLevel level, BlockPos center, double horizontalRadius, int verticalRadius, boolean dropBlocks, Entity owner) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) {
            return;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int hRadius = Mth.ceil(horizontalRadius);

        for (int x = -hRadius; x <= hRadius; x++) {
            for (int z = -hRadius; z <= hRadius; z++) {
                if (x == 0 && z == 0) continue; // Skip column directly under center
                if (x * x + z * z <= horizontalRadius * horizontalRadius) {
                    for (int y = -verticalRadius; y <= verticalRadius; y++) {
                        if (level.random.nextFloat() < 0.4f) {
                            mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                            if (level.getBlockState(mutablePos).getDestroySpeed(level, mutablePos) >= 0) {
                                level.destroyBlock(mutablePos, dropBlocks, owner);
                            }
                        }
                    }
                }
            }
        }
    }
}
