package com.wassuples.demonslayer.worldgen.fujikasane;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The people of Mt. Fujikasane's shrine: the Ubuyashiki twins, who stand before the hall and run the Final Selection,
 * and the five candidates who wait in the plaza with the player.
 *
 * <p>Every five seconds, while a player is near the mountain, this keeps exactly one of each (spawning any that are
 * missing at their places, removing stray copies) and directs them:
 * <ul>
 *   <li>no selection under way: everyone at the shrine (candidates strolling near their spots, twins in place);</li>
 *   <li>someone's selection under way: Tanjiro keeps close to that candidate; the others spread out over the summit
 *       and fight there; when it ends, the survivors gather back at the shrine.</li>
 * </ul>
 * Candidates also talk among themselves out loud now and then when a player is close.
 *
 * <p>The mountaintop is also demon-infested outside any selection: at night a few demons prowl the summit around
 * players who wander up there.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FujikasaneResidents {
    public static final String RESIDENT_TAG = "demonslayer.fujikasane_resident";
    /**
     * Who is on the mountain for the Final Selection, as in the story at this point: the twins and Tanjiro, Zenitsu,
     * Kanao and Genya. (Other characters, Inosuke among them, are kept in {@link StoryCharacter} for later missions.)
     */
    private static final StoryCharacter[] RESIDENTS = {StoryCharacter.KIRIYA, StoryCharacter.KANATA, StoryCharacter.TANJIRO,
            StoryCharacter.ZENITSU, StoryCharacter.KANAO, StoryCharacter.GENYA};
    private static final Map<StoryCharacter, Integer> MISSING = new EnumMap<>(StoryCharacter.class);
    private static long nextBark;

    private FujikasaneResidents() {
    }

    /** The resident playing {@code character}, if loaded. */
    @Nullable
    public static StoryNpcEntity loaded(MinecraftServer server, StoryCharacter character) {
        UUID id = FujikasaneSites.get(server).resident(character);
        if (id == null) return null;
        Entity e = server.overworld().getEntity(id);
        return e instanceof StoryNpcEntity npc && npc.isAlive() ? npc : null;
    }

    /** Singleplayer keeps statics between worlds. */
    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        MISSING.clear();
        nextBark = 0;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FujikasaneSites.find(player.server);   // locate once per world
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 100 != 13) return;
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        if (layout == null) return;
        BlockPos plaza = layout.plazaCenter();
        boolean playerNear = false;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(plaza.getX(), plaza.getY(), plaza.getZ()) < 176 * 176) {
                playerNear = true;
                break;
            }
        }
        // entity sections load separately from blocks: wait for them, or residents would be "missing" and replaced
        if (!playerNear || !level.isLoaded(plaza) || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(plaza))) return;
        keep(level, layout);
    }

    // ------------------------------------------------------------------ the residents

    private static void keep(ServerLevel level, FujikasaneLayout layout) {
        FujikasaneSites sites = FujikasaneSites.get(level.getServer());
        int r = FujikasaneLayout.R + 48;
        AABB area = new AABB(layout.centerX - r, level.getMinBuildHeight(), layout.centerZ - r, layout.centerX + r, level.getMaxBuildHeight(), layout.centerZ + r);
        List<StoryNpcEntity> all = level.getEntitiesOfClass(StoryNpcEntity.class, area, e -> e.getPersistentData().getBoolean(RESIDENT_TAG));
        // residents who don't belong on the mountain any more (worlds from before the roster changed) leave
        all.removeIf(e -> {
            if (java.util.Arrays.asList(RESIDENTS).contains(e.character())) return false;
            e.discard();
            return true;
        });
        List<ServerPlayer> trials = FinalSelectionMission.trialPlayers(level.getServer());
        trials.removeIf(p -> p.level != level);

        int candidateIndex = 0;
        for (StoryCharacter c : RESIDENTS) {
            UUID id = sites.resident(c);
            StoryNpcEntity found = null;
            List<StoryNpcEntity> strays = new ArrayList<>();
            for (StoryNpcEntity e : all) {
                if (e.character() != c) continue;
                if (id != null && e.getUUID().equals(id)) found = e;
                else strays.add(e);
            }
            if (found == null && !strays.isEmpty() && (id == null || MISSING.getOrDefault(c, 0) >= 2)) {
                found = strays.remove(0);   // adopt a copy whose record was lost
                sites.setResident(c, found.getUUID());
            }
            for (StoryNpcEntity stray : strays) stray.discard();
            if (found == null) {
                int missing = id == null ? 99 : MISSING.merge(c, 1, Integer::sum);
                if (missing >= 3) {
                    found = spawn(level, layout, c, candidateIndex);
                    if (found != null) sites.setResident(c, found.getUUID());
                    MISSING.remove(c);
                }
            } else {
                MISSING.remove(c);
            }
            if (found != null) direct(found, layout, c, candidateIndex, trials);
            if (!c.isTwin()) candidateIndex++;
        }
        bark(level, layout, all, trials);
    }

    @Nullable
    private static StoryNpcEntity spawn(ServerLevel level, FujikasaneLayout layout, StoryCharacter c, int candidateIndex) {
        BlockPos home = home(layout, c, candidateIndex);
        if (!level.isLoaded(home)) return null;
        StoryNpcEntity npc = ModEntityTypes.STORY_NPC.get().create(level);
        if (npc == null) return null;
        npc.setCharacter(c);
        npc.getPersistentData().putBoolean(RESIDENT_TAG, true);
        int y = Math.max(home.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.getX(), home.getZ()));
        float yaw = c.isTwin() ? -90F : level.getRandom().nextFloat() * 360F;
        npc.moveTo(home.getX() + 0.5, y, home.getZ() + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(home), MobSpawnType.STRUCTURE, null, null);
        level.addFreshEntity(npc);
        return npc;
    }

    private static BlockPos home(FujikasaneLayout layout, StoryCharacter c, int candidateIndex) {
        if (c == StoryCharacter.KANATA) return layout.twinPos(true);
        if (c == StoryCharacter.KIRIYA) return layout.twinPos(false);
        return layout.candidateSpot(candidateIndex);
    }

    private static void direct(StoryNpcEntity npc, FujikasaneLayout layout, StoryCharacter c, int candidateIndex, List<ServerPlayer> trials) {
        if (c.isTwin()) {
            npc.setCompanion(null);
            npc.setAnchor(home(layout, c, candidateIndex), 0);
            npc.setRestYaw(-90F);   // facing east across the plaza
            return;
        }
        if (trials.isEmpty()) {
            npc.setCompanion(null);
            npc.setFightsHandDemon(false);
            npc.setHiding(false);
            npc.setAnchor(home(layout, c, candidateIndex), 4);
            return;
        }
        if (c == StoryCharacter.ZENITSU) {
            // Zenitsu spends the selection hiding in the wisteria, too terrified to fight
            npc.setCompanion(null);
            npc.setHiding(true);
            npc.setAnchor(layout.zenitsuHideout(), 3);
            return;
        }
        npc.setHiding(false);
        // A selection is under way: each candidate holds their own stretch of the summit, far from the others
        // (the ground they hold shifts every night), and fights whatever comes for them there.
        ServerPlayer focus = trials.get(0);
        int night = FinalSelectionMission.activeNight(focus);
        npc.setAnchor(layout.summitPost(candidateIndex, night), 16);
        if (c == StoryCharacter.TANJIRO) {
            // Tanjiro only comes to a candidate's side once the Hand Demon is out and he has offered to help
            ServerPlayer with = null;
            for (ServerPlayer p : trials) if (FinalSelectionMission.tanjiroFightsWith(p)) with = p;
            npc.setCompanion(with == null ? null : with.getUUID());
            npc.setFightsHandDemon(with != null);
        } else {
            npc.setCompanion(null);
        }
    }

    /** Now and then a candidate says something out loud to the plaza when a player is near. */
    private static void bark(ServerLevel level, FujikasaneLayout layout, List<StoryNpcEntity> all, List<ServerPlayer> trials) {
        if (!trials.isEmpty() || level.getGameTime() < nextBark) return;
        nextBark = level.getGameTime() + 400 + level.getRandom().nextInt(600);
        List<StoryNpcEntity> near = new ArrayList<>();
        for (StoryNpcEntity npc : all) {
            if (npc.isRemoved() || npc.character().isTwin()) continue;
            if (level.getNearestPlayer(npc, 14.0D) != null) near.add(npc);
        }
        if (near.isEmpty()) return;
        StoryNpcEntity speaker = near.get(level.getRandom().nextInt(near.size()));
        int line = level.getRandom().nextInt(3);
        speaker.say(Component.translatable("npc.demonslayer." + speaker.character().key() + ".bark." + line));
    }

    // ------------------------------------------------------------------ strays

    /** A resident copy that isn't the recorded one (e.g. from an old chunk loading late) never joins. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof StoryNpcEntity npc)) return;
        if (!npc.getPersistentData().getBoolean(RESIDENT_TAG) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.loadedFromDisk()) {
            UUID id = FujikasaneSites.get(level.getServer()).resident(npc.character());
            if (id != null && !id.equals(npc.getUUID())) event.setCanceled(true);
        }
    }
}
