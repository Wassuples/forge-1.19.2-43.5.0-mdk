package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual for Killer Fish Scales (Gyokko's "perfect form"): diamond fish scales grow over the user's skin for the
 * duration. Humanoid owners (players and the mod's demons) get the scales drawn onto their actual model by
 * ScaleSkinLayer; anything else falls back to ScaleArmorRenderer's wrapped scales. Follows the owner; never saved.
 */
public class ScaleArmorEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ScaleArmorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ScaleArmorEntity.class, EntityDataSerializers.INT);

    public ScaleArmorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ScaleArmorEntity(Level level, LivingEntity owner, int duration) {
        this(ModEntityTypes.SCALE_ARMOR.get(), level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(DURATION, duration);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    /**
     * Client-side lookup from owner entity id to its active scale armour, read by ScaleSkinLayer when it draws the
     * scales onto the owner's skin. Entries are added while the armour ticks on the client and dropped once it's
     * removed. Never touched on the server.
     */
    public static final Map<Integer, ScaleArmorEntity> CLIENT_ACTIVE = new ConcurrentHashMap<>();

    @Nullable
    public static ScaleArmorEntity clientArmorFor(Entity owner) {
        ScaleArmorEntity armor = CLIENT_ACTIVE.get(owner.getId());
        if (armor == null) return null;
        if (armor.isRemoved() || armor.level != owner.level) {
            CLIENT_ACTIVE.remove(owner.getId(), armor);
            return null;
        }
        return armor;
    }

    /** 0..1 fade: the scales spread over the skin in the first 6 ticks and flake off in the last 6. */
    public float appear(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(age / 6.0f, 0.0f, 1.0f) * Mth.clamp((getDuration() - age) / 6.0f, 0.0f, 1.0f);
    }

    @Nullable
    public Entity getOwner() {
        return this.level.getEntity(this.entityData.get(OWNER_ID));
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        Entity owner = getOwner();
        if (owner != null) this.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (this.level.isClientSide && owner != null) CLIENT_ACTIVE.put(owner.getId(), this);
        if (!this.level.isClientSide && (owner == null || !owner.isAlive() || this.tickCount > getDuration())) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DURATION, 100);
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
