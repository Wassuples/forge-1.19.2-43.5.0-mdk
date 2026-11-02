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
 * Visual for Daki's Eight-Layered Obi Slash: eight patterned obi sashes burst one after another from the bow at
 * her back and whip forward onto the line she's aiming at; each reaches full length exactly on the tick the form
 * deals that slash's damage, then pulls back into a fan around her and keeps writhing there until all eight are
 * out, and they retract together. Follows its owner (and her aim); purely visual; never saved.
 */
public class ObiSashEntity extends Entity {
    public static final int SASHES = 8;
    public static final int INTERVAL = 4;
    public static final int EXTEND = 3;
    public static final int HOLD = 3;
    public static final int RETRACT = 5;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ObiSashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(ObiSashEntity.class, EntityDataSerializers.FLOAT);

    public ObiSashEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ObiSashEntity(Level level, LivingEntity owner, float range) {
        this(ModEntityTypes.OBI_SASH.get(), level);
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(RANGE, range);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    @Nullable
    public Entity getOwner() {
        return this.level.getEntity(this.entityData.get(OWNER_ID));
    }

    public float getRange() {
        return this.entityData.get(RANGE);
    }

    /** Tick at which sash {@code i} reaches full length (= the form's damage tick for that slash). */
    public static int strikeTick(int i) {
        return (i + 1) * INTERVAL;
    }

    public static int totalTicks() {
        return strikeTick(SASHES - 1) + HOLD + RETRACT + 1;
    }

    /** Tick at which every sash starts to retract together, after the last slash has landed and held. */
    public static int retractTick() {
        return strikeTick(SASHES - 1) + HOLD;
    }

    /**
     * 0..1 how far sash {@code i} is extended at {@code age}. A sash whips out over {@link #EXTEND} ticks so it is
     * fully out on its strike tick, then stays out (hovering, see {@link #settle}) until the last slash has landed,
     * so all eight are on screen together before they retract into the bow.
     */
    public static float extension(int i, float age) {
        float strike = strikeTick(i);
        float start = strike - EXTEND;
        if (age < start) return 0.0f;
        if (age < strike) return (age - start) / EXTEND;
        if (age < retractTick()) return 1.0f;
        return Math.max(0.0f, 1.0f - (age - retractTick()) / RETRACT);
    }

    /**
     * 0 while sash {@code i} is striking (its tip is on the aim line), easing to 1 over the ticks after its hold
     * as it pulls back and fans out around Daki, ready for the next layer.
     */
    public static float settle(int i, float age) {
        float t = (age - strikeTick(i) - HOLD) / 4.0f;
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        return t * t * (3.0f - 2.0f * t);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        Entity owner = getOwner();
        if (owner != null) this.setPos(owner.getX(), owner.getY(), owner.getZ());
        if (!this.level.isClientSide && (owner == null || !owner.isAlive() || this.tickCount > totalTicks())) {
            this.discard();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(getRange() + 2.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(RANGE, 12.0f);
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
