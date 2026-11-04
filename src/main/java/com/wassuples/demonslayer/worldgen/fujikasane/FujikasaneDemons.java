package com.wassuples.demonslayer.worldgen.fujikasane;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import com.wassuples.demonslayer.missions.IncenseManager;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The mountaintop's demons at night.
 *
 * <p><b>Population, not waves.</b> The summit holds one population of captive demons, spread over the whole of it
 * (never near the wisteria, never appearing within sight of a candidate): {@code 6 + 2 x night} for every candidate
 * whose selection is under way (8 on the first night, 20 on the seventh), capped at {@link #CAP}. Every demon that
 * appears is drawn from the candidate's budget for that night ({@code FinalSelectionMission#nightBudget}: 15 on the
 * first night, 33 on the seventh); once it is spent, that candidate's share of the mountain is not refilled until the
 * next night. The NPC candidates fight whatever crosses their path; they have no demons of their own.
 *
 * <p><b>Hunting.</b> Every few seconds, if fewer than {@code 1 + night / 2} demons are on a candidate, idle demons from
 * across the summit pick up their scent and come for them. Lingering in one spot (see FinalSelectionMission) brings
 * more of them, more often, with a howl from the direction they come from; but never more demons than the night holds.
 *
 * <p>Anyone else who climbs up at night (no selection under way) meets a handful of demons.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FujikasaneDemons {
    public static final int CAP = 48;
    private static final int PERIOD = 40, VISITOR_POPULATION = 4, KEEP_AWAY = 24, HUNT_RANGE = 90;

    private FujikasaneDemons() {
    }

    /** Demons the summit holds for one candidate on {@code night}. */
    public static int population(int night) {
        return 6 + 2 * night;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        long game = level.getGameTime();
        if (game % PERIOD != 21) return;
        long tod = level.getDayTime() % 24000L;
        if (tod < 13000L || tod > 23000L) return;
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        if (layout == null) return;
        RandomSource random = level.getRandom();

        List<ServerPlayer> onSummit = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && inSummit(layout, p)) onSummit.add(p);
        if (onSummit.isEmpty()) return;
        List<ServerPlayer> trials = new ArrayList<>(FinalSelectionMission.trialPlayers(level.getServer()));
        trials.retainAll(onSummit);
        List<CaptiveDemonEntity> demons = roamers(level, layout);

        if (trials.isEmpty()) {
            boolean visitor = onSummit.stream().anyMatch(p -> !p.isCreative());
            if (visitor && demons.size() < VISITOR_POPULATION) spawn(level, layout, onSummit, random, 1);
            return;
        }

        // ---- keep the population up, from each candidate's budget in turn
        int target = 0;
        for (ServerPlayer p : trials) target += population(Math.max(1, FinalSelectionMission.activeNight(p)));
        target = Math.min(CAP, target);
        int spawned = 0;
        java.util.Collections.shuffle(trials, new java.util.Random(random.nextLong()));
        for (ServerPlayer p : trials) {
            if (demons.size() + spawned >= target || spawned >= 2) break;
            BlockPos at = spreadSpot(level, layout, onSummit, random);
            if (at == null || !FinalSelectionMission.takeSpawn(p)) continue;
            if (spawnAt(level, at, Math.max(1, FinalSelectionMission.activeNight(p)), random) != null) spawned++;
        }

        // ---- the hunt: each candidate in turn, every ten seconds (four when lingering)
        for (ServerPlayer p : trials) {
            boolean lingering = FinalSelectionMission.isLingering(p);
            int every = lingering ? 2 : 5;
            if (((game / PERIOD) + (p.getId() & 7)) % every != 0) continue;
            int night = Math.max(1, FinalSelectionMission.activeNight(p));
            int want = 1 + night / 2 + (lingering ? 2 : 0);
            int sent = sendHunters(level, p, want, lingering ? 3 : 2, false);
            if (lingering && sent > 0 && random.nextInt(2) == 0) howlFrom(level, p);
        }
    }

    // ================================================================== hunting

    /**
     * Sends idle demons after {@code player} until {@code want} are on them (at most {@code max} this time). Returns how
     * many were sent.
     */
    public static int sendHunters(ServerLevel level, ServerPlayer player, int want, int max, boolean frenzied) {
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        if (layout == null) return 0;
        AABB near = player.getBoundingBox().inflate(20.0D, 12.0D, 20.0D);
        int engaged = level.getEntitiesOfClass(CaptiveDemonEntity.class, near, d -> d.isAlive() && (d.getTarget() == player || !d.isIdle())).size();
        if (!frenzied && engaged >= want) return 0;
        List<CaptiveDemonEntity> idle = level.getEntitiesOfClass(CaptiveDemonEntity.class, player.getBoundingBox().inflate(HUNT_RANGE, 48, HUNT_RANGE),
                d -> d.isAlive() && d.isIdle() && d.variant() != CaptiveDemonEntity.HORNED && !d.getPersistentData().contains(CORNER_TAG));
        idle.sort(Comparator.comparingDouble(d -> d.distanceToSqr(player)));
        int n = frenzied ? max : Math.min(max, want - engaged);
        int sent = 0;
        for (CaptiveDemonEntity d : idle) {
            if (sent >= n) break;
            if (frenzied) d.setFrenzied(true);
            d.huntTowards(player.blockPosition(), frenzied ? 1200 : 600);
            sent++;
        }
        return sent;
    }

    /** Persistent-data key of the cornered-candidate quest's demons (they stay on their quarry). */
    public static final String CORNER_TAG = "demonslayer.fs_corner";

    /** Night 6: a pack of starving demons bands together and comes for one candidate at once. */
    public static void sendPack(ServerLevel level, ServerPlayer player, int size) {
        int sent = sendHunters(level, player, size, size, false);
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        if (layout == null) return;
        RandomSource random = level.getRandom();
        double ang = random.nextDouble() * Math.PI * 2;
        for (int i = sent; i < size; i++) {
            for (int attempt = 0; attempt < 8; attempt++) {
                double a = ang + (random.nextDouble() - 0.5) * 0.8, d = 26 + random.nextDouble() * 6;
                BlockPos at = surface(level, layout, player.getBlockX() + (int) Math.round(Math.cos(a) * d), player.getBlockZ() + (int) Math.round(Math.sin(a) * d));
                if (at == null) continue;
                if (!FinalSelectionMission.takeSpawn(player)) return;   // tonight's demons are spent
                CaptiveDemonEntity demon = spawnAt(level, at, Math.max(1, FinalSelectionMission.activeNight(player)), random);
                if (demon != null) demon.huntTowards(player.blockPosition(), 900);
                break;
            }
        }
        howlFrom(level, player);
    }

    /** A howl from out in the dark, from the direction the nearest hunter is coming. */
    private static void howlFrom(ServerLevel level, ServerPlayer player) {
        List<CaptiveDemonEntity> hunters = level.getEntitiesOfClass(CaptiveDemonEntity.class, player.getBoundingBox().inflate(HUNT_RANGE, 48, HUNT_RANGE),
                d -> d.isAlive() && !d.isIdle() && d.distanceToSqr(player) > 12 * 12);
        Vec3 from;
        if (hunters.isEmpty()) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            from = player.position().add(Math.cos(a) * 24, 0, Math.sin(a) * 24);
        } else {
            hunters.sort(Comparator.comparingDouble(d -> d.distanceToSqr(player)));
            from = hunters.get(0).position();
        }
        if (!com.wassuples.demonslayer.sound.HandDemonMusic.silencing(level)) {
            level.playSound(null, from.x, from.y, from.z, ModSounds.DEMON_GROWL.get(), SoundSource.HOSTILE, 2.2F, 0.6F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    // ================================================================== spawning

    /** Captive demons roaming the mountain (the quests' own demons and the horned demon aren't counted). */
    private static List<CaptiveDemonEntity> roamers(ServerLevel level, FujikasaneLayout layout) {
        int r = FujikasaneLayout.R;
        AABB mountain = new AABB(layout.centerX - r, level.getMinBuildHeight(), layout.centerZ - r, layout.centerX + r, level.getMaxBuildHeight(), layout.centerZ + r);
        return level.getEntitiesOfClass(CaptiveDemonEntity.class, mountain,
                d -> d.isAlive() && d.variant() != CaptiveDemonEntity.HORNED && !d.getPersistentData().contains(CORNER_TAG));
    }

    private static boolean inSummit(FujikasaneLayout layout, Player p) {
        return layout.inDemonZone(layout.dx(p.blockPosition()), layout.dz(p.blockPosition())) && p.getY() > layout.baseY;
    }

    private static void spawn(ServerLevel level, FujikasaneLayout layout, List<ServerPlayer> players, RandomSource random, int night) {
        BlockPos at = spreadSpot(level, layout, players, random);
        if (at != null) spawnAt(level, at, night, random);
    }

    @Nullable
    static CaptiveDemonEntity spawnAt(ServerLevel level, BlockPos at, int night, RandomSource random) {
        CaptiveDemonEntity demon = ModEntityTypes.CAPTIVE_DEMON.get().create(level);
        if (demon == null) return null;
        demon.setVariant(CaptiveDemonEntity.randomSkin(random));
        demon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0F);
        demon.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        demon.setTier(night);
        level.addFreshEntity(demon);
        return demon;
    }

    /** A dark spot anywhere on the summit (spread evenly over its area), well short of the wisteria and out of sight. */
    @Nullable
    private static BlockPos spreadSpot(ServerLevel level, FujikasaneLayout layout, List<ServerPlayer> players, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double r = 6 + (CaptiveDemonEntity.ROAM_LIMIT - 6) * Math.sqrt(random.nextDouble());
            int x = layout.centerX + (int) Math.round(Math.cos(ang) * r), z = layout.centerZ + (int) Math.round(Math.sin(ang) * r);
            BlockPos top = surface(level, layout, x, z);
            if (top == null) continue;
            boolean seen = false;
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(Vec3.atCenterOf(top)) < KEEP_AWAY * KEEP_AWAY) {
                    seen = true;
                    break;
                }
            }
            if (!seen) return top;
        }
        return null;
    }

    /** The open, dark, dry ground at a column of the summit (null if it's no place for a demon to appear). */
    @Nullable
    private static BlockPos surface(ServerLevel level, FujikasaneLayout layout, int x, int z) {
        int dx = x - layout.centerX, dz = z - layout.centerZ;
        if (!layout.inDemonZone(dx, dz) || FujikasaneLayout.dist(dx, dz) > CaptiveDemonEntity.ROAM_LIMIT) return null;
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.isPositionEntityTicking(column)) return null;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.getBlockState(top).isAir() || !level.getBlockState(top.above()).isAir()) return null;
        if (!level.getFluidState(top.below()).isEmpty()) return null;
        if (level.getBlockState(top.below()).is(net.minecraft.tags.BlockTags.LOGS) || layout.spire(dx, dz) > 1.0) return null;   // not up a tree or a crag
        if (level.getMaxLocalRawBrightness(top) > 7) return null;
        if (IncenseManager.inCloud(level, Vec3.atCenterOf(top))) return null;
        return top;
    }
}
