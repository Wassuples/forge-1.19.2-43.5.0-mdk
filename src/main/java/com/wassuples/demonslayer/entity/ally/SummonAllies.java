package com.wassuples.demonslayer.entity.ally;

import com.wassuples.demonslayer.entity.custom.MistCloneEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;

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
    private static final int MAX_OWNER_DEPTH = 8;

    private SummonAllies() {}

    @Nullable
    public static UUID getOwnerUuid(@Nullable Entity entity) {
        return getOwnerUuid(entity, 0);
    }

    @Nullable
    private static UUID getOwnerUuid(@Nullable Entity entity, int depth) {
        if (entity == null || depth > MAX_OWNER_DEPTH) return null;
        if (entity instanceof Projectile || entity instanceof MistCloneEntity) return null;

        // Resolve through owner-entity chains first so nested summons inherit the root summoner.
        LivingEntity ownerEntity = getOwnerEntity(entity);
        if (ownerEntity != null) {
            UUID nested = getOwnerUuid(ownerEntity, depth + 1);
            return nested != null ? nested : ownerEntity.getUUID();
        }

        if (entity instanceof TamableAnimal tamable) {
            return tamable.getOwnerUUID();
        }

        UUID ownerId = getOwnerUuidDirect(entity);
        if (ownerId != null && entity.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity owner = serverLevel.getEntity(ownerId);
            if (owner != null && owner != entity) {
                UUID nested = getOwnerUuid(owner, depth + 1);
                return nested != null ? nested : ownerId;
            }
        }

        return ownerId;
    }

    @Nullable
    private static LivingEntity getOwnerEntity(Entity entity) {
        try {
            var m = entity.getClass().getMethod("getOwner");
            Object v = m.invoke(entity);
            if (v instanceof LivingEntity le) return le;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            var m = entity.getClass().getMethod("getPilot");
            Object v = m.invoke(entity);
            if (v instanceof LivingEntity le) return le;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            var m = entity.getClass().getMethod("getCaster");
            Object v = m.invoke(entity);
            if (v instanceof LivingEntity le) return le;
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    @Nullable
    private static UUID getOwnerUuidDirect(Entity entity) {
        try {
            var m = entity.getClass().getMethod("getOwnerUUID");
            Object v = m.invoke(entity);
            if (v instanceof UUID uuid) return uuid;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            var m = entity.getClass().getMethod("getCasterUUID");
            Object v = m.invoke(entity);
            if (v instanceof UUID uuid) return uuid;
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

    /**
     * Targeting-only friendliness check.
     * Summons should not intentionally target their owner or same-host summons.
     */
    public static boolean isFriendlyForTargeting(@Nullable Entity attacker, @Nullable Entity target) {
        if (attacker == null || target == null) return false;
        if (attacker == target) return true;
        if (areAllies(attacker, target)) return true;

        if (target instanceof LivingEntity targetLiving && isOwnedBy(attacker, targetLiving)) {
            return true;
        }
        if (attacker instanceof LivingEntity attackerLiving && isOwnedBy(target, attackerLiving)) {
            return true;
        }

        return false;
    }

    public static boolean canSummonTarget(@Nullable Entity attacker, @Nullable LivingEntity target) {
        return attacker != null
                && target != null
                && target.isAlive()
                && !isFriendlyForTargeting(attacker, target);
    }
}
