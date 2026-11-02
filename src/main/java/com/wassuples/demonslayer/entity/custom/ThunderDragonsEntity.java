package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Crazed Cry of Thunderdeath (Kyōmei Raisatsu): Zohakuten strikes a drum and two colossal wooden dragons circle
 * the arena high overhead (on the same horizontal path the form used for its old particle markers). Dragon 0
 * carries Sekido's electricity, dragon 1 Urogi's sound waves: at {@link #WIND_UP} the sound dragon cries out and
 * a wall of sound rolls across the arena, and from {@link #LIGHTNING_START} every bolt the form calls down is
 * spat from the thunder dragon's jaws as a crackling arc. Positioned at the arena centre; the form discards it when it ends. Never saved.
 */
public class ThunderDragonsEntity extends TimedVisualEntity {
    public static final int WIND_UP = 60;
    public static final int LIGHTNING_START = 80;
    public static final float HEIGHT = 18.0f;
    public static final float ANGULAR_SPEED = 0.05f;

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOLT_TICK = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BOLT_X = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BOLT_Y = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BOLT_Z = SynchedEntityData.defineId(ThunderDragonsEntity.class, EntityDataSerializers.FLOAT);

    public ThunderDragonsEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public ThunderDragonsEntity(Level level, LivingEntity caster, Vec3 centre, float radius, int duration) {
        this(ModEntityTypes.THUNDER_DRAGONS.get(), level);
        setOwnerId(caster.getId());
        this.setPos(centre.x, centre.y, centre.z);
        this.entityData.set(RADIUS, radius);
        this.entityData.set(DURATION, duration);
    }

    /** Server: a bolt was called down at {@code at}; the nearer dragon spits it. */
    public void bolt(Vec3 at) {
        Vec3 rel = at.subtract(this.position());
        this.entityData.set(BOLT_X, (float) rel.x);
        this.entityData.set(BOLT_Y, (float) rel.y);
        this.entityData.set(BOLT_Z, (float) rel.z);
        this.entityData.set(BOLT_TICK, this.tickCount);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getBoltTick() {
        return this.entityData.get(BOLT_TICK);
    }

    public Vec3 getBoltOffset() {
        return new Vec3(this.entityData.get(BOLT_X), this.entityData.get(BOLT_Y), this.entityData.get(BOLT_Z));
    }

    /** Head position of dragon {@code index} (0 or 1) relative to the centre, matching the form's circle. */
    public Vec3 headOffset(int index, float age) {
        double angle = age * ANGULAR_SPEED + index * Math.PI;
        float r = getRadius();
        return new Vec3(Math.cos(angle) * r, HEIGHT + Math.sin(age * 0.11 + index * 2.0) * 3.0, Math.sin(angle) * r);
    }

    @Override
    protected int lifeTicks() {
        return this.entityData.get(DURATION) + 5;
    }

    @Override
    protected double cullRadius() {
        return getRadius() + 30.0;
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(RADIUS, 25.0f);
        this.entityData.define(DURATION, 240);
        this.entityData.define(BOLT_TICK, -100);
        this.entityData.define(BOLT_X, 0.0f);
        this.entityData.define(BOLT_Y, 0.0f);
        this.entityData.define(BOLT_Z, 0.0f);
    }
}
