package com.wassuples.demonslayer.entity.ally;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility for determining whether two entities should be treated as allies because they share the
 * same owner UUID (summoner).
 *
 * Contract:
 * - getOwnerUuid(entity): best-effort extraction of an "owner" UUID for any summon-like entity.
 * - areAllies(a, b): true if both have non-null owner UUIDs and they match.
 * - isOwnedBy(target, owner): true if target's owner UUID == owner's UUID.
 */
public final class SummonAllies {
    private SummonAllies() {}

    @Nullable
    public static UUID getOwnerUuid(@Nullable Entity entity) {
        if (entity == null) return null;

        // Vanilla tameables
        if (entity instanceof TamableAnimal tamable) {
            return tamable.getOwnerUUID();
        }

        // Projectile API (covers arrows, tridents, your custom projectiles that extend Projectile)
        // Uses getOwner() which may be null.
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            Entity owner = projectile.getOwner();
            return owner != null ? owner.getUUID() : null;
        }

        // Your custom owned summons: look for a no-arg getOwnerUUID() method returning UUID.
        // This avoids adding an interface to every entity and keeps edits tiny.
        try {
            var m = entity.getClass().getMethod("getOwnerUUID");
            Object v = m.invoke(entity);
            if (v instanceof UUID uuid) return uuid;
        } catch (ReflectiveOperationException ignored) {
        }

        // Some of your entities use getOwner() returning UUID
        try {
            var m = entity.getClass().getMethod("getOwner");
            Object v = m.invoke(entity);
            if (v instanceof UUID uuid) return uuid;
            if (v instanceof LivingEntity le) return le.getUUID();
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    public static boolean areAllies(@Nullable Entity a, @Nullable Entity b) {
        UUID oa = getOwnerUuid(a);
        if (oa == null) return false;
        UUID ob = getOwnerUuid(b);
        return oa.equals(ob);
    }

    public static boolean isOwnedBy(@Nullable Entity target, @Nullable LivingEntity owner) {
        if (target == null || owner == null) return false;
        UUID ot = getOwnerUuid(target);
        return ot != null && ot.equals(owner.getUUID());
    }
}

