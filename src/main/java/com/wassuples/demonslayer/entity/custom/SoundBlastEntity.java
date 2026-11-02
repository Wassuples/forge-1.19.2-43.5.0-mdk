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
 * Compressed Sound Waves (Kyōatsu Meiha): Zohakuten opens his mouth and fires a blast of sound compressed into a
 * thin beam along the form's hitscan line: rings of pressure racing down a white-hot core, a shock cone at the
 * mouth and a burst where it lands. Positioned at the line's start, end synced as an offset.
 * Purely visual; never saved.
 */
public class SoundBlastEntity extends TimedVisualEntity {
    public static final int CHARGE = 3;
    public static final int LIFE = 18;

    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(SoundBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(SoundBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(SoundBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HIT = SynchedEntityData.defineId(SoundBlastEntity.class, EntityDataSerializers.BOOLEAN);

    public SoundBlastEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public SoundBlastEntity(Level level, LivingEntity caster, Vec3 start, Vec3 end, boolean hit) {
        this(ModEntityTypes.SOUND_BLAST.get(), level);
        setOwnerId(caster.getId());
        this.setPos(start.x, start.y, start.z);
        Vec3 rel = end.subtract(start);
        this.entityData.set(END_X, (float) rel.x);
        this.entityData.set(END_Y, (float) rel.y);
        this.entityData.set(END_Z, (float) rel.z);
        this.entityData.set(HIT, hit);
    }

    public Vec3 getEndOffset() {
        return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
    }

    public boolean didHit() {
        return this.entityData.get(HIT);
    }

    @Override
    protected int lifeTicks() {
        return LIFE;
    }

    @Override
    protected double cullRadius() {
        return getEndOffset().length() + 3.0;
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(END_X, 0.0f);
        this.entityData.define(END_Y, 0.0f);
        this.entityData.define(END_Z, 10.0f);
        this.entityData.define(HIT, false);
    }
}
