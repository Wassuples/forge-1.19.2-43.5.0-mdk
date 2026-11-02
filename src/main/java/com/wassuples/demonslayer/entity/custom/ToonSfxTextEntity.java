package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * A purely visual, non-saved entity that carries one cartoon SFX word ("BOING!", "BONK!"...).
 * The server only decides what it says and how long it lives; all styling and animation is
 * done client-side by {@code ToonSfxTextRenderer}, so every viewer sees the same lettering.
 */
public class ToonSfxTextEntity extends Entity {
    private static final EntityDataAccessor<String> TEXT = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FILL = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OUTLINE = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BURST = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BURST_OUTLINE = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ToonSfxTextEntity.class, EntityDataSerializers.FLOAT);

    public ToonSfxTextEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public ToonSfxTextEntity(Level level) {
        this(ModEntityTypes.TOON_SFX_TEXT.get(), level);
    }

    public void setup(String text, int fill, int outline, int burst, int burstOutline, int style, int seed,
                      int durationTicks, float scale) {
        this.entityData.set(TEXT, text);
        this.entityData.set(FILL, fill);
        this.entityData.set(OUTLINE, outline);
        this.entityData.set(BURST, burst);
        this.entityData.set(BURST_OUTLINE, burstOutline);
        this.entityData.set(STYLE, style);
        this.entityData.set(SEED, seed);
        this.entityData.set(DURATION, Math.max(6, durationTicks));
        this.entityData.set(SCALE, scale);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TEXT, "");
        this.entityData.define(FILL, 0xFFFFFFFF);
        this.entityData.define(OUTLINE, 0xFF000000);
        this.entityData.define(BURST, 0xFFFFE23B);
        this.entityData.define(BURST_OUTLINE, 0xFF000000);
        this.entityData.define(STYLE, 0);
        this.entityData.define(SEED, 0);
        this.entityData.define(DURATION, 20);
        this.entityData.define(SCALE, 1.0f);
    }

    @Override
    public void tick() {
        // Deliberately skip super.tick(): no physics, fire, fluids or portals for a floating word.
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;
        // Gentle upward drift, done identically on both sides so it never rubber-bands.
        this.setPos(this.getX(), this.getY() + 0.012, this.getZ());
        if (!this.level.isClientSide && this.tickCount >= getDuration()) {
            this.discard();
        }
    }

    public String getText() { return this.entityData.get(TEXT); }
    public int getFill() { return this.entityData.get(FILL); }
    public int getOutline() { return this.entityData.get(OUTLINE); }
    public int getBurst() { return this.entityData.get(BURST); }
    public int getBurstOutline() { return this.entityData.get(BURST_OUTLINE); }
    public int getStyle() { return this.entityData.get(STYLE); }
    public int getSeed() { return this.entityData.get(SEED); }
    public int getDuration() { return this.entityData.get(DURATION); }
    public float getTextScale() { return this.entityData.get(SCALE); }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 64.0 * 64.0;
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
