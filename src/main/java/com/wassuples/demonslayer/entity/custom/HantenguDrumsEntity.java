package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Zohakuten's ring of taiko drums, worn on the back while Hantengu commands his wooden dragons (Countless Striking
 * Trees, Crazed Cry of Thunderdeath). Every {@link #beat()} makes the next drum boom: its skin shudders and a ring
 * of sound rolls off it. Follows its owner; purely visual; the form discards it when it ends.
 */
public class HantenguDrumsEntity extends TimedVisualEntity {
    public static final int DRUMS = 5;

    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(HantenguDrumsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_BEAT = SynchedEntityData.defineId(HantenguDrumsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAT_DRUM = SynchedEntityData.defineId(HantenguDrumsEntity.class, EntityDataSerializers.INT);

    public HantenguDrumsEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public HantenguDrumsEntity(Level level, LivingEntity owner, int duration) {
        this(ModEntityTypes.HANTENGU_DRUMS.get(), level);
        setOwnerId(owner.getId());
        this.entityData.set(DURATION, duration);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    /** Server: boom the next drum in the ring. */
    public void beat() {
        this.entityData.set(BEAT_DRUM, (this.entityData.get(BEAT_DRUM) + 1) % DRUMS);
        this.entityData.set(LAST_BEAT, this.tickCount);
    }

    public int getLastBeat() {
        return this.entityData.get(LAST_BEAT);
    }

    public int getBeatDrum() {
        return this.entityData.get(BEAT_DRUM);
    }

    public int getDuration() {
        return this.entityData.get(DURATION);
    }

    @Override
    protected int lifeTicks() {
        return getDuration() + 10;
    }

    @Override
    protected double cullRadius() {
        return 4.0;
    }

    @Override
    protected boolean followsOwner() {
        return true;
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(DURATION, 200);
        this.entityData.define(LAST_BEAT, -100);
        this.entityData.define(BEAT_DRUM, 0);
    }
}
