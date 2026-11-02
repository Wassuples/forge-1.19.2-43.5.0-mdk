package com.wassuples.demonslayer.client.toon;

import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client half of Fake Tunnel. The server never changes a block: each client hides the tunnel's blocks in
 * its own copy of the world (so the 2x2 hole really shows what's behind the wall/mountain), keeps them
 * hidden if the server re-sends them, and puts them back when the tunnel ends.
 */
@OnlyIn(Dist.CLIENT)
public final class ToonTunnelClient {
    /** setBlock flags: re-render (2|8) + skip shape updates (16); no neighbour updates, so torches etc. stay put. */
    private static final int CLIENT_ONLY_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE;

    private static final Map<Integer, Tunnel> TUNNELS = new HashMap<>();
    /** packed BlockPos -> owner entity id. Read from the collision mixin, possibly off-thread, hence concurrent. */
    private static final Map<Long, Integer> CELL_OWNERS = new ConcurrentHashMap<>();
    private static volatile boolean active;

    private ToonTunnelClient() {
    }

    private static final class Tunnel {
        final int ownerId;
        final List<BlockPos> cells;
        final Map<BlockPos, BlockState> originals = new HashMap<>();

        Tunnel(int ownerId, List<BlockPos> cells) {
            this.ownerId = ownerId;
            this.cells = cells;
        }
    }

    public static boolean hasActiveTunnels() {
        return active;
    }

    /** Called every client tick by each tunnel entity. */
    public static void tick(ToonFakeTunnelEntity entity) {
        if (!(entity.level instanceof ClientLevel level)) return;
        if (entity.isExpiring()) {
            remove(entity.getId(), level);
            return;
        }

        List<BlockPos> cells = entity.getCells();
        if (cells.isEmpty()) return; // synced data not in yet

        Tunnel tunnel = TUNNELS.get(entity.getId());
        if (tunnel == null || tunnel.ownerId != entity.getOwnerEntityId() || !tunnel.cells.equals(cells)) {
            if (tunnel != null) remove(entity.getId(), level);
            tunnel = new Tunnel(entity.getOwnerEntityId(), cells);
            TUNNELS.put(entity.getId(), tunnel);
            for (BlockPos pos : cells) {
                CELL_OWNERS.put(pos.asLong(), tunnel.ownerId);
            }
            active = true;
        }

        for (BlockPos pos : tunnel.cells) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || !current.getFluidState().isEmpty()) continue;
            // Whatever is there now is the server's truth (first sight, or a re-sent/changed block).
            tunnel.originals.put(pos, current);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), CLIENT_ONLY_FLAGS);
        }
    }

    /** Restore the hidden blocks. The server also re-sends the real states right after, as a safety net. */
    public static void remove(int entityId, @Nullable ClientLevel level) {
        Tunnel tunnel = TUNNELS.remove(entityId);
        if (tunnel == null) return;
        for (BlockPos pos : tunnel.cells) {
            CELL_OWNERS.remove(pos.asLong());
        }
        if (level != null) {
            tunnel.originals.forEach((pos, state) -> {
                if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, state, CLIENT_ONLY_FLAGS);
                }
            });
        }
        // Another tunnel may share cells; re-register its cells.
        TUNNELS.values().forEach(other -> other.cells.forEach(pos -> CELL_OWNERS.put(pos.asLong(), other.ownerId)));
        active = !TUNNELS.isEmpty();
    }

    public static void onEntityRemoved(ToonFakeTunnelEntity entity) {
        remove(entity.getId(), entity.level instanceof ClientLevel level ? level : null);
    }

    /** World unload / disconnect: the client world is gone, just forget everything. */
    public static void clear() {
        TUNNELS.clear();
        CELL_OWNERS.clear();
        active = false;
    }

    /**
     * Full block for anyone but the caster inside a hidden tunnel cell, so they bump into it exactly like the
     * server's real wall. Null = no override.
     */
    @Nullable
    public static VoxelShape collisionOverride(BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) return null;
        Entity entity = entityContext.getEntity();
        if (entity == null || !entity.level.isClientSide) return null; // never touch server-side physics
        Integer owner = CELL_OWNERS.get(pos.asLong());
        if (owner == null || owner == entity.getId()) return null;
        return Shapes.block();
    }
}
