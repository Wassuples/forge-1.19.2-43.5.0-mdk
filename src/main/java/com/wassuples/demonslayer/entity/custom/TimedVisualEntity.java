package com.wassuples.demonslayer.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Base for purely visual effect entities: no physics, no collision, never saved, ticked by hand, discarded by the
 * server once {@link #lifeTicks()} have passed (or its owner is gone, when it has one and {@link #followsOwner()}).
 * Subclasses define their own synced data and are drawn entirely by their renderer.
 */
public abstract class TimedVisualEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TimedVisualEntity.class, EntityDataSerializers.INT);

    protected TimedVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Total lifetime in ticks; the server discards the entity after this. */
    protected abstract int lifeTicks();

    /** Extra margin around the entity for frustum culling (effects reach far beyond the tiny hitbox). */
    protected abstract double cullRadius();

    /** If true the entity sits on its owner every tick and dies with them. */
    protected boolean followsOwner() {
        return false;
    }

    public void setOwnerId(int id) {
        this.entityData.set(OWNER_ID, id);
    }

    @Nullable
    public Entity getOwner() {
        int id = this.entityData.get(OWNER_ID);
        return id < 0 ? null : this.level.getEntity(id);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        Entity owner = followsOwner() ? getOwner() : null;
        if (owner != null) this.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (!this.level.isClientSide) {
            boolean ownerGone = followsOwner() && (owner == null || !owner.isAlive());
            if (ownerGone || this.tickCount > lifeTicks()) this.discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(cullRadius());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 64.0 + cullRadius() * 2.0;
        return distance < d * d;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        defineVisualData();
    }

    /** Define the subclass's synced fields here. */
    protected abstract void defineVisualData();

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
