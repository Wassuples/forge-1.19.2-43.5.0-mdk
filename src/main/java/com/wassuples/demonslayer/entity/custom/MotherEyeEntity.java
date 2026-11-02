package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ally.SummonAllies;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Custom entity: Mother Eye.
 */
public class MotherEyeEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private UUID casterUUID;
    private final int lifeTime = 1200; // 60 seconds max
    private final List<BlockPos> lesserEyePositions = new ArrayList<>();
    /**
     * Lesser eye positions, synced through entity data so only players tracking this Mother Eye receive them and
     * several Enmus never overwrite each other (replaces the old global Sync/ClearLesserEyes packets).
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<CompoundTag> LESSER_EYES =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MotherEyeEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.COMPOUND_TAG);
    /** Game tick at which the lesser eyes last re-opened (drives the client "eyes opening" animation). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> EYES_OPENED_TICK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MotherEyeEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CASTER_ID =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MotherEyeEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    public MotherEyeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;

        Entity caster = getCaster();
        if (caster == null || !this.getPassengers().contains(caster)) {
            this.discard();
            return;
        }

        if (this.tickCount > lifeTime) {
            this.discard();
            return;
        }

        if (this.tickCount % 40 == 0) {
            spawnLesserEyes();
        }
    }

    private void spawnLesserEyes() {
        if (this.level.isClientSide) return;

        List<BlockPos> newEyePositions = new ArrayList<>();
        double radius = 20.0;

        AABB searchArea = this.getBoundingBox().inflate(radius);
        Entity caster = getCaster();
        LivingEntity livingCaster = caster instanceof LivingEntity ? (LivingEntity) caster : null;
        List<LivingEntity> targets = this.level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                e -> e != this && e != caster && (livingCaster == null || !SummonAllies.isOwnedBy(e, livingCaster))
        );

        for (LivingEntity target : targets) {
            if (target instanceof Player p && p.getInventory().getArmor(3).is(Items.CARVED_PUMPKIN)) {
                continue;
            }

            double distanceInFront = 5.0 + this.random.nextDouble() * 5.0;
            Vec3 spawnVec = target.position().add(target.getLookAngle().scale(distanceInFront));

            BlockPos groundPos = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(spawnVec));
            if (groundPos.distSqr(this.blockPosition()) > radius * radius) continue;
            if (groundPos.distSqr(target.blockPosition()) > 15*15) continue;

            newEyePositions.add(groundPos);
        }

        this.lesserEyePositions.clear();
        this.lesserEyePositions.addAll(newEyePositions);

        CompoundTag tag = new CompoundTag();
        tag.putLongArray("Eyes", newEyePositions.stream().mapToLong(BlockPos::asLong).toArray());
        this.entityData.set(LESSER_EYES, tag);
        if (!newEyePositions.isEmpty()) {
            this.entityData.set(EYES_OPENED_TICK, this.tickCount);
        }
    }

    /** Server: the live list. Client: decoded from synced data. */
    public List<BlockPos> getLesserEyePositions() {
        if (!this.level.isClientSide) {
            return this.lesserEyePositions;
        }
        long[] packed = this.entityData.get(LESSER_EYES).getLongArray("Eyes");
        List<BlockPos> list = new ArrayList<>(packed.length);
        for (long p : packed) list.add(BlockPos.of(p));
        return list;
    }

    public int getEyesOpenedTick() {
        return this.entityData.get(EYES_OPENED_TICK);
    }

    /** Network id of the caster (riding Enmu), for client visuals. */
    public int getCasterEntityId() {
        return this.entityData.get(CASTER_ID);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LESSER_EYES, new CompoundTag());
        this.entityData.define(EYES_OPENED_TICK, -1000);
        this.entityData.define(CASTER_ID, -1);
    }

    @Nullable
    public Entity getCaster() {
        if (this.casterUUID != null && !this.level.isClientSide) {
            return ((ServerLevel)this.level).getEntity(this.casterUUID);
        }
        return null;
    }

    public void setCaster(@Nullable Entity caster) {
        this.casterUUID = caster != null ? caster.getUUID() : null;
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.hasUUID("CasterUUID")) {
            this.casterUUID = pCompound.getUUID("CasterUUID");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.casterUUID != null) {
            pCompound.putUUID("CasterUUID", this.casterUUID);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity pEntity) { }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.isBypassInvul()) return super.hurt(pSource, pAmount);
        // Can only be hurt by direct attacks
        if (pSource.getDirectEntity() instanceof LivingEntity) {
            return super.hurt(pSource, pAmount);
        }
        return false;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() { }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Nullable
    public UUID getCasterUUID() {
        return this.casterUUID;
    }

    @Override
    public double getPassengersRidingOffset() {
        // Enmu stands on top of the flesh mound drawn by MotherEyeRenderer (top ≈ 0.95 blocks).
        return 1.2D;
    }


    @Override
    public void knockback(double pStrength, double pX, double pZ) { }
}
