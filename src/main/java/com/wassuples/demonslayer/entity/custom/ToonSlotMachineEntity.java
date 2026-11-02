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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Pocket's slot machine, as a real thing floating in front of the caster: the same cabinet/reels/caption that
 * used to be HUD-only, now visible to the caster and everyone they're about to hit. Visual only, never saved.
 */
public class ToonSlotMachineEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ToonSlotMachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESULT = SynchedEntityData.defineId(ToonSlotMachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RESOLVE_TICK = SynchedEntityData.defineId(ToonSlotMachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ToonSlotMachineEntity.class, EntityDataSerializers.INT);

    public ToonSlotMachineEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ToonSlotMachineEntity(Level level, LivingEntity owner, int result, int resolveTick, int durationTicks) {
        this(ModEntityTypes.TOON_SLOT_MACHINE.get(), level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(RESULT, result);
        this.entityData.set(RESOLVE_TICK, resolveTick);
        this.entityData.set(DURATION, durationTicks);
        Vec3 pos = anchorFor(owner);
        this.setPos(pos.x, pos.y, pos.z);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
    }

    /** Floats in front of the owner's face, slightly above eye level. */
    public static Vec3 anchorFor(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        return owner.getEyePosition().add(flat.scale(1.7)).add(0.0, 0.25, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(RESULT, 0);
        this.entityData.define(RESOLVE_TICK, 32);
        this.entityData.define(DURATION, 40);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        if (getOwner() instanceof LivingEntity owner) {
            Vec3 pos = anchorFor(owner);
            // Ease toward the anchor so it trails the caster's head a little, like it's bobbing on a spring.
            Vec3 now = this.position();
            Vec3 next = now.lerp(pos, this.tickCount <= 1 ? 1.0 : 0.45);
            this.setPos(next.x, next.y, next.z);
        }

        if (!this.level.isClientSide && (getOwner() == null || this.tickCount >= getDuration())) {
            this.discard();
        }
    }

    @Nullable
    public Entity getOwner() {
        return this.level.getEntity(this.entityData.get(OWNER_ID));
    }

    public int getResult() {
        return this.entityData.get(RESULT);
    }

    public int getResolveTick() {
        return this.entityData.get(RESOLVE_TICK);
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
