package com.wassuples.demonslayer.worldgen.fujikasane;

import com.wassuples.demonslayer.block.HangingWisteriaBlock;
import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.worldgen.ModFeatures;
import com.wassuples.demonslayer.worldgen.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Material;

/**
 * Builds Mt. Fujikasane chunk by chunk (the piece covers the whole mountain; {@code postProcess} only ever touches the
 * chunk being decorated). Runs in the {@code top_layer_modification} step, after the biome's own trees, so it can
 * reshape the ground freely:
 * <ol>
 *   <li>terrain: raise/cut every column to the mountain profile, drain any water, clear what grew there, and lay a
 *       surface by steepness (stone crags, scree, grass; podzol and moss on the summit);</li>
 *   <li>works: the south stairway with stone lanterns and torii, the shrine plaza with its hall, and the short north
 *       stairs through the last torii into the summit;</li>
 *   <li>growth: a dense band of wisteria from the foot up to the slope (on a fixed world grid so trees line up across
 *       chunks), and a wisteria-free summit of scattered spruce, boulders and ferns.</li>
 * </ol>
 * Everything is a pure function of {@link FujikasaneLayout}, so neighbouring chunks agree on every height.
 */
public class FujikasaneMountainPiece extends StructurePiece {
    private static final int TREE_CELL = 7;
    private final int centerX, centerZ, baseY;

    public FujikasaneMountainPiece(int centerX, int centerZ, int baseY, int minY) {
        super(ModStructures.FUJIKASANE_MOUNTAIN.get(), 0, new BoundingBox(
                centerX - FujikasaneLayout.R, minY, centerZ - FujikasaneLayout.R,
                centerX + FujikasaneLayout.R, baseY + FujikasaneLayout.H + 40, centerZ + FujikasaneLayout.R));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.baseY = baseY;
    }

    public FujikasaneMountainPiece(CompoundTag tag) {
        super(ModStructures.FUJIKASANE_MOUNTAIN.get(), tag);
        this.centerX = tag.getInt("CX");
        this.centerZ = tag.getInt("CZ");
        this.baseY = tag.getInt("BaseY");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", centerX);
        tag.putInt("CZ", centerZ);
        tag.putInt("BaseY", baseY);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        FujikasaneLayout layout = new FujikasaneLayout(centerX, centerZ, baseY, level.getSeed());
        int seaLevel = generator.getSeaLevel();
        int minX = chunkPos.getMinBlockX(), minZ = chunkPos.getMinBlockZ();
        int[] surface = new int[256];
        boolean any = false;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = minX + lx, z = minZ + lz;
                int dx = x - centerX, dz = z - centerZ;
                if (FujikasaneLayout.dist(dx, dz) > FujikasaneLayout.R) {
                    surface[lz * 16 + lx] = Integer.MIN_VALUE;
                    continue;
                }
                surface[lz * 16 + lx] = shapeColumn(level, layout, x, z, dx, dz, seaLevel);
                any = true;
            }
        }
        if (!any) return;
        buildWorks(level, layout, box);
        buildSummitRuins(level, layout, box, random);
        grow(level, generator, layout, box, chunkPos, surface, random);
    }

    // ================================================================== terrain

    private static boolean isOurs(BlockState s) {
        return s.is(ModBlocks.WISTERIA_LOG.get()) || s.is(ModBlocks.WISTERIA_WOOD.get()) || s.is(ModBlocks.WISTERIA_LEAVES.get())
                || s.is(ModBlocks.HANGING_WISTERIA.get());
    }

    /**
     * WORLD_SURFACE is primed when features start and kept current while they place blocks (the _WG maps are not),
     * so it includes the biome's trees; it is also the live map for operator placement.
     */
    private static Heightmap.Types surfaceType(WorldGenLevel level) {
        return Heightmap.Types.WORLD_SURFACE;
    }

    /** Top block of the natural ground (ignores air, water, plants, snow, trees and our own blossoms). */
    private static int findGround(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, level.getHeight(surfaceType(level), x, z) - 1, z);
        int min = level.getMinBuildHeight();
        while (p.getY() > min) {
            BlockState s = level.getBlockState(p);
            Material m = s.getMaterial();
            boolean skip = s.isAir() || !s.getFluidState().isEmpty() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || isOurs(s)
                    || m == Material.PLANT || m == Material.REPLACEABLE_PLANT || m == Material.TOP_SNOW || m == Material.SNOW
                    || m == Material.BAMBOO || m == Material.BAMBOO_SAPLING || m == Material.ICE || m == Material.WATER_PLANT
                    || m == Material.REPLACEABLE_WATER_PLANT || m == Material.VEGETABLE;
            if (!skip) return p.getY();
            p.move(Direction.DOWN);
        }
        return min;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Shapes one column; returns the Y of its top block. */
    private int shapeColumn(WorldGenLevel level, FujikasaneLayout layout, int x, int z, int dx, int dz, int seaLevel) {
        double t = FujikasaneLayout.dist(dx, dz) / FujikasaneLayout.R;
        int ground = findGround(level, x, z);
        boolean built = layout.inPlaza(dx, dz) || layout.onSouthStairs(dx, dz) || layout.onNorthStairs(dx, dz);
        int target = layout.surfaceY(dx, dz);
        if (!built && t > 0.92) {
            // the foot settles into whatever land surrounds the mountain (raised to dry land if that was water)
            double w = smooth((t - 0.92) / 0.08);
            int outside = Math.max(ground, seaLevel + 1);
            target = (int) Math.round(target * (1 - w) + outside * w);
        }

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 0, z);
        // clear above the new surface: everything (incl. water and the biome's trees) on the mountain proper,
        // only loose ground and water at the very foot so trees around the rim keep standing
        int top = level.getHeight(surfaceType(level), x, z);
        boolean keepTrees = t > 0.94;
        for (int y = top; y > target; y--) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || isOurs(s)) continue;
            if (keepTrees && (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES))) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }

        // materials by steepness and zone
        double slope = layout.slope(dx, dz);
        double n = layout.scatter(dx, dz, 3);
        boolean summit = FujikasaneLayout.dist(dx, dz) < FujikasaneLayout.DEMON_R - 2;
        BlockState topState, sub;
        if (built) {
            topState = Blocks.STONE_BRICKS.defaultBlockState();
            sub = Blocks.STONE_BRICKS.defaultBlockState();
        } else if (slope > 1.7 || (slope > 1.25 && n < 0.45)) {
            topState = n < 0.25 ? Blocks.ANDESITE.defaultBlockState() : n < 0.32 ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState();
            sub = Blocks.STONE.defaultBlockState();
        } else if (slope > 1.0) {
            topState = n < 0.5 ? Blocks.COARSE_DIRT.defaultBlockState() : n < 0.62 ? Blocks.GRAVEL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            sub = Blocks.DIRT.defaultBlockState();
        } else if (Math.abs(dx) <= FujikasaneLayout.STAIR_HALF_WIDTH && dz > FujikasaneLayout.STAIR_BOTTOM) {
            // the stairway gives way to a trodden path across the foot, which follows the land around it
            topState = n < 0.5 ? Blocks.GRAVEL.defaultBlockState() : n < 0.8 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            sub = Blocks.DIRT.defaultBlockState();
        } else if (summit) {
            topState = n < 0.3 ? Blocks.PODZOL.defaultBlockState() : n < 0.42 ? Blocks.COARSE_DIRT.defaultBlockState()
                    : n < 0.47 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            sub = Blocks.DIRT.defaultBlockState();
        } else {
            topState = n < 0.05 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            sub = Blocks.DIRT.defaultBlockState();
        }

        // fill up (or re-skin) to the target
        int from = Math.min(ground, target - 4);
        for (int y = from + 1; y <= target; y++) {
            p.setY(y);
            BlockState current = level.getBlockState(p);
            if (y <= ground && !current.isAir() && current.getFluidState().isEmpty() && y < target - 3) continue;   // keep the old ground deep down
            if (isOurs(current)) continue;
            BlockState s = y == target ? topState : y >= target - 3 ? sub : Blocks.STONE.defaultBlockState();
            level.setBlock(p, s, 2);
        }
        // water seeping in from the side at the foot would flow over the new slope: wall it in
        for (Direction d : Direction.Plane.HORIZONTAL) {
            p.set(x + d.getStepX(), target, z + d.getStepZ());
            if (FujikasaneLayout.dist(dx + d.getStepX(), dz + d.getStepZ()) <= FujikasaneLayout.R) continue;
            if (!level.getFluidState(p).isEmpty() && level.getBlockState(p).getBlock() == Blocks.WATER) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        }
        return target;
    }

    // ================================================================== works

    private static void put(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        if (!box.isInside(new Vec3i(x, y, z))) return;
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    private static BlockState stair(net.minecraft.world.level.block.Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    private static BlockState pillar(Direction.Axis axis) {
        return ModBlocks.VERMILION_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private void buildWorks(WorldGenLevel level, FujikasaneLayout layout, BoundingBox box) {
        int cx = centerX, cz = centerZ;
        // chunk overlaps nothing built? skip
        BoundingBox works = new BoundingBox(cx - 16, box.minY(), cz + layout.northStairEnd() - 2, cx + 16, box.maxY(),
                cz + FujikasaneLayout.STAIR_BOTTOM + 2);
        if (!works.intersects(box)) return;

        // ---- south stairway
        for (int dz = FujikasaneLayout.STAIR_TOP; dz <= FujikasaneLayout.STAIR_BOTTOM; dz++) {
            int y = layout.southStairY(dz);
            boolean step = dz > FujikasaneLayout.STAIR_TOP && layout.southStairY(dz - 1) == y + 1;
            for (int dx = -FujikasaneLayout.STAIR_HALF_WIDTH; dx <= FujikasaneLayout.STAIR_HALF_WIDTH; dx++) {
                boolean mossy = layout.scatter(dx, dz, 11) < 0.3;
                put(level, box, cx + dx, y, cz + dz, (mossy ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                put(level, box, cx + dx, y + 1, cz + dz, step
                        ? stair(mossy ? Blocks.MOSSY_STONE_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM)
                        : Blocks.AIR.defaultBlockState());
                for (int k = 2; k <= 5; k++) put(level, box, cx + dx, y + k, cz + dz, Blocks.AIR.defaultBlockState());
            }
            // kerbs
            for (int side = -1; side <= 1; side += 2) {
                int kx = side * (FujikasaneLayout.STAIR_HALF_WIDTH + 1);
                put(level, box, cx + kx, y, cz + dz, Blocks.STONE_BRICKS.defaultBlockState());
            }
            int rel = dz - FujikasaneLayout.STAIR_TOP;
            if (rel % 10 == 4) {
                lantern(level, layout, box, -4, dz);
                lantern(level, layout, box, 4, dz);
            }
            if (rel == 2 || dz == FujikasaneLayout.STAIR_BOTTOM - 3) torii(level, box, dz, y, false);
        }

        // ---- plaza
        int py = layout.plazaY();
        int pz = cz + FujikasaneLayout.PLAZA_DIST;
        int r = FujikasaneLayout.PLAZA_R;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;
                BlockState floor;
                boolean path = Math.abs(dx) <= 1 || (Math.abs(dz) <= 1 && dx < 0);
                if (d > r - 1) floor = Blocks.STONE_BRICKS.defaultBlockState();
                else if (path) floor = Blocks.STONE_BRICKS.defaultBlockState();
                else if (Math.abs(dx) == 2 || (Math.abs(dz) == 2 && dx < 0)) floor = Blocks.SMOOTH_STONE.defaultBlockState();
                else floor = layout.scatter(dx, dz, 21) < 0.15 ? Blocks.DIORITE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                put(level, box, cx + dx, py, pz + dz, floor);
                for (int k = 1; k <= 7; k++) put(level, box, cx + dx, py + k, pz + dz, Blocks.AIR.defaultBlockState());
                // low wall around the rim with gates north, south (stairs) and east
                if (d > r - 1 && Math.abs(dx) > 2 && !(dx > 0 && Math.abs(dz) <= 1)) {
                    put(level, box, cx + dx, py + 1, pz + dz, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
                }
            }
        }
        // gate lanterns
        lanternAt(level, box, cx - 3, py + 1, pz + r - 1);
        lanternAt(level, box, cx + 3, py + 1, pz + r - 1);
        lanternAt(level, box, cx - 3, py + 1, pz - r + 1);
        lanternAt(level, box, cx + 3, py + 1, pz - r + 1);
        lanternAt(level, box, cx + 3, py + 1, pz - 4);
        lanternAt(level, box, cx + 3, py + 1, pz + 4);
        shrine(level, box, cx, py, pz);

        // ---- north stairs into the summit, under the last torii
        int end = layout.northStairEnd();
        for (int dz = FujikasaneLayout.NORTH_STAIR_START - 1; dz >= end; dz--) {
            int y = layout.northStairY(dz);
            boolean step = dz > end && layout.northStairY(dz - 1) == y + 1;
            for (int dx = -FujikasaneLayout.STAIR_HALF_WIDTH; dx <= FujikasaneLayout.STAIR_HALF_WIDTH; dx++) {
                boolean mossy = layout.scatter(dx, dz, 13) < 0.45;
                put(level, box, cx + dx, y, cz + dz, (mossy ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                put(level, box, cx + dx, y + 1, cz + dz, step
                        ? stair(mossy ? Blocks.MOSSY_STONE_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM)
                        : Blocks.AIR.defaultBlockState());
                for (int k = 2; k <= 5; k++) put(level, box, cx + dx, y + k, cz + dz, Blocks.AIR.defaultBlockState());
            }
        }
        torii(level, box, FujikasaneLayout.NORTH_STAIR_START - 2, layout.northStairY(FujikasaneLayout.NORTH_STAIR_START - 2), true);
    }

    /**
     * Landmarks on the mountaintop: the Hand Demon's den (a pit strewn with the bones of candidates) and a ruined
     * hut from some long-gone selection.
     */
    private void buildSummitRuins(WorldGenLevel level, FujikasaneLayout layout, BoundingBox box, RandomSource random) {
        // ---- the den
        int dx0 = FujikasaneLayout.DEN_X, dz0 = FujikasaneLayout.DEN_Z;
        int rim = layout.naturalY(dx0, dz0);
        for (int dz = -7; dz <= 7; dz++) {
            for (int dx = -7; dx <= 7; dx++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 7) continue;
                int x = centerX + dx0 + dx, z = centerZ + dz0 + dz;
                if (!box.isInside(new Vec3i(x, rim, z))) continue;
                int floor = d < 5 ? rim - 4 + (int) (d / 2.5) : rim;
                for (int y = floor + 1; y <= rim + 8; y++) put(level, box, x, y, z, Blocks.AIR.defaultBlockState());
                double n = layout.scatter(dx0 + dx, dz0 + dz, 61);
                put(level, box, x, floor, z, (n < 0.3 ? Blocks.MOSSY_COBBLESTONE : n < 0.55 ? Blocks.COARSE_DIRT : n < 0.7 ? Blocks.BONE_BLOCK
                        : Blocks.PODZOL).defaultBlockState());
                put(level, box, x, floor - 1, z, Blocks.STONE.defaultBlockState());
                if (d < 5 && n > 0.92) put(level, box, x, floor + 1, z, Blocks.COBWEB.defaultBlockState());
                if (d >= 5 && d <= 6.5 && n < 0.25) put(level, box, x, rim + 1, z, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            }
        }
        // ---- the ruined hut
        int hx = FujikasaneLayout.HUT_X, hz = FujikasaneLayout.HUT_Z;
        int fy = layout.naturalY(hx, hz) + 1;
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                int x = centerX + hx + dx, z = centerZ + hz + dz;
                if (!box.isInside(new Vec3i(x, fy, z))) continue;
                for (int y = fy - 4; y < fy; y++) put(level, box, x, y, z, Blocks.STONE.defaultBlockState());
                double n = layout.scatter(hx + dx, hz + dz, 67);
                put(level, box, x, fy, z, n < 0.2 ? Blocks.COARSE_DIRT.defaultBlockState() : ModBlocks.WISTERIA_PLANKS.get().defaultBlockState());
                for (int y = fy + 1; y <= fy + 6; y++) put(level, box, x, y, z, Blocks.AIR.defaultBlockState());
                boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                if (corner) {
                    int h = 1 + (int) (n * 4);
                    for (int y = 1; y <= h; y++) put(level, box, x, fy + y, z, ModBlocks.STRIPPED_WISTERIA_LOG.get().defaultBlockState());
                } else if (edge && !(dz == 3 && Math.abs(dx) <= 1)) {
                    int h = (int) (n * 3);   // walls fallen to stumps here and there
                    for (int y = 1; y <= h; y++) put(level, box, x, fy + y, z, (n < 0.5 ? ModBlocks.SHOJI_SCREEN : ModBlocks.WISTERIA_PLANKS).get().defaultBlockState());
                } else if (!edge && n > 0.85) {
                    put(level, box, x, fy + 1, z, ModBlocks.KAWARA_TILE_SLAB.get().defaultBlockState());   // fallen roof tiles
                }
            }
        }
    }

    private void lantern(WorldGenLevel level, FujikasaneLayout layout, BoundingBox box, int dx, int dz) {
        int y = layout.naturalY(dx, dz);
        lanternAt(level, box, centerX + dx, y + 1, centerZ + dz);
    }

    private static void lanternAt(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        put(level, box, x, y, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        put(level, box, x, y + 1, z, ModBlocks.STONE_LANTERN.get().defaultBlockState());
    }

    /** A myōjin torii across the path at {@code dz}: vermilion pillars and beams, black lacquered kasagi. */
    private void torii(WorldGenLevel level, BoundingBox box, int dz, int y, boolean rope) {
        int z = centerZ + dz;
        for (int side = -1; side <= 1; side += 2) {
            int x = centerX + side * 3;
            for (int k = -2; k <= 0; k++) put(level, box, x, y + k, z, Blocks.STONE_BRICKS.defaultBlockState());   // footing
            for (int k = 1; k <= 6; k++) put(level, box, x, y + k, z, pillar(Direction.Axis.Y));
        }
        for (int dx = -4; dx <= 4; dx++) put(level, box, centerX + dx, y + 5, z, pillar(Direction.Axis.X));   // nuki
        for (int dx = -4; dx <= 4; dx++) put(level, box, centerX + dx, y + 7, z, pillar(Direction.Axis.X));   // shimaki
        put(level, box, centerX, y + 6, z, ModBlocks.BLACK_LACQUER.get().defaultBlockState());               // gakuzuka
        for (int dx = -5; dx <= 5; dx++) put(level, box, centerX + dx, y + 8, z, ModBlocks.BLACK_LACQUER.get().defaultBlockState());   // kasagi
        // the kasagi's tips sweep up
        put(level, box, centerX - 6, y + 8, z, stair(ModBlocks.KAWARA_TILE_STAIRS.get(), Direction.EAST, Half.TOP));
        put(level, box, centerX + 6, y + 8, z, stair(ModBlocks.KAWARA_TILE_STAIRS.get(), Direction.WEST, Half.TOP));
        if (rope) {
            // shide-hung rope across the gate into the summit: the last line the wisteria will not cross
            for (int dx = -2; dx <= 2; dx++) put(level, box, centerX + dx, y + 4, z, ModBlocks.HANGING_WISTERIA.get().defaultBlockState()
                    .setValue(HangingWisteriaBlock.BOTTOM, true));
        }
    }

    /** The hall where the twins receive the candidates: west side of the plaza, facing east. */
    private static void shrine(WorldGenLevel level, BoundingBox box, int cx, int py, int pz) {
        BlockState planks = ModBlocks.WISTERIA_PLANKS.get().defaultBlockState();
        BlockState shoji = ModBlocks.SHOJI_SCREEN.get().defaultBlockState();
        // platform and floor
        for (int dx = -9; dx <= -3; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                put(level, box, cx + dx, py, pz + dz, Blocks.STONE_BRICKS.defaultBlockState());
                put(level, box, cx + dx, py + 1, pz + dz, dx == -3 ? Blocks.AIR.defaultBlockState() : planks);
                for (int k = 2; k <= 5; k++) put(level, box, cx + dx, py + k, pz + dz, Blocks.AIR.defaultBlockState());
            }
        }
        // entry steps
        for (int dz = -2; dz <= 2; dz++) put(level, box, cx - 3, py + 1, pz + dz, stair(ModBlocks.WISTERIA_STAIRS.get(), Direction.WEST, Half.BOTTOM));
        // posts, walls (open front)
        for (int dx = -9; dx <= -4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean corner = (dx == -9 || dx == -4) && Math.abs(dz) == 4;
                boolean wall = dx == -9 || Math.abs(dz) == 4;
                if (!wall && !(dx == -4 && Math.abs(dz) == 4)) continue;
                for (int k = 2; k <= 4; k++) {
                    BlockState s = corner || (dx == -9 && dz == 0) || (Math.abs(dz) == 4 && dx == -6) ? pillar(Direction.Axis.Y) : shoji;
                    put(level, box, cx + dx, py + k, pz + dz, s);
                }
                put(level, box, cx + dx, py + 5, pz + dz, pillar(dx == -9 || dx == -4 ? Direction.Axis.Z : Direction.Axis.X));
            }
        }
        for (int dz = -4; dz <= 4; dz++) put(level, box, cx - 4, py + 5, pz + dz, pillar(Direction.Axis.Z));   // front beam
        // altar: a lantern on a lacquered stand at the back
        put(level, box, cx - 8, py + 2, pz, ModBlocks.BLACK_LACQUER.get().defaultBlockState());
        put(level, box, cx - 8, py + 3, pz, ModBlocks.STONE_LANTERN.get().defaultBlockState());
        // gable roof along Z, kawara tiles, eaves overhanging one block
        BlockState tiles = ModBlocks.KAWARA_TILES.get().defaultBlockState();
        for (int layer = 0; layer < 4; layer++) {
            int y = py + 6 + layer;
            int west = -10 + layer, east = -3 - layer;
            for (int dz = -5; dz <= 5; dz++) {
                put(level, box, cx + west, y, pz + dz, stair(ModBlocks.KAWARA_TILE_STAIRS.get(), Direction.EAST, Half.BOTTOM));
                put(level, box, cx + east, y, pz + dz, stair(ModBlocks.KAWARA_TILE_STAIRS.get(), Direction.WEST, Half.BOTTOM));
                for (int dx = west + 1; dx < east; dx++) put(level, box, cx + dx, y, pz + dz, Math.abs(dz) == 5 ? tiles : planks);
            }
        }
        for (int dz = -5; dz <= 5; dz++) {
            put(level, box, cx - 7, py + 10, pz + dz, ModBlocks.KAWARA_TILE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            put(level, box, cx - 6, py + 10, pz + dz, ModBlocks.KAWARA_TILE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        // blossoms hanging from the eaves
        for (int dz = -5; dz <= 5; dz += 2) {
            put(level, box, cx - 10, py + 5, pz + dz, ModBlocks.HANGING_WISTERIA.get().defaultBlockState().setValue(HangingWisteriaBlock.BOTTOM, true));
        }
        for (int dz = -5; dz <= 5; dz += 2) {
            if (Math.abs(dz) <= 1) continue;
            put(level, box, cx - 3, py + 5, pz + dz, ModBlocks.HANGING_WISTERIA.get().defaultBlockState().setValue(HangingWisteriaBlock.BOTTOM, true));
        }
    }

    // ================================================================== growth

    private void grow(WorldGenLevel level, ChunkGenerator generator, FujikasaneLayout layout, BoundingBox box, ChunkPos chunkPos,
                      int[] surface, RandomSource random) {
        int minX = chunkPos.getMinBlockX(), minZ = chunkPos.getMinBlockZ();

        // wisteria band: one candidate tree per grid cell (grid anchored on the mountain, so it's chunk-independent)
        int gx0 = Math.floorDiv(minX - centerX, TREE_CELL) - 1, gx1 = Math.floorDiv(minX + 15 - centerX, TREE_CELL) + 1;
        int gz0 = Math.floorDiv(minZ - centerZ, TREE_CELL) - 1, gz1 = Math.floorDiv(minZ + 15 - centerZ, TREE_CELL) + 1;
        for (int gz = gz0; gz <= gz1; gz++) {
            for (int gx = gx0; gx <= gx1; gx++) {
                int dx = gx * TREE_CELL + (int) (layout.scatter(gx, gz, 31) * (TREE_CELL - 2));
                int dz = gz * TREE_CELL + (int) (layout.scatter(gx, gz, 37) * (TREE_CELL - 2));
                int x = centerX + dx, z = centerZ + dz;
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                if (!layout.wisteriaAllowed(dx, dz)) continue;
                double r = FujikasaneLayout.dist(dx, dz) / FujikasaneLayout.R;
                double density = r < 0.62 ? 0.6 : r > 0.93 ? 0.5 : 0.85;   // thinner at the wisteria line and the foot
                if (layout.scatter(gx, gz, 41) > density) continue;
                int top = surface[(z - minZ) * 16 + (x - minX)];
                if (top == Integer.MIN_VALUE) continue;
                BlockPos at = new BlockPos(x, top + 1, z);
                if (!level.getBlockState(at.below()).is(BlockTags.DIRT)) continue;
                ModFeatures.WISTERIA_TREE.get().place(NoneFeatureConfiguration.INSTANCE, level, generator, random, at);
            }
        }

        // ground cover and the summit's own growth
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int top = surface[lz * 16 + lx];
                if (top == Integer.MIN_VALUE) continue;
                int x = minX + lx, z = minZ + lz, dx = x - centerX, dz = z - centerZ;
                if (layout.inPlaza(dx, dz) || layout.onSouthStairs(dx, dz) || layout.onNorthStairs(dx, dz)) continue;
                if (Math.abs(dx) <= FujikasaneLayout.STAIR_HALF_WIDTH + 2 && dz >= layout.northStairEnd() - 1) continue;
                if (FujikasaneLayout.dist(dx - FujikasaneLayout.DEN_X, dz - FujikasaneLayout.DEN_Z) < 9
                        || FujikasaneLayout.dist(dx - FujikasaneLayout.HUT_X, dz - FujikasaneLayout.HUT_Z) < 6) continue;
                BlockPos ground = new BlockPos(x, top, z), above = ground.above();
                if (!level.getBlockState(above).isAir()) continue;
                BlockState g = level.getBlockState(ground);
                double n = layout.scatter(dx, dz, 51);
                boolean summit = layout.inDemonZone(dx, dz);
                if (summit) {
                    // the mountaintop is broken up by groves, dead trees, thickets and rocks: no long sightlines
                    boolean inner = lx >= 3 && lx <= 12 && lz >= 3 && lz <= 12;
                    boolean core = lx >= 4 && lx <= 11 && lz >= 4 && lz <= 11;
                    boolean grove = layout.grove(dx, dz) > 0.55;
                    boolean soil = g.is(BlockTags.DIRT) && layout.slope(dx, dz) < 1.3;
                    double spruce = grove ? 0.05 : 0.012, dead = grove ? 0.062 : 0.019;
                    if (n < spruce && inner && soil) {
                        if (grove && n < 0.006 && core) TreeFeatures.MEGA_SPRUCE.value().place(level, generator, random, above);
                        else (n < spruce * 0.4 ? TreeFeatures.PINE : TreeFeatures.SPRUCE).value().place(level, generator, random, above);
                    } else if (n < dead && lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13) {
                        deadTree(level, box, x, top + 1, z, random);
                    } else if (n < dead + 0.004 && inner) {
                        fallenLog(level, box, x, top + 1, z, random);
                    } else if (n < dead + 0.012 && inner) {
                        boulder(level, box, x, top, z, random);
                    } else if (n < dead + 0.05) {
                        thicket(level, box, x, top + 1, z, random);
                    } else if (n < 0.28 && g.is(Blocks.GRASS_BLOCK)) {
                        if (n < 0.2) tallPlant(level, box, x, top + 1, z, n < 0.15 ? Blocks.TALL_GRASS : Blocks.LARGE_FERN);
                        else put(level, box, x, top + 1, z, Blocks.FERN.defaultBlockState());
                    } else if (n < 0.3 && (g.is(Blocks.PODZOL) || g.is(Blocks.COARSE_DIRT))) {
                        put(level, box, x, top + 1, z, Blocks.DEAD_BUSH.defaultBlockState());
                    }
                } else if (g.is(Blocks.GRASS_BLOCK)) {
                    if (n < 0.18) put(level, box, x, top + 1, z, Blocks.GRASS.defaultBlockState());
                    else if (n < 0.2) put(level, box, x, top + 1, z, Blocks.FERN.defaultBlockState());
                }
            }
        }
    }

    /** A leafless, twisted dead tree: a trunk of weathered spruce with a few bare limbs. */
    private static void deadTree(WorldGenLevel level, BoundingBox box, int x, int y, int z, RandomSource random) {
        if (!level.getBlockState(new BlockPos(x, y - 1, z)).isFaceSturdy(level, new BlockPos(x, y - 1, z), Direction.UP)) return;
        int h = 4 + random.nextInt(4);
        BlockState log = (random.nextBoolean() ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG).defaultBlockState();
        for (int i = 0; i < h; i++) put(level, box, x, y + i, z, log);
        int limbs = 1 + random.nextInt(3);
        for (int l = 0; l < limbs; l++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int at = y + 2 + random.nextInt(Math.max(1, h - 2));
            int len = 1 + random.nextInt(2);
            for (int k = 1; k <= len; k++) {
                put(level, box, x + d.getStepX() * k, at + (k == len ? 1 : 0), z + d.getStepZ() * k,
                        log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
    }

    /** A rotting trunk lying on the ground. */
    private static void fallenLog(WorldGenLevel level, BoundingBox box, int x, int y, int z, RandomSource random) {
        Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        int len = 3 + random.nextInt(3);
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        for (int k = 0; k < len; k++) {
            int px = x + (axis == Direction.Axis.X ? k : 0), pz = z + (axis == Direction.Axis.Z ? k : 0);
            BlockPos p = new BlockPos(px, y, pz);
            if (!box.isInside(p) || !level.getBlockState(p).isAir() || level.getBlockState(p.below()).isAir()) break;
            put(level, box, px, y, pz, log);
            if (random.nextInt(3) == 0) put(level, box, px, y + 1, pz, Blocks.MOSS_CARPET.defaultBlockState());
        }
    }

    /** A low thicket of spruce boughs, chest to head high. */
    private static void thicket(WorldGenLevel level, BoundingBox box, int x, int y, int z, RandomSource random) {
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        put(level, box, x, y, z, leaves);
        if (random.nextBoolean()) put(level, box, x, y + 1, z, leaves);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(3) == 0 && level.getBlockState(new BlockPos(x + d.getStepX(), y, z + d.getStepZ())).isAir()) {
                put(level, box, x + d.getStepX(), y, z + d.getStepZ(), leaves);
            }
        }
    }

    private static void tallPlant(WorldGenLevel level, BoundingBox box, int x, int y, int z, net.minecraft.world.level.block.Block plant) {
        if (!level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) return;
        put(level, box, x, y, z, plant.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        put(level, box, x, y + 1, z, plant.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
    }

    private static void boulder(WorldGenLevel level, BoundingBox box, int x, int y, int z, RandomSource random) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                for (int oy = 0; oy <= 1; oy++) {
                    if (oy == 1 && (Math.abs(ox) + Math.abs(oz) > 1 || random.nextBoolean())) continue;
                    if (Math.abs(ox) + Math.abs(oz) == 2 && random.nextInt(3) == 0) continue;
                    put(level, box, x + ox, y + oy, z + oz, (random.nextInt(3) == 0 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState());
                }
            }
        }
    }
}
