package com.wassuples.demonslayer.entity.tracking;

import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks all Crystalline Divine Child clones by owner UUID so they can be cleaned up without relying
 * on radius-based entity queries.
 *
 * Contract:
 * - register(clone, owner): call when a clone is created/assigned an owner (server side).
 * - unregister(clone): call when a clone is removed/invalidated (server side).
 * - removeAllForOwner(level, ownerId): discards every tracked clone for the given owner.
 *
 * Notes:
 * - The tracker stores clone UUIDs, and resolves entities from the ServerLevel when cleaning up.
 * - If an entity isn't loaded, it will naturally be cleaned up when the chunk is loaded and the
 *   clone is registered again.
 */
public final class CrystallineDivineChildTracker {
    private static final ConcurrentMap<UUID, Set<UUID>> CLONES_BY_OWNER = new ConcurrentHashMap<>();

    private CrystallineDivineChildTracker() {
    }

    public static void register(CrystallineDivineChildEntity clone, @Nullable LivingEntity owner) {
        if (clone == null || owner == null) return;
        UUID ownerId = owner.getUUID();
        UUID cloneId = clone.getUUID();
        CLONES_BY_OWNER.compute(ownerId, (k, existing) -> {
            Set<UUID> set = (existing == null) ? Collections.synchronizedSet(new HashSet<>()) : existing;
            set.add(cloneId);
            return set;
        });
    }

    public static void unregister(CrystallineDivineChildEntity clone) {
        if (clone == null) return;
        // Try to remove from whichever owner set it might be in.
        UUID cloneId = clone.getUUID();
        CLONES_BY_OWNER.forEach((ownerId, set) -> {
            set.remove(cloneId);
            if (set.isEmpty()) {
                CLONES_BY_OWNER.remove(ownerId, set);
            }
        });
    }

    public static void removeAllForOwner(ServerLevel level, UUID ownerId) {
        if (level == null || ownerId == null) return;

        Set<UUID> cloneIds = CLONES_BY_OWNER.remove(ownerId);
        if (cloneIds == null || cloneIds.isEmpty()) return;

        // Copy to avoid concurrent modification if something tries to unregister mid-loop.
        UUID[] ids = cloneIds.toArray(new UUID[0]);
        for (UUID id : ids) {
            Entity e = level.getEntity(id);
            if (e instanceof CrystallineDivineChildEntity clone) {
                clone.discard();
            }
        }
    }
}

