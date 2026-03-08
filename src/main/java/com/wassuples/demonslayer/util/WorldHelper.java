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
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Static world-manipulation utilities for form environmental effects.
 *
 * <p>Methods that modify blocks (e.g. fire trenches, ice patches) respect the
 * {@link com.wassuples.demonslayer.gamerule.ModGameRules#RULE_FORMS_GRIEFING} game rule.
 */
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
        destroyBlocksInRadius(level, center, horizontalRadius, verticalRadius, dropBlocks, owner, Float.MAX_VALUE);
    }

    /**
     * Destroys blocks in a radius around a center point, with a hardness cap and deterministic "inside-out" ordering.
     *
     * @param maxHardness max destroy speed (hardness). Values < 0 are treated as "break nothing".
     */
    public static void destroyBlocksInRadius(ServerLevel level, BlockPos center, double horizontalRadius, int verticalRadius, boolean dropBlocks, Entity owner, float maxHardness) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) {
            return;
        }
        if (maxHardness < 0) {
            return;
        }

        // Collect candidates (so we can sort by distance and destroy from the inside out).
        class Candidate {
            final BlockPos pos;
            final int dist2;

            Candidate(BlockPos pos, int dist2) {
                this.pos = pos;
                this.dist2 = dist2;
            }
        }

        int hRadius = Mth.ceil(horizontalRadius);
        int radiusSq = (int) Math.ceil(horizontalRadius * horizontalRadius);

        java.util.ArrayList<Candidate> candidates = new java.util.ArrayList<>();

        for (int x = -hRadius; x <= hRadius; x++) {
            for (int z = -hRadius; z <= hRadius; z++) {
                int horizDistSq = x * x + z * z;
                if (horizDistSq > radiusSq) continue;

                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    // Skip the column directly under center (preserve floor under the effect)
                    if (x == 0 && z == 0) continue;

                    BlockPos pos = center.offset(x, y, z);
                    int dist2 = horizDistSq + y * y;
                    candidates.add(new Candidate(pos, dist2));
                }
            }
        }

        // Closest first.
        candidates.sort(java.util.Comparator.comparingInt(c -> c.dist2));

        for (Candidate c : candidates) {
            BlockPos pos = c.pos;

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            // Never break block entities (chests/spawners/etc.)
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) continue;

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) continue; // unbreakable
            if (hardness > maxHardness) continue; // too hard

            level.destroyBlock(pos, dropBlocks, owner);
        }
    }

    /**
     * Destroys blocks in front of the attacker in a forward cone.
     *
     * Contract:
     * - Only affects blocks generally in front of {@code origin} (dot >= {@code minForwardDot}).
     * - Never breaks the column directly under/at the origin (prevents breaking under the player).
     * - Breaks only a subset of blocks: for each (x,z) column, we scan outward and stop at the first block we don't break,
     *   ensuring we never break blocks "behind" an unbroken block in that line.
     * - Respects forms griefing gamerule.
     */
    public static void destroyBlocksInFrontCone(ServerLevel level,
                                                Vec3 origin,
                                                Vec3 forward,
                                                double maxRange,
                                                int verticalRadius,
                                                boolean dropBlocks,
                                                Entity owner,
                                                float maxHardness,
                                                float breakChance,
                                                double minForwardDot) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) return;
        if (maxHardness < 0) return;
        if (breakChance <= 0) return;

        Vec3 fwd = new Vec3(forward.x, 0, forward.z);
        if (fwd.lengthSqr() < 1.0E-6) {
            fwd = new Vec3(0, 0, 1);
        } else {
            fwd = fwd.normalize();
        }

        // We'll iterate offsets by increasing distance so we naturally break closest-first.
        int hRadius = Mth.ceil(maxRange);
        int maxR = Math.max(1, hRadius);

        BlockPos originPos = new BlockPos(origin);
        final int originX = originPos.getX();
        final int originZ = originPos.getZ();

        // Utility: floor at/under player must never be destroyed.
        final int protectedFloorY = originPos.getY();

        // For each column in front, march outward from near to far and "ray-stop" when a block isn't destroyed.
        for (int dz = 0; dz <= maxR; dz++) {
            for (int dx = -maxR; dx <= maxR; dx++) {
                // Build a horizontal offset vector and skip near-zero.
                if (dx == 0 && dz == 0) continue;

                double distSq = (dx * (double) dx) + (dz * (double) dz);
                if (distSq > maxRange * maxRange) continue;

                // Cone check (in front of player)
                Vec3 off = new Vec3(dx, 0, dz);
                double dot = off.normalize().dot(fwd);
                if (dot < minForwardDot) continue;

                // Line ordering: scale step count based on distance, do integer stepping from origin.
                // We treat (dx,dz) as the target direction and step from closest to farthest.
                int steps = (int) Math.round(Math.sqrt(distSq));
                if (steps <= 0) continue;

                double stepX = dx / (double) steps;
                double stepZ = dz / (double) steps;

                boolean blocked = false;
                // Start at step 1 (closest).
                for (int s = 1; s <= steps; s++) {
                    if (blocked) break;

                    int x = originX + Mth.floor(stepX * s + 0.5 * Math.signum(stepX));
                    int z = originZ + Mth.floor(stepZ * s + 0.5 * Math.signum(stepZ));

                    for (int y = -verticalRadius; y <= verticalRadius; y++) {
                        int yy = originPos.getY() + y;

                        // Never break blocks at/below the player's feet column.
                        if (x == originX && z == originZ && yy <= protectedFloorY) continue;

                        BlockPos pos = new BlockPos(x, yy, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;

                        // Never break block entities.
                        if (level.getBlockEntity(pos) != null) continue;

                        float hardness = state.getDestroySpeed(level, pos);
                        if (hardness < 0) continue;
                        if (hardness > maxHardness) {
                            // Hard block blocks the ray for that (dx,dz) line.
                            blocked = true;
                            continue;
                        }

                        // Only break some blocks. If we decide not to break this solid block, it should block behind.
                        if (level.random.nextFloat() > breakChance) {
                            blocked = true;
                            continue;
                        }

                        level.destroyBlock(pos, dropBlocks, owner);
                    }
                }
            }
        }
    }
}
