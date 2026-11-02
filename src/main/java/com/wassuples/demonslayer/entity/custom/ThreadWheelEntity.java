package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ally.SummonAllies;
import com.wassuples.demonslayer.util.CombatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rui's Cutting Thread Rotation: a spinning wheel of threads that rolls along the ground, slicing through
 * anything in its path.
 *
 * <p>Audit fixes (T5/T6/X5/X6): plain entity, not a 20-HP physics mob; hitbox matches the visual; follows the
 * terrain itself (rolls up and down steps of up to 1.5 blocks, shatters against taller walls) instead of
 * relying on mob step-up; each victim is cut at most every 10 ticks; damage is attributed and spares allies;
 * direction is synced so it survives reloads.
 */
public class ThreadWheelEntity extends Entity {
    public static final float DIAMETER = 2.6f;
    private static final double SPEED = 0.45;
    private static final int LIFE_TICKS = 100;
    private static final float DAMAGE = 7.0f;
    private static final int HIT_COOLDOWN = 10;
    private static final double MAX_STEP = 1.5;

    private static final EntityDataAccessor<Float> TRAVEL_YAW = SynchedEntityData.defineId(ThreadWheelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LAST_CUT_TICK = SynchedEntityData.defineId(ThreadWheelEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public ThreadWheelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID != null && this.level instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /** Horizontal travel direction (only yaw is used; the wheel always rolls flat along the ground). */
    public void setDirection(Vec3 direction, double ignoredSpeed) {
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
        this.entityData.set(TRAVEL_YAW, yaw);
        this.setYRot(yaw);
    }

    public float getTravelYaw() {
        return this.entityData.get(TRAVEL_YAW);
    }

    public float cutFlash(float age) {
        float since = age - this.entityData.get(LAST_CUT_TICK);
        return since < 0 || since > 5 ? 0.0f : 1.0f - since / 5.0f;
    }

    /** Drops the wheel onto the ground below its current position. */
    public void snapToGround() {
        Double ground = findGround(this.position(), MAX_STEP + 0.5, 4.0);
        if (ground != null) this.setPos(this.getX(), ground, this.getZ());
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        float yaw = getTravelYaw() * Mth.DEG_TO_RAD;
        Vec3 step = new Vec3(-Mth.sin(yaw) * SPEED, 0, Mth.cos(yaw) * SPEED);
        Vec3 next = this.position().add(step);
        Double ground = findGround(next, MAX_STEP, 3.0);

        if (ground == null) {
            // Ran into a wall too tall to roll over (or off a huge drop): shatter.
            if (!this.level.isClientSide) {
                shatter((ServerLevel) this.level);
            }
            return;
        }
        // Ease vertically so steps look like rolling, not teleporting.
        double y = this.getY() + Mth.clamp(ground - this.getY(), -0.6, 0.6);
        this.setPos(next.x, y, next.z);

        if (this.level.isClientSide) {
            if (this.tickCount % 2 == 0) {
                BlockState below = this.level.getBlockState(new BlockPos(this.getX(), this.getY() - 0.2, this.getZ()));
                if (!below.isAir()) {
                    this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                            this.getX(), this.getY() + 0.05, this.getZ(), -step.x * 2, 0.15, -step.z * 2);
                }
            }
            return;
        }

        cutThrough((ServerLevel) this.level);
        if (this.tickCount % 4 == 0) {
            this.level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.9f);
        }
        if (this.tickCount >= LIFE_TICKS) {
            shatter((ServerLevel) this.level);
        }
    }

    private void cutThrough(ServerLevel level) {
        LivingEntity owner = getOwner();
        boolean cut = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), e -> e.isAlive()
                && !e.isSpectator() && e != owner && !SummonAllies.isFriendlyForTargeting(this, e)
                && !(owner != null && SummonAllies.isFriendlyForTargeting(owner, e)))) {
            Integer last = lastHit.get(e.getUUID());
            if (last != null && this.tickCount - last < HIT_COOLDOWN) continue;
            lastHit.put(e.getUUID(), this.tickCount);
            boolean hurt = owner != null
                    ? e.hurt(CombatHelper.getDamageSource(owner), CombatHelper.calculateFinalDamage(owner, DAMAGE))
                    : e.hurt(DamageSource.MAGIC, DAMAGE);
            if (hurt) {
                cut = true;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
            }
        }
        if (cut) this.entityData.set(LAST_CUT_TICK, this.tickCount);
    }

    private void shatter(ServerLevel level) {
        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + DIAMETER * 0.5, this.getZ(), 30, 0.6, 0.8, 0.6, 0.3);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + DIAMETER * 0.5, this.getZ(), 3, 0.4, 0.5, 0.4, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 1.2f, 0.6f);
        this.discard();
    }

    /**
     * Ground height near {@code at}: the highest solid surface between {@code up} blocks above and {@code down}
     * blocks below, or null if the column is blocked higher than {@code up} (a wall).
     */
    @Nullable
    private Double findGround(Vec3 at, double up, double down) {
        Vec3 top = new Vec3(at.x, this.getY() + up, at.z);
        // A wall: the space the wheel's lower half would occupy above the max step is solid.
        BlockPos headPos = new BlockPos(at.x, this.getY() + up + 0.5, at.z);
        if (!this.level.getBlockState(headPos).getCollisionShape(this.level, headPos).isEmpty()) {
            return null;
        }
        BlockHitResult hit = this.level.clip(new ClipContext(top, new Vec3(at.x, this.getY() - down, at.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : null;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(0.5);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TRAVEL_YAW, 0.0f);
        this.entityData.define(LAST_CUT_TICK, -100);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) this.ownerUUID = tag.getUUID("OwnerUUID");
        this.entityData.set(TRAVEL_YAW, tag.getFloat("TravelYaw"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) tag.putUUID("OwnerUUID", this.ownerUUID);
        tag.putFloat("TravelYaw", getTravelYaw());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
