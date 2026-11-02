package com.wassuples.demonslayer.worldgen.fujikasane;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Geometry of Mt. Fujikasane, shared by world generation ({@link FujikasaneMountainPiece}) and the Final Selection
 * mission. Everything is a pure function of the centre, the base height and the world seed, so the mission can reason
 * about the mountain without loading it.
 *
 * <pre>
 *   r 0 .. 72    the mountaintop: a broad, rugged dome (about 32..80 blocks up) with rock spires, no wisteria,
 *                demons penned in by the wisteria below (DEMON_R)
 *   r 72 .. 94   the slope terrace (~26..32 up): wisteria, and the shrine plaza on the south side (r 72..92)
 *   r 94 .. 124  the foot: wisteria all the way down; the south stairway (torii, lanterns) runs from the plaza
 *                down to r 118, then a path to the edge
 * </pre>
 * Coordinates: dx/dz are offsets from the centre; +z is south.
 */
public final class FujikasaneLayout {
    /** Footprint radius. A structure's chunks are only built within 8 chunks of its start, so 128 is the ceiling. */
    public static final int R = 128;
    /** Headroom above the base (peak ~80 plus spires). */
    public static final int H = 100;
    public static final int DEMON_R = 72;
    public static final int PLAZA_DIST = 82;
    public static final int PLAZA_R = 10;
    private static final int FOOT_R = 124, TERRACE_R = 94;
    private static final double SHELF = 26, SUMMIT_EDGE = 32, PEAK = 48;
    /**
     * Where the south stairway leaves the plaza, and where its stone steps end. Below that (the outer rim, where the
     * mountain blends into whatever land surrounds it) a trodden path continues to the foot.
     */
    public static final int STAIR_TOP = PLAZA_DIST + PLAZA_R, STAIR_BOTTOM = 118;
    /** North stairs: from the plaza's north edge up to the summit terrain. */
    public static final int NORTH_STAIR_START = PLAZA_DIST - PLAZA_R;
    public static final int STAIR_HALF_WIDTH = 2;
    /** Summit landmarks: the Hand Demon's den and a ruined hut. */
    public static final int DEN_X = -24, DEN_Z = -30, HUT_X = 38, HUT_Z = -14;

    public final int centerX, centerZ, baseY;
    private final long seed;
    private final int plazaY;
    private final int[] southStairs = new int[STAIR_BOTTOM - STAIR_TOP + 1];
    private final int[] northStairs = new int[NORTH_STAIR_START + 1];
    private int northStairEnd;

    public FujikasaneLayout(int centerX, int centerZ, int baseY, long seed) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.baseY = baseY;
        this.seed = seed ^ ((long) centerX * 341873128712L) ^ ((long) centerZ * 132897987541L);
        this.plazaY = baseY + (int) Math.round(profile(0, PLAZA_DIST));

        // South stairway: never climbs going down, drops at most one block per step, hugs the terrain.
        int y = plazaY;
        for (int dz = STAIR_TOP; dz <= STAIR_BOTTOM; dz++) {
            int terrain = baseY + (int) Math.round(profile(0, dz));
            y = Math.max(y - 1, Math.min(y, terrain));
            southStairs[dz - STAIR_TOP] = y;
        }
        // North stairs: climb at most one block per step from the plaza until they meet the summit terrain.
        y = plazaY;
        northStairEnd = 0;
        for (int dz = NORTH_STAIR_START; dz >= 0; dz--) {
            int terrain = baseY + (int) Math.round(profile(0, dz));
            if (y >= terrain) {
                northStairs[dz] = y;
                northStairEnd = dz;
                break;
            }
            y = Math.min(terrain, y + 1);
            northStairs[dz] = y;
            northStairEnd = dz;
        }
    }

    // ================================================================== base height

    /** Foot level: average surface height around the mountain's rim, never below sea level. */
    public static int computeBaseY(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random, int cx, int cz) {
        int sum = 0, n = 0;
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            int x = cx + (int) Math.round(Math.cos(a) * R * 0.9);
            int z = cz + (int) Math.round(Math.sin(a) * R * 0.9);
            sum += generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, random);
            n++;
        }
        int base = Math.max(generator.getSeaLevel() + 1, sum / n);
        return Math.min(base, heights.getMaxBuildHeight() - H - 30);
    }

    // ================================================================== shape

    private double hash(int x, int z, long salt) {
        long h = x * 374761393L + z * 668265263L + (seed + salt) * 2654435761L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }

    private double noise(double x, double z, double cell, long salt) {
        int gx = (int) Math.floor(x / cell), gz = (int) Math.floor(z / cell);
        double fx = x / cell - gx, fz = z / cell - gz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(gx, gz, salt), b = hash(gx + 1, gz, salt), c = hash(gx, gz + 1, salt), d = hash(gx + 1, gz + 1, salt);
        double top = a + (b - a) * fx, bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** The radial shape: foot rising to the terrace, terrace to the summit's edge, then a broad dome. */
    private static double radial(double r) {
        if (r >= FOOT_R) return 0;
        if (r >= TERRACE_R) return SHELF * smooth((FOOT_R - r) / (FOOT_R - TERRACE_R));
        if (r >= DEMON_R) return SHELF + (SUMMIT_EDGE - SHELF) * smooth((TERRACE_R - r) / (TERRACE_R - DEMON_R));
        return SUMMIT_EDGE + PEAK * smooth(1 - r / DEMON_R);
    }

    /** Mountain height above the base at an offset (no surrounding ground), in blocks. */
    public double profile(double dx, double dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r >= R) return 0;
        double edge = Math.min(1, (R - r) / 20);
        double rugged = (noise(dx, dz, 22, 1) - 0.5) * 10 * (r < DEMON_R ? 1 : 0.5) * edge;
        double fine = (noise(dx, dz, 7, 7) - 0.5) * 3 * edge;
        return Math.max(0, radial(r) + rugged + fine + spire(dx, dz));
    }

    /** Smooth 0..1 field marking where groves stand thick on the summit. */
    public double grove(int dx, int dz) {
        return noise(dx, dz, 20, 91);
    }

    /** Ground height ignoring the rock outcrops (for places NPCs walk to). */
    public int walkY(int dx, int dz) {
        return (int) Math.round(baseY + profile(dx, dz) - spire(dx, dz));
    }

    /** Height of the summit's rock outcrops at this column (0 where there is none). */
    public double spire(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz) < DEMON_R - 6 ? Math.max(0, noise(dx, dz, 13, 3) - 0.78) * 60 : 0;
    }

    /** Steepness (blocks of rise per block) of the bare mountain profile. */
    public double slope(int dx, int dz) {
        return (Math.abs(profile(dx + 1, dz) - profile(dx - 1, dz)) + Math.abs(profile(dx, dz + 1) - profile(dx, dz - 1))) * 0.5;
    }

    /** Value in [0,1) for deterministic per-column choices (materials, scatter). */
    public double scatter(int dx, int dz, long salt) {
        return hash(dx, dz, 1000 + salt);
    }

    // ================================================================== zones

    public static double dist(int dx, int dz) {
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    public boolean inPlaza(int dx, int dz) {
        return dist(dx, dz - PLAZA_DIST) <= PLAZA_R;
    }

    public boolean onSouthStairs(int dx, int dz) {
        return Math.abs(dx) <= STAIR_HALF_WIDTH && dz >= STAIR_TOP && dz <= STAIR_BOTTOM;
    }

    public boolean onNorthStairs(int dx, int dz) {
        return Math.abs(dx) <= STAIR_HALF_WIDTH && dz < NORTH_STAIR_START && dz >= northStairEnd;
    }

    /** The shrine building's footprint on the west side of the plaza (it faces east). */
    public boolean inShrine(int dx, int dz) {
        return dx >= -9 && dx <= -4 && Math.abs(dz - PLAZA_DIST) <= 4;
    }

    public boolean inDemonZone(int dx, int dz) {
        return dist(dx, dz) < DEMON_R && !inPlaza(dx, dz);
    }

    public boolean onMountain(int dx, int dz) {
        return dist(dx, dz) <= R;
    }

    /** Wisteria grows from the foot to the slope, but not on the paths, the plaza or its terrace. */
    public boolean wisteriaAllowed(int dx, int dz) {
        double r = dist(dx, dz);
        if (r < DEMON_R + 4 || r > R - 5) return false;
        if (Math.abs(dx) <= STAIR_HALF_WIDTH + 4 && dz >= NORTH_STAIR_START - 4) return false;   // stairs and foot path
        return dist(dx, dz - PLAZA_DIST) > PLAZA_R + 3;
    }

    // ================================================================== heights

    public int plazaY() {
        return plazaY;
    }

    /** Walking height (top block) of the south stairway at {@code dz}. */
    public int southStairY(int dz) {
        return southStairs[Math.max(0, Math.min(southStairs.length - 1, dz - STAIR_TOP))];
    }

    public int northStairY(int dz) {
        return northStairs[Math.max(northStairEnd, Math.min(NORTH_STAIR_START, dz))];
    }

    /**
     * Target surface height (absolute Y of the top block) for natural (unbuilt) columns, before blending with the
     * surrounding ground. The plaza terrace is folded in here.
     */
    public int naturalY(int dx, int dz) {
        double h = baseY + profile(dx, dz);
        // the slopes either side of a stairway ease into it, so the path is never a trench or a causeway
        int ax = Math.abs(dx);
        if (ax <= STAIR_HALF_WIDTH + 5) {
            double k = Math.max(0, (ax - STAIR_HALF_WIDTH) / 5.0);
            if (dz >= STAIR_TOP && dz <= STAIR_BOTTOM) h = southStairY(dz) + (h - southStairY(dz)) * k;
            else if (dz < NORTH_STAIR_START && dz >= northStairEnd) h = northStairY(dz) + (h - northStairY(dz)) * k;
        }
        double d = dist(dx, dz - PLAZA_DIST);
        if (d < PLAZA_R + 6) {
            double k = Math.max(0, (d - PLAZA_R) / 6.0);
            h = plazaY + (h - plazaY) * k;
        }
        return (int) Math.round(h);
    }

    /** Northernmost row of the north stairs (where they meet the summit). */
    public int northStairEnd() {
        return northStairEnd;
    }

    /** Surface height of any column, built or natural. */
    public int surfaceY(int dx, int dz) {
        if (inPlaza(dx, dz)) return plazaY;
        if (onSouthStairs(dx, dz)) return southStairY(dz);
        if (onNorthStairs(dx, dz)) return northStairY(dz);
        return naturalY(dx, dz);
    }

    // ================================================================== landmarks (absolute)

    public BlockPos plazaCenter() {
        return new BlockPos(centerX, plazaY + 1, centerZ + PLAZA_DIST);
    }

    /** Where the twins stand: in front of the shrine, facing east across the plaza. */
    public BlockPos twinPos(boolean kanata) {
        return new BlockPos(centerX - 2, plazaY + 1, centerZ + PLAZA_DIST + (kanata ? 1 : -1));
    }

    /** Spots round the plaza (east, south and north sides; the hall is to the west) for the anonymous candidates. */
    public BlockPos plazaRingSpot(int index) {
        double a = -Math.PI * 0.4 + index * (Math.PI * 1.25 / 11.0) - Math.PI * 0.1;
        double r = 7.5 + (index % 2);
        return new BlockPos(centerX + (int) Math.round(Math.cos(a) * r), plazaY + 1, centerZ + PLAZA_DIST + (int) Math.round(Math.sin(a) * r));
    }

    /** Places around the plaza where the other candidates wait. */
    public BlockPos candidateSpot(int index) {
        double a = Math.PI * 0.15 + index * 0.55;
        return new BlockPos(centerX + (int) Math.round(Math.cos(a) * 6), plazaY + 1, centerZ + PLAZA_DIST + (int) Math.round(Math.sin(a) * 6) - 1);
    }

    /**
     * Where candidate {@code index} keeps watch on the summit on a given night: spread round the mountaintop (about 22
     * to 52 blocks out, ~72 degrees apart), turning a little each night so nobody holds the same ground twice.
     */
    public BlockPos summitPost(int index, int night) {
        double ang = index * 2.39996 + night * 0.9 + 0.4;   // golden angle: no two candidates share a bearing
        double r = 20 + ((index / 5 + index * 3 + night * 2) % 6) * 6.5;
        int dx = 0, dz = 0;
        for (int k = 0; k < 8; k++) {   // step off any rock outcrop
            dx = (int) Math.round(Math.cos(ang + k * 0.15) * r);
            dz = (int) Math.round(Math.sin(ang + k * 0.15) * r);
            if (spire(dx, dz) < 1.0) break;
        }
        return new BlockPos(centerX + dx, naturalY(dx, dz) + 1, centerZ + dz);
    }

    /** Where Zenitsu hides during the selection: in the wisteria just below the summit's east edge. */
    public BlockPos zenitsuHideout() {
        int dx = DEMON_R + 6, dz = -6;
        return new BlockPos(centerX + dx, naturalY(dx, dz) + 1, centerZ + dz);
    }

    public BlockPos den() {
        return new BlockPos(centerX + DEN_X, naturalY(DEN_X, DEN_Z) - 2, centerZ + DEN_Z);
    }

    public BlockPos hut() {
        return new BlockPos(centerX + HUT_X, naturalY(HUT_X, HUT_Z) + 2, centerZ + HUT_Z);
    }

    public BlockPos summit() {
        return new BlockPos(centerX, baseY + (int) Math.round(profile(0, 0)) + 1, centerZ);
    }

    public int dx(BlockPos pos) {
        return pos.getX() - centerX;
    }

    public int dz(BlockPos pos) {
        return pos.getZ() - centerZ;
    }
}
