package com.wassuples.demonslayer.worldgen.tree;

import com.mojang.serialization.Codec;
import com.wassuples.demonslayer.block.HangingWisteriaBlock;
import com.wassuples.demonslayer.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Material;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Wisteria tree: a leaning, gnarled trunk with a root flare, 3-5 branches arching out from the crown, a broad
 * umbrella canopy, and curtains of hanging blossoms under it (kept two blocks off the ground so you can walk beneath).
 *
 * <p>Used by both world generation and the sapling ({@code demonslayer:trees_wisteria} is now this feature). The shape
 * is planned in memory first; nothing is placed unless the trunk has room, so a blocked sapling simply doesn't grow.
 * Leaf distances are computed here so the canopy never decays on its own.
 */
public class WisteriaTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public WisteriaTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private enum Part { LOG_Y, LOG_X, LOG_Z, LEAF, HANG }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos origin = ctx.origin();

        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) return false;

        Map<BlockPos, Part> plan = new LinkedHashMap<>();

        // ---- trunk
        int h = 5 + r.nextInt(3);
        int[] lean = DIRS[r.nextInt(4)];
        int x = 0, z = 0, shifts = 0;
        for (int i = 0; i < h; i++) {
            plan.put(origin.offset(x, i, z), Part.LOG_Y);
            if (i >= 1 && i < h - 1 && shifts < 2 && r.nextFloat() < 0.35F) {
                x += lean[0];
                z += lean[1];
                plan.put(origin.offset(x, i, z), lean[0] != 0 ? Part.LOG_X : Part.LOG_Z);
                shifts++;
            }
        }
        BlockPos top = origin.offset(x, h - 1, z);
        for (int[] d : DIRS) {
            if (r.nextFloat() < 0.45F) plan.put(origin.offset(d[0], 0, d[1]), d[0] != 0 ? Part.LOG_X : Part.LOG_Z);
        }

        // room check: every trunk/branch cell must be replaceable
        Set<BlockPos> leaves = new LinkedHashSet<>();

        // ---- branches
        int n = 3 + r.nextInt(3);
        double off = r.nextDouble() * Mth.TWO_PI;
        for (int b = 0; b < n; b++) {
            double a = off + b * Mth.TWO_PI / n + (r.nextDouble() - 0.5D) * 0.6D;
            int len = 3 + r.nextInt(3);
            int bx = top.getX(), by = top.getY(), bz = top.getZ();
            double fx = bx, fz = bz;
            for (int s = 1; s <= len; s++) {
                fx += Math.cos(a);
                fz += Math.sin(a);
                int nx = (int) Math.round(fx), nz = (int) Math.round(fz);
                int ny = by + (s <= 2 && r.nextFloat() < 0.6F ? 1 : 0) - (s == len && len >= 4 ? 1 : 0);
                Part horiz = Math.abs(Math.cos(a)) >= Math.abs(Math.sin(a)) ? Part.LOG_X : Part.LOG_Z;
                if (nx != bx && nz != bz) plan.putIfAbsent(new BlockPos(nx, by, bz), Part.LOG_X);
                if (ny != by) plan.putIfAbsent(new BlockPos(bx, ny, bz), Part.LOG_Y);
                bx = nx; by = ny; bz = nz;
                plan.putIfAbsent(new BlockPos(bx, by, bz), horiz);
                if (s >= len - 1) blob(leaves, new BlockPos(bx, by + 1, bz), 2, 1, 1, r);
            }
        }
        blob(leaves, top.above(), 3, 0, 1, r);

        for (Map.Entry<BlockPos, Part> e : plan.entrySet()) {
            if (!canReplace(level, e.getKey(), e.getKey().equals(origin))) {
                // roots may be skipped; trunk and branches may not
                if (e.getKey().getY() == origin.getY() && !e.getKey().equals(origin)) continue;
                return false;
            }
        }
        for (BlockPos p : leaves) plan.putIfAbsent(p, Part.LEAF);

        // ---- blossom curtains
        Map<BlockPos, Boolean> hanging = new HashMap<>();
        for (BlockPos p : leaves) {
            if (plan.get(p) != Part.LEAF) continue;
            BlockPos below = p.below();
            if (plan.containsKey(below)) continue;
            double rim = Math.sqrt(Mth.square(p.getX() - top.getX()) + Mth.square(p.getZ() - top.getZ()));
            if (r.nextFloat() >= 0.3F + 0.07F * rim) continue;
            int len = 1 + r.nextInt(2 + Math.min(3, (int) rim));
            BlockPos q = below;
            BlockPos last = null;
            for (int i = 0; i < len; i++) {
                if (plan.containsKey(q) || q.getY() <= origin.getY() + 1 || !isAirish(level, q)) break;
                plan.put(q, Part.HANG);
                last = q;
                q = q.below();
            }
            if (last != null) hanging.put(last, true);
        }

        // ---- place logs, then leaves with computed distance, then blossoms
        BlockState logY = ModBlocks.WISTERIA_LOG.get().defaultBlockState();
        Set<BlockPos> logs = new HashSet<>();
        for (Map.Entry<BlockPos, Part> e : plan.entrySet()) {
            BlockPos p = e.getKey();
            switch (e.getValue()) {
                case LOG_Y, LOG_X, LOG_Z -> {
                    if (!canReplace(level, p, p.equals(origin))) continue;
                    Direction.Axis axis = e.getValue() == Part.LOG_X ? Direction.Axis.X : e.getValue() == Part.LOG_Z ? Direction.Axis.Z : Direction.Axis.Y;
                    level.setBlock(p, logY.setValue(RotatedPillarBlock.AXIS, axis), 19);
                    logs.add(p);
                }
                default -> { }
            }
        }
        Map<BlockPos, Integer> dist = leafDistances(plan, logs);
        BlockState leaf = ModBlocks.WISTERIA_LEAVES.get().defaultBlockState();
        for (Map.Entry<BlockPos, Part> e : plan.entrySet()) {
            if (e.getValue() != Part.LEAF) continue;
            BlockPos p = e.getKey();
            if (!isAirish(level, p)) continue;
            int d = dist.getOrDefault(p, 7);
            if (d >= 7) continue;   // would decay immediately
            level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, d).setValue(LeavesBlock.PERSISTENT, false), 19);
        }
        BlockState hang = ModBlocks.HANGING_WISTERIA.get().defaultBlockState();
        for (Map.Entry<BlockPos, Part> e : plan.entrySet()) {
            if (e.getValue() != Part.HANG) continue;
            BlockPos p = e.getKey();
            BlockState above = level.getBlockState(p.above());
            if (!(above.is(BlockTags.LEAVES) || above.is(ModBlocks.HANGING_WISTERIA.get()) || above.is(BlockTags.LOGS))) continue;
            if (!isAirish(level, p)) continue;
            level.setBlock(p, hang.setValue(HangingWisteriaBlock.BOTTOM, hanging.containsKey(p)), 19);
        }
        // fix tips if a curtain was cut short
        for (BlockPos p : plan.keySet()) {
            if (plan.get(p) == Part.HANG && level.getBlockState(p).is(ModBlocks.HANGING_WISTERIA.get())) {
                boolean bottom = !level.getBlockState(p.below()).is(ModBlocks.HANGING_WISTERIA.get());
                level.setBlock(p, level.getBlockState(p).setValue(HangingWisteriaBlock.BOTTOM, bottom), 19);
            }
        }
        return true;
    }

    private static void blob(Set<BlockPos> out, BlockPos c, int rx, int down, int up, RandomSource r) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rx; dz <= rx; dz++) {
                for (int dy = -down; dy <= up; dy++) {
                    double d = (dx * dx + dz * dz) / Mth.square(rx + 0.5D) + (dy * dy) / (1.6D * 1.6D);
                    if (d <= 1.0D && !(d > 0.75D && r.nextFloat() < 0.35F)) out.add(c.offset(dx, dy, dz));
                }
            }
        }
    }

    /** BFS from logs through planned leaves, capped at 7 (vanilla leaf distance rules). */
    private static Map<BlockPos, Integer> leafDistances(Map<BlockPos, Part> plan, Set<BlockPos> logs) {
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos l : logs) {
            for (Direction d : Direction.values()) {
                BlockPos p = l.relative(d);
                if (plan.get(p) == Part.LEAF && !dist.containsKey(p)) {
                    dist.put(p, 1);
                    queue.add(p);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = dist.get(p);
            if (d >= 6) continue;
            for (Direction dir : Direction.values()) {
                BlockPos q = p.relative(dir);
                if (plan.get(q) == Part.LEAF && !dist.containsKey(q)) {
                    dist.put(q, d + 1);
                    queue.add(q);
                }
            }
        }
        return dist;
    }

    private static boolean isAirish(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.getMaterial().isReplaceable() && s.getFluidState().isEmpty();
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos p, boolean isOrigin) {
        BlockState s = level.getBlockState(p);
        if (isOrigin && s.is(ModBlocks.WISTERIA_SAPLING.get())) return true;
        return s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.SAPLINGS) || s.getMaterial() == Material.REPLACEABLE_PLANT
                || s.getMaterial() == Material.PLANT || (s.getMaterial().isReplaceable() && s.getFluidState().isEmpty());
    }
}
