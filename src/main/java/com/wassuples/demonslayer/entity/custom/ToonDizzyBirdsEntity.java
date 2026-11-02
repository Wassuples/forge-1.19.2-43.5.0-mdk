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

/** Visual-only ring of cartoon birds circling a dazed entity's head. Follows its target; never saved. */
public class ToonDizzyBirdsEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ToonDizzyBirdsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ToonDizzyBirdsEntity.class, EntityDataSerializers.INT);

    public ToonDizzyBirdsEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ToonDizzyBirdsEntity(Level level, LivingEntity target, int durationTicks) {
        this(ModEntityTypes.TOON_DIZZY_BIRDS.get(), level);
        this.entityData.set(TARGET_ID, target.getId());
        this.entityData.set(DURATION, durationTicks);
        this.setPos(target.getX(), target.getY() + target.getBbHeight(), target.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(DURATION, 100);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        Entity target = getTarget();
        if (target != null) {
            this.setPos(target.getX(), target.getY() + target.getBbHeight(), target.getZ());
        }
        if (!this.level.isClientSide && (target == null || !target.isAlive() || this.tickCount >= getDuration())) {
            this.discard();
        }
    }

    @Nullable
    public Entity getTarget() {
        return this.level.getEntity(getTargetId());
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
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
