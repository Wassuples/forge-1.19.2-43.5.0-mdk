package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handle given to a {@link StoryMission} hook: the owning player, the run's persistent state, and helpers.
 */
public final class MissionContext {
    /** Persistent-data tag on scoped entities: {owner, mission, run, key}. */
    public static final String SCOPE_TAG = "demonslayer.mission";

    private final ServerPlayer player;
    private final PlayerMissions data;
    private final StoryMission mission;
    private boolean dirty;
    private boolean ended;

    MissionContext(ServerPlayer player, PlayerMissions data, StoryMission mission) {
        this.player = player;
        this.data = data;
        this.mission = mission;
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return player.getLevel();
    }

    public PlayerMissions data() {
        return data;
    }

    public StoryMission mission() {
        return mission;
    }

    /** This run's persistent state (saved with the player). */
    public CompoundTag state() {
        return data.getActiveState();
    }

    /** Random id of this run; scoped entities from older runs are recognised and removed. */
    public int runId() {
        return state().getInt("run");
    }

    public void markDirty() {
        dirty = true;
    }

    boolean isDirty() {
        return dirty;
    }

    public boolean hasEnded() {
        return ended;
    }

    void setEnded() {
        ended = true;
    }

    // ------------------------------------------------------------------ messaging (the mod's own HUD, never chat)

    /** A line in the notice scroll under the mission tracker. */
    public void notice(Component message) {
        ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.NOTICE, message, Component.empty(), 0xEDE3E3, 120), player);
    }

    /** A notice drawn in the warning colour (and held a little longer). */
    public void warn(Component message) {
        ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.WARNING, message, Component.empty(), 0xFF7070, 160), player);
    }

    /** The large brush-stroke banner across the upper screen. {@code color} is RGB. */
    public void banner(Component title, @Nullable Component subtitle, int color, int ticks) {
        ModMessages.sendToPlayer(new MissionNoticeS2CPacket(MissionNoticeS2CPacket.BANNER, title,
                subtitle == null ? Component.empty() : subtitle, color, ticks), player);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
    }

    // ------------------------------------------------------------------ scoped entities

    /** Spawns an entity owned by this run under {@code key}; it is removed when the mission ends. */
    public <T extends Entity> T spawnScoped(T entity, String key) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("owner", player.getUUID());
        tag.putString("mission", mission.id().toString());
        tag.putInt("run", runId());
        tag.putString("key", key);
        entity.getPersistentData().put(SCOPE_TAG, tag);
        if (entity instanceof Mob mob) mob.setPersistenceRequired();
        // Record it first: the join check (MissionManager.onEntityJoin) rejects scoped entities this run doesn't know.
        CompoundTag entities = state().getCompound("entities");
        entities.putUUID(key, entity.getUUID());
        state().put("entities", entities);
        level().addFreshEntity(entity);
        markDirty();
        return entity;
    }

    /** The scoped entity under {@code key}, if it exists and is loaded. */
    @Nullable
    public Entity scoped(String key) {
        CompoundTag entities = state().getCompound("entities");
        if (!entities.hasUUID(key)) return null;
        Entity e = level().getEntity(entities.getUUID(key));
        return e != null && e.isAlive() ? e : null;
    }

    @Nullable
    public UUID scopedId(String key) {
        CompoundTag entities = state().getCompound("entities");
        return entities.hasUUID(key) ? entities.getUUID(key) : null;
    }

    public void forgetScoped(String key) {
        CompoundTag entities = state().getCompound("entities");
        entities.remove(key);
        state().put("entities", entities);
    }

    public void discardScoped(String key) {
        Entity e = scoped(key);
        if (e != null) e.discard();
        forgetScoped(key);
    }

    /** Removes every scoped entity of this run, in every loaded dimension; unloaded ones are caught when they load. */
    void discardAllScoped() {
        CompoundTag entities = state().getCompound("entities");
        for (String key : entities.getAllKeys()) {
            if (!entities.hasUUID(key)) continue;
            UUID id = entities.getUUID(key);
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                    break;
                }
            }
        }
        state().remove("entities");
    }

    // ------------------------------------------------------------------ ending

    /** Ends the mission successfully on the given branch. */
    public void complete(String outcome) {
        MissionManager.finish(this, true, outcome, null);
    }

    /** Ends the mission in failure; it can be attempted again. */
    public void fail(Component reason) {
        MissionManager.finish(this, false, null, reason);
    }

    // ------------------------------------------------------------------ world helpers

    /**
     * A standable surface spot {@code min..max} blocks from {@code centre} (solid ground, two free blocks, no fluid,
     * not on leaves), within 10 blocks of the centre's height. Null if none found.
     */
    @Nullable
    public BlockPos findSurfaceSpot(BlockPos centre, int min, int max, int attempts) {
        ServerLevel level = level();
        RandomSource r = level.getRandom();
        for (int i = 0; i < attempts; i++) {
            double ang = r.nextDouble() * Mth.TWO_PI;
            double dist = min + r.nextDouble() * (max - min);
            int x = centre.getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = centre.getZ() + (int) Math.round(Math.sin(ang) * dist);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(top.getY() - centre.getY()) > 10) continue;
            BlockState below = level.getBlockState(top.below());
            if (!below.isFaceSturdy(level, top.below(), net.minecraft.core.Direction.UP) || below.is(BlockTags.LEAVES)) continue;
            if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()) continue;
            if (!level.getBlockState(top).getCollisionShape(level, top).isEmpty()) continue;
            if (!level.getBlockState(top.above()).getCollisionShape(level, top.above()).isEmpty()) continue;
            return top;
        }
        return null;
    }
}
