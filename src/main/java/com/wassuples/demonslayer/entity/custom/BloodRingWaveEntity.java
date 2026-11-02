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
 * Visual for Rotating Circular Slashes: Flying Blood Sickles. A ring of spinning blood sickles bursts out from
 * Gyutaro and sweeps outward, leaving crimson arcs in the air. Its radius follows exactly the same timeline the
 * form uses for damage ({@code maxRadius * age / duration}). Purely visual; follows the caster; never saved.
 */
public class BloodRingWaveEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(BloodRingWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(BloodRingWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(BloodRingWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(BloodRingWaveEntity.class, EntityDataSerializers.FLOAT);

    public BloodRingWaveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public BloodRingWaveEntity(Level level, LivingEntity owner, int duration, float maxRadius, float height) {
        this(ModEntityTypes.BLOOD_RING_WAVE.get(), level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(DURATION, duration);
        this.entityData.set(MAX_RADIUS, maxRadius);
        this.entityData.set(HEIGHT, height);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    @Nullable
    public Entity getOwner() {
        return this.level.getEntity(this.entityData.get(OWNER_ID));
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public float getWaveHeight() {
        return this.entityData.get(HEIGHT);
    }

    /** Same formula the form uses for its damage ring. */
    public float radiusAt(float age) {
        return getMaxRadius() * Math.min(1.0f, age / getDuration());
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        Entity owner = getOwner();
        if (owner != null) this.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (!this.level.isClientSide && (owner == null || this.tickCount > getDuration() + 6)) {
            this.discard();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(getMaxRadius() + 1.5, getWaveHeight() + 1.0, getMaxRadius() + 1.5);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DURATION, 30);
        this.entityData.define(MAX_RADIUS, 15.0f);
        this.entityData.define(HEIGHT, 4.0f);
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
