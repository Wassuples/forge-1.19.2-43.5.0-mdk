package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.effects.ModEffects;
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
 * Visual-only companion for anything under Forced Sleep (from any source: Whispers, lesser eyes, Hantengu's
 * sound waves): drifting "Z z z", a slowly turning dream halo and soft pink-violet motes, visible to everyone.
 * Created and removed automatically by {@code ForcedSleepEffect}; never saved.
 */
public class DreamAuraEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(DreamAuraEntity.class, EntityDataSerializers.INT);

    public DreamAuraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public DreamAuraEntity(Level level, LivingEntity target) {
        this(ModEntityTypes.DREAM_AURA.get(), level);
        this.entityData.set(TARGET_ID, target.getId());
        this.setPos(target.getX(), target.getY(), target.getZ());
    }

    @Nullable
    public Entity getTarget() {
        return this.level.getEntity(this.entityData.get(TARGET_ID));
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        Entity target = getTarget();
        if (target != null) {
            this.setPos(target.getX(), target.getY(), target.getZ());
        }
        if (!this.level.isClientSide && (!(target instanceof LivingEntity living) || !living.isAlive()
                || !living.hasEffect(ModEffects.FORCED_SLEEP.get()))) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
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
