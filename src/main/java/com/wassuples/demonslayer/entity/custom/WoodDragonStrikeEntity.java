package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One of Zohakuten's wooden dragons for Countless Striking Trees: its neck bursts out of the ground a few blocks
 * from the strike point and the head slams down onto it in two ticks (the form deals its damage on spawn), holds
 * there grinding into the earth, rears up and roars, then sinks back into the ground. Positioned at the strike
 * point; the root is synced as an offset. Purely visual; never saved.
 */
public class WoodDragonStrikeEntity extends TimedVisualEntity {
    public static final int SLAM_TICKS = 2;
    public static final int HOLD_END = 10;
    public static final int REAR_END = 24;
    public static final int LIFE = 34;

    private static final EntityDataAccessor<Float> ROOT_X = SynchedEntityData.defineId(WoodDragonStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROOT_Y = SynchedEntityData.defineId(WoodDragonStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROOT_Z = SynchedEntityData.defineId(WoodDragonStrikeEntity.class, EntityDataSerializers.FLOAT);

    public WoodDragonStrikeEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public WoodDragonStrikeEntity(Level level, Vec3 strike, Vec3 root) {
        this(ModEntityTypes.WOOD_DRAGON_STRIKE.get(), level);
        this.setPos(strike.x, strike.y, strike.z);
        Vec3 rel = root.subtract(strike);
        this.entityData.set(ROOT_X, (float) rel.x);
        this.entityData.set(ROOT_Y, (float) rel.y);
        this.entityData.set(ROOT_Z, (float) rel.z);
    }

    /** Where the neck comes out of the ground, relative to the strike point. */
    public Vec3 getRootOffset() {
        return new Vec3(this.entityData.get(ROOT_X), this.entityData.get(ROOT_Y), this.entityData.get(ROOT_Z));
    }

    @Override
    protected int lifeTicks() {
        return LIFE;
    }

    @Override
    protected double cullRadius() {
        return 10.0;
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(ROOT_X, 3.0f);
        this.entityData.define(ROOT_Y, 0.0f);
        this.entityData.define(ROOT_Z, 0.0f);
    }
}
