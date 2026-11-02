package com.wassuples.demonslayer.entity.custom;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.ally.SummonAllies;
import com.wassuples.demonslayer.util.CombatHelper;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared server behaviour for Rui's thread traps (Cutting Thread Cage, Murderous Eye Basket).
 *
 * <p>Each trap forms (visual only), then <b>closes</b> at {@link #closeTick()}: whatever is inside at that
 * moment is trapped and can't get out (touching the threads cuts and shoves them back in). From
 * {@link #contractStartTick()} the trap tightens to {@link #finalRadiusFraction()} of its size, cutting
 * harder, and ends with a snap at {@link #totalTicks()}.
 *
 * <p>Cylindrical traps (the cage) measure horizontal distance from the centre; spherical traps (the basket)
 * measure 3D distance from a centre {@link #getCentreHeight()} above the origin.
 *
 * <p>Plain entity (not a mob): can't be targeted, killed or pushed, and doesn't count toward mob caps.
 * Damage is attributed to the caster and never hits the caster, their summons or allies.
 */
public abstract class ThreadTrapEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ThreadTrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_CUT_TICK = SynchedEntityData.defineId(ThreadTrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CENTRE_HEIGHT = SynchedEntityData.defineId(ThreadTrapEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUUID;
    private final Set<UUID> trapped = new HashSet<>();
    private boolean trapClosed;

    protected ThreadTrapEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // ------------------------------------------------------------------ tuning (per subclass)

    public abstract float baseRadius();

    public abstract int closeTick();

    public abstract int contractStartTick();

    public abstract int totalTicks();

    public abstract float finalRadiusFraction();

    public abstract boolean spherical();

    protected abstract float contactDamage();

    protected abstract float contractDamageStart();

    protected abstract float contractDamageEnd();

    protected abstract float snapDamage();

    public abstract boolean bloodThreads();

    /** Exponent of the contraction easing: 2 = slow start, higher = stays wide then snaps shut. */
    protected float contractEasing() {
        return 2.0f;
    }

    // ------------------------------------------------------------------ shared timeline (both sides)

    public float contractProgress(float age) {
        return Mth.clamp((age - contractStartTick()) / (float) (totalTicks() - contractStartTick()), 0.0f, 1.0f);
    }

    /** Trap radius used for trapping/cutting; renderers use it too so visuals and hitbox always agree. */
    public float currentRadius(float age) {
        float eased = (float) Math.pow(contractProgress(age), contractEasing());
        return baseRadius() * Mth.lerp(eased, 1.0f, finalRadiusFraction());
    }

    public float cutFlash(float age) {
        float since = age - this.entityData.get(LAST_CUT_TICK);
        return since < 0 || since > 6 ? 0.0f : 1.0f - since / 6.0f;
    }

    public float getCentreHeight() {
        return this.entityData.get(CENTRE_HEIGHT);
    }

    public void setCentreHeight(float height) {
        this.entityData.set(CENTRE_HEIGHT, height);
    }

    // ------------------------------------------------------------------ setup

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.entityData.set(OWNER_ID, owner != null ? owner.getId() : -1);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /** Server side only (null on clients). Also read by SummonAllies via reflection. */
    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID != null && this.level instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public int getOwnerEntityId() {
        return this.entityData.get(OWNER_ID);
    }

    /** Centres the trap on the ground under {@code pos} (fixes traps spawning inside walls). */
    public void placeOnGround(Vec3 pos) {
        BlockHitResult hit = this.level.clip(new ClipContext(pos.add(0, 1.5, 0), pos.add(0, -6.0, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 ground = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : pos;
        this.setPos(ground.x, ground.y, ground.z);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        if (this.level.isClientSide) return;
        ServerLevel level = (ServerLevel) this.level;

        if (!trapClosed && this.tickCount >= closeTick()) {
            closeTrap(level);
        }
        if (trapClosed) {
            holdTrapped(level, this.tickCount);
        }
        if (this.tickCount >= totalTicks()) {
            snap(level);
            this.discard();
        }
    }

    private void closeTrap(ServerLevel level) {
        trapClosed = true;
        for (LivingEntity e : candidates(baseRadius() + 0.5f)) {
            if (trapDistance(e) < baseRadius() - 0.2) {
                trapped.add(e.getUUID());
            }
        }
        level.playSound(null, this.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    private void holdTrapped(ServerLevel level, float age) {
        float radius = currentRadius(age);
        float contract = contractProgress(age);
        boolean damageTick = this.tickCount % 5 == 0;
        boolean cut = false;
        Vec3 centre = centre();

        for (LivingEntity e : candidates(baseRadius() + 1.5f)) {
            if (!trapped.contains(e.getUUID())) continue;
            double dist = trapDistance(e);
            if (dist > radius + 1.5) {
                trapped.remove(e.getUUID()); // escaped by teleport / pearl: let them go
                continue;
            }
            double edge = dist + e.getBbWidth() * 0.5;
            if (edge > radius - 0.15) {
                Vec3 toCentre = centre.subtract(e.position().add(0, spherical() ? e.getBbHeight() * 0.5 : 0, 0));
                if (!spherical()) toCentre = new Vec3(toCentre.x, 0, toCentre.z);
                toCentre = toCentre.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toCentre.normalize();
                e.setDeltaMovement(toCentre.scale(0.35 + contract * 0.3).add(0, spherical() ? 0.0 : 0.05, 0));
                e.hurtMarked = true;
                if (damageTick) {
                    float damage = contract > 0 ? Mth.lerp(contract, contractDamageStart(), contractDamageEnd()) : contactDamage();
                    cut |= hurtAttributed(e, damage);
                }
            }
        }

        if (cut) {
            this.entityData.set(LAST_CUT_TICK, this.tickCount);
            level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.6f + contract * 0.4f);
        }
        if (contract > 0 && this.tickCount % 4 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.7f, 0.8f + contract);
        }
    }

    private void snap(ServerLevel level) {
        float radius = currentRadius(this.tickCount) + 0.6f;
        for (LivingEntity e : candidates(radius)) {
            if (trapDistance(e) <= radius) {
                hurtAttributed(e, snapDamage());
            }
        }
        Vec3 c = centre();
        DustParticleOptions dust = new DustParticleOptions(
                bloodThreads() ? new Vector3f(0.85f, 0.1f, 0.15f) : new Vector3f(0.95f, 0.95f, 1.0f), 1.2f);
        level.sendParticles(dust, c.x, c.y + (spherical() ? 0 : 1.0), c.z, 40, 0.8, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y + (spherical() ? 0 : 1.0), c.z, 6, 0.6, 0.5, 0.6, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.7f);
        level.playSound(null, this.blockPosition(), SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 1.4f, 0.5f);
    }

    /** Trap centre in world space (ground centre for cylinders, sphere centre for spheres). */
    public Vec3 centre() {
        return this.position().add(0, spherical() ? getCentreHeight() : 0, 0);
    }

    public double trapDistance(Entity e) {
        Vec3 c = centre();
        if (spherical()) {
            return e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(c);
        }
        double dx = e.getX() - c.x;
        double dz = e.getZ() - c.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private List<LivingEntity> candidates(float radius) {
        Vec3 c = centre();
        AABB box = spherical()
                ? new AABB(c, c).inflate(radius + 1.0)
                : new AABB(c.x - radius, c.y - 1.0, c.z - radius, c.x + radius, c.y + 6.0, c.z + radius);
        LivingEntity owner = getOwner();
        return this.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator()
                && e != owner && !SummonAllies.isFriendlyForTargeting(this, e)
                && !(owner != null && SummonAllies.isFriendlyForTargeting(owner, e)));
    }

    private boolean hurtAttributed(LivingEntity target, float baseDamage) {
        LivingEntity owner = getOwner();
        if (owner != null) {
            return target.hurt(CombatHelper.getDamageSource(owner), CombatHelper.calculateFinalDamage(owner, baseDamage));
        }
        return target.hurt(DamageSource.MAGIC, baseDamage);
    }

    // ------------------------------------------------------------------ boilerplate

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(LAST_CUT_TICK, -100);
        this.entityData.define(CENTRE_HEIGHT, 1.0f);
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
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 96.0 * 96.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float r = baseRadius() + 1.0f;
        return new AABB(this.getX() - r, this.getY() - 0.5, this.getZ() - r, this.getX() + r, this.getY() + 7.0, this.getZ() + r);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) this.ownerUUID = tag.getUUID("OwnerUUID");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) tag.putUUID("OwnerUUID", this.ownerUUID);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
