package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual for Doma's Cryokinesis. One entity per cast, drawn by DomaArtRenderer according to its {@link Mode}:
 * crystal lotuses, ice tendrils, vines and icicles, and billowing freezing mist.
 * Some modes ride along with Doma; when a form reports an impact ({@link #impact}) the entity can freeze in place
 * so the burst stays where it happened. Purely visual; never saved.
 */
public class DomaArtEntity extends TimedVisualEntity {
    public enum Mode {
        /** Frozen Lotus: a fan sweep of freezing mist across the cone and crystal lotuses blooming in it. */
        FROZEN_LOTUS,
        /** Barren Hanging Garden: weeping tendrils of ice lashing around Doma with every wave. */
        HANGING_GARDEN,
        /** Freezing Clouds: a wall of freezing mist rolling forward. */
        FREEZING_CLOUDS,
        /** Lotus Vines: ice vines snaking to the target, then a garden of ice lotuses. */
        LOTUS_VINES,
        /** Wintry Icicles: icicles form in the air over the target and plunge into it. */
        WINTRY_ICICLES,
        /** Crystalline Divine Child charge: a crystal lotus per clone orbiting Doma. */
        CRYSTAL_CHARGE
    }

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RELEASE_TICK = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> IMPACT_TICK = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.INT);
    /** Impact point relative to the entity, plus the extra points (Afterglow targets). */
    private static final EntityDataAccessor<CompoundTag> POINTS = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** Random seed for the layout of lotuses / icicles. */
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DomaArtEntity.class, EntityDataSerializers.INT);

    public DomaArtEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Rides along with {@code owner}. */
    public DomaArtEntity(Level level, LivingEntity owner, Mode mode, int duration) {
        this(ModEntityTypes.DOMA_ART.get(), level);
        setOwnerId(owner.getId());
        this.entityData.set(MODE, mode.ordinal());
        this.entityData.set(DURATION, duration);
        Vec3 look = owner.getLookAngle();
        setDirection(look);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    /** Stays at {@code at} (Lotus Vines' start, Wintry Icicles' target). */
    public DomaArtEntity(Level level, LivingEntity owner, Mode mode, int duration, Vec3 at, Vec3 direction) {
        this(level, owner, mode, duration);
        this.entityData.set(FROZEN, true);
        setDirection(direction);
        this.setPos(at.x, at.y, at.z);
    }

    private void setDirection(Vec3 d) {
        this.entityData.set(DIR_X, (float) d.x);
        this.entityData.set(DIR_Y, (float) d.y);
        this.entityData.set(DIR_Z, (float) d.z);
    }

    // ------------------------------------------------------------------------------------------- server events

    /** Server: the charge was released. */
    public void release(Vec3 direction) {
        setDirection(direction);
        this.entityData.set(RELEASE_TICK, this.tickCount);
    }

    /** Server: set the points of interest without freezing or changing the lifetime (lotus spots etc.). */
    public void setPoints(Vec3 main, java.util.List<Vec3> extra) {
        CompoundTag root = new CompoundTag();
        Vec3 rel = main.subtract(this.position());
        root.putFloat("x", (float) rel.x);
        root.putFloat("y", (float) rel.y);
        root.putFloat("z", (float) rel.z);
        ListTag list = new ListTag();
        for (Vec3 p : extra) {
            Vec3 r = p.subtract(this.position());
            CompoundTag t = new CompoundTag();
            t.putFloat("x", (float) r.x);
            t.putFloat("y", (float) r.y);
            t.putFloat("z", (float) r.z);
            list.add(t);
        }
        root.put("extra", list);
        this.entityData.set(POINTS, root);
    }

    /**
     * Server: something hit at {@code at}. The entity freezes where it is (so the burst stays put) and lives
     * {@code linger} more ticks.
     */
    public void impact(Vec3 at, int linger) {
        impact(at, List.of(), linger);
    }

    /** Server: impact with extra points (lotus positions, garden spots). */
    public void impact(Vec3 at, List<Vec3> extra, int linger) {
        this.entityData.set(FROZEN, true);
        CompoundTag root = new CompoundTag();
        Vec3 rel = at.subtract(this.position());
        root.putFloat("x", (float) rel.x);
        root.putFloat("y", (float) rel.y);
        root.putFloat("z", (float) rel.z);
        ListTag list = new ListTag();
        for (Vec3 p : extra) {
            Vec3 r = p.subtract(this.position());
            CompoundTag t = new CompoundTag();
            t.putFloat("x", (float) r.x);
            t.putFloat("y", (float) r.y);
            t.putFloat("z", (float) r.z);
            list.add(t);
        }
        root.put("extra", list);
        this.entityData.set(POINTS, root);
        this.entityData.set(IMPACT_TICK, this.tickCount);
        this.entityData.set(DURATION, Math.max(getDuration(), this.tickCount + linger));
    }

    /** Server: the form ended; let the visual play out {@code linger} more ticks, then go. */
    public void finish(int linger) {
        this.entityData.set(DURATION, this.tickCount + linger);
    }

    // ------------------------------------------------------------------------------------------- accessors

    public void setSeed(int seed) {
        this.entityData.set(SEED, seed);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public Mode getMode() {
        int i = this.entityData.get(MODE);
        Mode[] all = Mode.values();
        return i >= 0 && i < all.length ? all[i] : Mode.FROZEN_LOTUS;
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    public boolean isFrozen() {
        return this.entityData.get(FROZEN);
    }

    public Vec3 getAimDirection() {
        return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
    }

    public int getReleaseTick() {
        return this.entityData.get(RELEASE_TICK);
    }

    public int getImpactTick() {
        return this.entityData.get(IMPACT_TICK);
    }

    public Vec3 getImpactOffset() {
        CompoundTag t = this.entityData.get(POINTS);
        return new Vec3(t.getFloat("x"), t.getFloat("y"), t.getFloat("z"));
    }

    public List<Vec3> getExtraPoints() {
        List<Vec3> out = new ArrayList<>();
        for (Tag tag : this.entityData.get(POINTS).getList("extra", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) tag;
            out.add(new Vec3(t.getFloat("x"), t.getFloat("y"), t.getFloat("z")));
        }
        return out;
    }

    @Override
    protected int lifeTicks() {
        return getDuration() + 2;
    }

    @Override
    protected double cullRadius() {
        return 28.0;
    }

    @Override
    protected boolean followsOwner() {
        return !isFrozen();
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(MODE, 0);
        this.entityData.define(DURATION, 20);
        this.entityData.define(FROZEN, false);
        this.entityData.define(DIR_X, 0.0f);
        this.entityData.define(DIR_Y, 0.0f);
        this.entityData.define(DIR_Z, 1.0f);
        this.entityData.define(RELEASE_TICK, -1);
        this.entityData.define(IMPACT_TICK, -1);
        this.entityData.define(POINTS, new CompoundTag());
        this.entityData.define(SEED, 0);
    }
}
