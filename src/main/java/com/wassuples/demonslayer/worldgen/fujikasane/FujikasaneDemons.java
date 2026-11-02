package com.wassuples.demonslayer.worldgen.fujikasane;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The mountaintop is demon-infested. Every three seconds at night this tops up the captive demons around everyone on
 * the summit:
 * <ul>
 *   <li>around a candidate whose Final Selection is under way: up to {@code 3 + night / 2} close by at once, drawn
 *       from that night's budget ({@code 10 + 4 x night} demons: 14 on the first night, 38 on the seventh). Linger in
 *       one spot and the pack finds you regardless: more of them, two at a time, budget or not;</li>
 *   <li>around each NPC candidate on the summit during a selection: one or two of their own;</li>
 *   <li>around anyone else who climbs up at night: three.</li>
 * </ul>
 * Demons appear out of sight (14+ blocks away), in the dark, well inside the summit, never in incense smoke.
 * Total cap: {@link #CAP}.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FujikasaneDemons {
    public static final int CAP = 60;

    private FujikasaneDemons() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 60 != 21) return;
        long tod = level.getDayTime() % 24000L;
        if (tod < 13000L || tod > 23000L) return;
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        if (layout == null) return;
        int r = FujikasaneLayout.R;
        AABB mountain = new AABB(layout.centerX - r, level.getMinBuildHeight(), layout.centerZ - r, layout.centerX + r, level.getMaxBuildHeight(), layout.centerZ + r);
        int total = level.getEntitiesOfClass(CaptiveDemonEntity.class, mountain, LivingEntity::isAlive).size();
        if (total >= CAP) return;
        RandomSource random = level.getRandom();

        List<ServerPlayer> trials = FinalSelectionMission.trialPlayers(level.getServer());
        int selectionNight = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !inSummit(layout, p)) continue;
            boolean trial = trials.contains(p);
            if (!trial) {
                if (!p.isCreative()) total += topUp(level, layout, p, 32, 3, 1, 1, random, CAP - total, null);
                continue;
            }
            int night = Math.max(1, FinalSelectionMission.activeNight(p));
            selectionNight = Math.max(selectionNight, night);
            boolean lingering = FinalSelectionMission.isLingering(p);
            int want = 3 + night / 2 + (lingering ? 3 : 0);
            total += topUp(level, layout, p, 36, want, lingering ? 2 : 1, night, random, CAP - total, lingering ? null : p);
        }
        if (selectionNight > 0) {
            for (StoryNpcEntity npc : level.getEntitiesOfClass(StoryNpcEntity.class, mountain, n -> n.isAlive() && n.character().fighter() && !n.isHiding())) {
                if (!layout.inDemonZone(layout.dx(npc.blockPosition()), layout.dz(npc.blockPosition()))) continue;
                int want = npc.character().named() ? 1 + selectionNight / 3 : 1;
                total += topUp(level, layout, npc, 18, want, 1, selectionNight, random, CAP - total, null);
                if (total >= CAP) break;
            }
        }
    }

    private static boolean inSummit(FujikasaneLayout layout, Entity e) {
        return layout.inDemonZone(layout.dx(e.blockPosition()), layout.dz(e.blockPosition())) && e.getY() > layout.baseY;
    }

    /** Spawns up to {@code batch} demons near {@code focus} if fewer than {@code want} are within {@code radius}. */
    private static int topUp(ServerLevel level, FujikasaneLayout layout, Entity focus, int radius, int want, int batch, int night,
                             RandomSource random, int room, @Nullable ServerPlayer budgetOf) {
        if (room <= 0) return 0;
        int near = level.getEntitiesOfClass(CaptiveDemonEntity.class, focus.getBoundingBox().inflate(radius), LivingEntity::isAlive).size();
        int spawned = 0;
        for (int i = 0; i < Math.min(batch, want - near) && spawned < room; i++) {
            BlockPos at = findSpot(level, layout, focus, random);
            if (at == null) continue;
            if (budgetOf != null && !FinalSelectionMission.takeSpawn(budgetOf)) return spawned;   // tonight's demons are spent
            CaptiveDemonEntity demon = ModEntityTypes.CAPTIVE_DEMON.get().create(level);
            if (demon == null) return spawned;
            demon.setVariant(CaptiveDemonEntity.randomSkin(random));
            demon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360F, 0F);
            demon.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            demon.setTier(night);
            level.addFreshEntity(demon);
            spawned++;
        }
        return spawned;
    }

    @Nullable
    private static BlockPos findSpot(ServerLevel level, FujikasaneLayout layout, Entity focus, RandomSource random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = 14 + random.nextDouble() * 16;
            int x = focus.getBlockX() + (int) (Math.cos(ang) * dist), z = focus.getBlockZ() + (int) (Math.sin(ang) * dist);
            int dx = x - layout.centerX, dz = z - layout.centerZ;
            if (!layout.inDemonZone(dx, dz) || FujikasaneLayout.dist(dx, dz) > FujikasaneLayout.DEMON_R - 10) continue;
            if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBlockState(top).isAir() || !level.getBlockState(top.above()).isAir()) continue;
            if (!level.getFluidState(top.below()).isEmpty()) continue;
            if (level.getMaxLocalRawBrightness(top) > 7) continue;
            if (level.getNearestPlayer(top.getX(), top.getY(), top.getZ(), 12.0D, false) != null) continue;
            if (com.wassuples.demonslayer.missions.IncenseManager.inCloud(level, net.minecraft.world.phys.Vec3.atCenterOf(top))) continue;
            return top;
        }
        return null;
    }
}
