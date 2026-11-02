package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.block.LaidBedrollBlock;
import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket;
import com.wassuples.demonslayer.networking.packet.RestStateS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Resting on a bedroll until nightfall (time of day 12600, just before dusk, so the night itself is still lived).
 * <ul>
 *   <li>Only by day, in the overworld, with no monster within 16 blocks and no hit taken in the last 5 seconds.</li>
 *   <li>The bedroll is unrolled on the ground and everyone nearby sees the player lying on it
 *       ({@link RestStateS2CPacket}); the resting player's own screen dims.</li>
 *   <li>Sneaking, moving, being hit, or a monster coming within 10 blocks gets you up.</li>
 *   <li>Time moves on like sleeping: once the share of overworld players resting (on bedrolls or in beds) reaches the
 *       {@code playersSleepingPercentage} game rule, and everyone resting has lain down for 3 seconds.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RestManager {
    public static final long NIGHTFALL = 12600L;
    private static final int SETTLE_TICKS = 60;
    private static final Map<UUID, Rest> RESTING = new HashMap<>();

    private record Rest(Vec3 pos, long start, @Nullable BlockPos mat) {
    }

    private RestManager() {
    }

    public static void tryRest(ServerPlayer player) {
        if (RESTING.containsKey(player.getUUID())) {
            stop(player, Component.translatable("rest.demonslayer.got_up"));
            return;
        }
        ServerLevel level = player.getLevel();
        Component refusal = null;
        long tod = level.getDayTime() % 24000L;
        if (level.dimension() != Level.OVERWORLD) refusal = Component.translatable("rest.demonslayer.refuse.dimension");
        else if (tod >= NIGHTFALL) refusal = Component.translatable("rest.demonslayer.refuse.night");
        else if (!player.isOnGround()) refusal = Component.translatable("rest.demonslayer.refuse.ground");
        else if (!level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0D), m -> m.isAlive()).isEmpty())
            refusal = Component.translatable("rest.demonslayer.refuse.monsters");
        else if (player.getLastHurtByMob() != null && player.tickCount - player.getLastHurtByMobTimestamp() < 100)
            refusal = Component.translatable("rest.demonslayer.refuse.hurt");
        if (refusal != null) {
            MissionManager.notify(player, refusal, true);
            return;
        }
        // unroll the mat where they stand
        BlockPos at = player.blockPosition();
        BlockPos mat = null;
        net.minecraft.core.Direction facing = player.getDirection();
        if (level.getBlockState(at).isAir() && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
            // a player drawn lying down without a bed lies across their facing, head to their right: the pillow goes there
            level.setBlock(at, ModBlocks.LAID_BEDROLL.get().defaultBlockState().setValue(LaidBedrollBlock.FACING, facing.getClockWise()), 3);
            mat = at;
            // settle onto the middle of the mat, squared up with it
            player.connection.teleport(at.getX() + 0.5D, player.getY(), at.getZ() + 0.5D, facing.toYRot(), player.getXRot());
            player.setYBodyRot(facing.toYRot());
            player.setYHeadRot(facing.toYRot());
        }
        RESTING.put(player.getUUID(), new Rest(player.position(), level.getGameTime(), mat));
        player.setDeltaMovement(Vec3.ZERO);
        ModMessages.sendToTrackingAndSelf(new RestStateS2CPacket(player.getId(), true), player);
        ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.REST, Component.translatable("rest.demonslayer.resting"),
                Component.empty(), 0xD8CEC8, 24000), player);
    }

    public static boolean isResting(ServerPlayer player) {
        return RESTING.containsKey(player.getUUID());
    }

    public static boolean isRestingAt(BlockPos pos) {
        for (Rest r : RESTING.values()) if (pos.equals(r.mat())) return true;
        return false;
    }

    /** Gets {@code player} up (with an optional message). */
    private static void stop(ServerPlayer player, @Nullable Component why) {
        Rest rest = RESTING.remove(player.getUUID());
        if (rest == null) return;
        ServerLevel overworld = player.server.overworld();   // the mat is always in the overworld (rests are overworld-only)
        if (rest.mat() != null && overworld.getBlockState(rest.mat()).is(ModBlocks.LAID_BEDROLL.get())) {
            overworld.removeBlock(rest.mat(), false);
        }
        ModMessages.sendToTrackingAndSelf(new RestStateS2CPacket(player.getId(), false), player);
        ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.REST, Component.empty(), Component.empty(), 0, 0), player);
        if (why != null) MissionManager.notify(player, why, false);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RESTING.isEmpty()) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        boolean settled = true;
        java.util.List<ServerPlayer> getUp = new java.util.ArrayList<>();
        for (Iterator<Map.Entry<UUID, Rest>> it = RESTING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Rest> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                Rest r = e.getValue();
                if (r.mat() != null && overworld.getBlockState(r.mat()).is(ModBlocks.LAID_BEDROLL.get())) overworld.removeBlock(r.mat(), false);
                it.remove();
                continue;
            }
            boolean disturbed = p.level != overworld || !p.isAlive() || p.isShiftKeyDown()
                    || p.position().distanceToSqr(e.getValue().pos()) > 1.0D
                    || (p.getLastHurtByMob() != null && p.tickCount - p.getLastHurtByMobTimestamp() < 5)
                    || (now % 20 == 0 && !overworld.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(10.0D), m -> m.isAlive()).isEmpty())
                    || (overworld.getDayTime() % 24000L >= NIGHTFALL && now - e.getValue().start() > SETTLE_TICKS);
            if (disturbed) {
                getUp.add(p);   // stopped after the loop (stop() edits the map)
                continue;
            }
            p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
            if (now - e.getValue().start() < SETTLE_TICKS) settled = false;
        }
        for (ServerPlayer p : getUp) stop(p, Component.translatable("rest.demonslayer.interrupted"));
        if (!settled || RESTING.isEmpty()) return;

        // like sleeping: enough of the overworld's players must be resting (bedroll) or asleep (bed)
        int players = 0, resting = 0;
        for (ServerPlayer p : overworld.players()) {
            if (p.isSpectator()) continue;
            players++;
            if (RESTING.containsKey(p.getUUID()) || p.isSleeping()) resting++;
        }
        int pct = overworld.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (pct > 100) return;
        int needed = Math.max(1, Mth.ceil(players * pct / 100.0F));
        if (resting < needed) {
            if (now % 100 == 0) {
                for (UUID id : RESTING.keySet()) {
                    ServerPlayer r = server.getPlayerList().getPlayer(id);
                    if (r != null) ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.REST,
                            Component.translatable("rest.demonslayer.waiting", resting, needed), Component.empty(), 0xD8CEC8, 24000), r);
                }
            }
            return;
        }
        long day = overworld.getDayTime() / 24000L;
        if (overworld.getDayTime() % 24000L < NIGHTFALL) overworld.setDayTime(day * 24000L + NIGHTFALL);
        for (UUID id : RESTING.keySet().toArray(new UUID[0])) {
            ServerPlayer r = server.getPlayerList().getPlayer(id);
            if (r != null) stop(r, Component.translatable("rest.demonslayer.woke"));
        }
        RESTING.clear();
    }

    /** Someone who walks up later sees a resting player lying down (and nobody else). */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        // always the current state: a watcher who was away (another dimension) may still remember an old rest
        if (event.getTarget() instanceof ServerPlayer rester && event.getEntity() instanceof ServerPlayer watcher) {
            ModMessages.sendToPlayer(new RestStateS2CPacket(rester.getId(), RESTING.containsKey(rester.getUUID())), watcher);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) stop(p, null);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        RESTING.clear();
    }
}
