package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual for Ten Thousand Gliding Slime-Fish: a shoal of Gyokko's goldfish pours out along each of the form's
 * streams, weaving through the air to its impact point, then bursts into slime. Streams are synced as world-space
 * start/end pairs. Purely visual; never saved.
 */
public class FishSchoolEntity extends Entity {
    public static final int TRAVEL_TICKS = 10;
    public static final int LIFE_TICKS = TRAVEL_TICKS + 10;

    private static final EntityDataAccessor<CompoundTag> STREAMS = SynchedEntityData.defineId(FishSchoolEntity.class, EntityDataSerializers.COMPOUND_TAG);

    public FishSchoolEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public FishSchoolEntity(Level level, Vec3 at, List<Vec3[]> streams) {
        this(ModEntityTypes.FISH_SCHOOL.get(), level);
        this.setPos(at.x, at.y, at.z);
        ListTag list = new ListTag();
        for (Vec3[] s : streams) {
            CompoundTag t = new CompoundTag();
            t.putDouble("sx", s[0].x); t.putDouble("sy", s[0].y); t.putDouble("sz", s[0].z);
            t.putDouble("ex", s[1].x); t.putDouble("ey", s[1].y); t.putDouble("ez", s[1].z);
            list.add(t);
        }
        CompoundTag root = new CompoundTag();
        root.put("s", list);
        this.entityData.set(STREAMS, root);
    }

    /** World-space [start, end] pairs. */
    public List<Vec3[]> getStreams() {
        List<Vec3[]> out = new ArrayList<>();
        for (Tag tag : this.entityData.get(STREAMS).getList("s", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) tag;
            out.add(new Vec3[]{new Vec3(t.getDouble("sx"), t.getDouble("sy"), t.getDouble("sz")),
                    new Vec3(t.getDouble("ex"), t.getDouble("ey"), t.getDouble("ez"))});
        }
        return out;
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        if (!this.level.isClientSide && this.tickCount > LIFE_TICKS) this.discard();
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(30.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STREAMS, new CompoundTag());
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
