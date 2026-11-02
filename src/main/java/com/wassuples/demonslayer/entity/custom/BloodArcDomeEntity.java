package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Visual for Rampant Arc Rampage: while Gyutaro's parry window is open, blood sickles whirl around him in every
 * direction like a cage of arcs. If the parry lands, they explode outward. Purely visual (damage and the parry
 * itself are still handled by {@code RampantArcRampageParry}); follows its owner; never saved.
 */
public class BloodArcDomeEntity extends Entity {
    public static final int BURST_TICKS = 10;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(BloodArcDomeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GUARD_TICKS = SynchedEntityData.defineId(BloodArcDomeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BURST_TICK = SynchedEntityData.defineId(BloodArcDomeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BURST_RADIUS = SynchedEntityData.defineId(BloodArcDomeEntity.class, EntityDataSerializers.FLOAT);

    public BloodArcDomeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public BloodArcDomeEntity(Level level, LivingEntity owner, int guardTicks, float burstRadius) {
        this(ModEntityTypes.BLOOD_ARC_DOME.get(), level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(GUARD_TICKS, guardTicks);
        this.entityData.set(BURST_RADIUS, burstRadius);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    /** Parry succeeded: blow the arcs outward. */
    public void burst() {
        if (getBurstTick() < 0) this.entityData.set(BURST_TICK, this.tickCount);
    }

    @Nullable
    public Entity getOwner() {
        return this.level.getEntity(this.entityData.get(OWNER_ID));
    }

    public int getGuardTicks() {
        return this.entityData.get(GUARD_TICKS);
    }

    public int getBurstTick() {
        return this.entityData.get(BURST_TICK);
    }

    public float getBurstRadius() {
        return this.entityData.get(BURST_RADIUS);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        Entity owner = getOwner();
        if (owner != null) this.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (this.level.isClientSide) return;

        int burst = getBurstTick();
        boolean expired = burst >= 0 ? this.tickCount - burst > BURST_TICKS : this.tickCount > getGuardTicks();
        if (owner == null || !owner.isAlive() || expired) {
            this.discard();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(getBurstRadius() + 1.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(GUARD_TICKS, 25);
        this.entityData.define(BURST_TICK, -1);
        this.entityData.define(BURST_RADIUS, 7.0f);
    }

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
