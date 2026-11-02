package com.wassuples.demonslayer.worldgen.fujikasane;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.worldgen.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where this world's Mt. Fujikasane is (overworld SavedData), plus the UUIDs of the characters who live at its shrine.
 *
 * <p>The mountain is found once with the structure locator (cheap for the single-ring placement) and remembered. Worlds
 * whose ring position was generated before the mountain existed have none: an operator can raise one with
 * {@code /demonslayer fujikasane place}, which is recorded here as well.
 */
public final class FujikasaneSites extends SavedData {
    private static final String NAME = DemonSlayer.MOD_ID + "_fujikasane";
    private static final long RETRY_TICKS = 20L * 60 * 10;

    private boolean known;
    private int centerX, centerZ, baseY;
    private long lastSearch = Long.MIN_VALUE / 2;
    private final Map<StoryCharacter, UUID> residents = new EnumMap<>(StoryCharacter.class);
    @Nullable
    private FujikasaneLayout layout;

    public static FujikasaneSites get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(FujikasaneSites::load, FujikasaneSites::new, NAME);
    }

    /** The mountain's layout, searching for it if not known yet (at most every ten minutes). Null if there is none. */
    @Nullable
    public static FujikasaneLayout find(MinecraftServer server) {
        FujikasaneSites sites = get(server);
        ServerLevel overworld = server.overworld();
        if (!sites.known) {
            long now = overworld.getGameTime();
            if (now - sites.lastSearch >= 0 && now - sites.lastSearch < RETRY_TICKS) return null;
            sites.lastSearch = now;
            sites.locate(overworld);
        }
        return sites.layout(overworld);
    }

    /** The layout if already known (never searches). */
    @Nullable
    public static FujikasaneLayout known(MinecraftServer server) {
        FujikasaneSites sites = get(server);
        return sites.known ? sites.layout(server.overworld()) : null;
    }

    @Nullable
    private FujikasaneLayout layout(ServerLevel overworld) {
        if (!known) return null;
        if (layout == null) layout = new FujikasaneLayout(centerX, centerZ, baseY, overworld.getSeed());
        return layout;
    }

    private void locate(ServerLevel overworld) {
        BlockPos from = overworld.getSharedSpawnPos();
        BlockPos found = overworld.findNearestMapStructure(ModStructures.FUJIKASANE_TAG, from, 100, false);
        if (found == null) return;
        // the locator returns the start chunk's middle; that is exactly the mountain's centre
        ChunkPos chunk = new ChunkPos(found);
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        int base = FujikasaneLayout.computeBaseY(overworld.getChunkSource().getGenerator(), overworld, overworld.getChunkSource().randomState(), cx, cz);
        setSite(cx, cz, base);
    }

    private void setSite(int cx, int cz, int base) {
        this.known = true;
        this.centerX = cx;
        this.centerZ = cz;
        this.baseY = base;
        this.layout = null;
        this.residents.clear();
        setDirty();
    }

    // ------------------------------------------------------------------ residents

    @Nullable
    public UUID resident(StoryCharacter character) {
        return residents.get(character);
    }

    public void setResident(StoryCharacter character, UUID id) {
        residents.put(character, id);
        setDirty();
    }

    // ------------------------------------------------------------------ manual placement

    /**
     * Raises the mountain centred on {@code centre} in an existing world, chunk by chunk, and records it. Heavy (it
     * loads and rewrites ~256 chunks); for operators fixing worlds made before the mountain existed.
     */
    public static FujikasaneLayout placeAt(ServerLevel level, BlockPos centre) {
        int cx = centre.getX(), cz = centre.getZ();
        int sum = 0, n = 0;
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            int x = cx + (int) Math.round(Math.cos(a) * FujikasaneLayout.R * 0.9);
            int z = cz + (int) Math.round(Math.sin(a) * FujikasaneLayout.R * 0.9);
            level.getChunk(x >> 4, z >> 4);
            sum += level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            n++;
        }
        int base = Math.min(Math.max(level.getSeaLevel() + 1, sum / n), level.getMaxBuildHeight() - FujikasaneLayout.H - 30);
        FujikasaneMountainPiece piece = new FujikasaneMountainPiece(cx, cz, base, level.getMinBuildHeight());
        RandomSource random = RandomSource.create(level.getSeed() ^ cx * 31L ^ cz);
        int r = FujikasaneLayout.R;
        for (int chx = (cx - r) >> 4; chx <= (cx + r) >> 4; chx++) {
            for (int chz = (cz - r) >> 4; chz <= (cz + r) >> 4; chz++) {
                ChunkPos pos = new ChunkPos(chx, chz);
                level.getChunk(chx, chz);
                BoundingBox box = new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                        pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), random, box, pos, centre);
            }
        }
        FujikasaneSites sites = get(level.getServer());
        sites.setSite(cx, cz, base);
        return sites.layout(level);
    }

    // ------------------------------------------------------------------ persistence

    public FujikasaneSites() {
    }

    public static FujikasaneSites load(CompoundTag tag) {
        FujikasaneSites s = new FujikasaneSites();
        s.known = tag.getBoolean("Known");
        s.centerX = tag.getInt("CX");
        s.centerZ = tag.getInt("CZ");
        s.baseY = tag.getInt("BaseY");
        CompoundTag res = tag.getCompound("Residents");
        for (StoryCharacter c : StoryCharacter.values()) {
            if (res.hasUUID(c.name())) s.residents.put(c, res.getUUID(c.name()));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Known", known);
        tag.putInt("CX", centerX);
        tag.putInt("CZ", centerZ);
        tag.putInt("BaseY", baseY);
        CompoundTag res = new CompoundTag();
        residents.forEach((c, id) -> res.putUUID(c.name(), id));
        tag.put("Residents", res);
        return tag;
    }

    /** True when {@code level} is the overworld (the only dimension with a Mt. Fujikasane). */
    public static boolean isMountainLevel(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }
}
