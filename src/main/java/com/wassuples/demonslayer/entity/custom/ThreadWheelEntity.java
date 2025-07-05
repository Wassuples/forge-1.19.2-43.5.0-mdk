package com.wassuples.demonslayer.entity.custom;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ThreadWheelEntity extends Monster implements IAnimatable {
    private static final EntityDataAccessor<Float> DATA_INITIAL_YAW = SynchedEntityData.defineId(ThreadWheelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ROTATION_STORED = SynchedEntityData.defineId(ThreadWheelEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimationFactory factory = new AnimationFactory(this);

    private int lifeTicks = 120;
    private Vec3 travelVector = Vec3.ZERO;

    public ThreadWheelEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.maxUpStep = 1.0F;

        this.moveControl = new MoveControl(this) { @Override public void tick() {} };
        this.lookControl = new LookControl(this) { @Override public void tick() {} };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D);
    }

    @Override
    public void tick() {
        super.tick();
        handleRotationLock();

        if (this.level.isClientSide) {
            return;
        }

        this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3D), e -> e != this && !(e instanceof DemonEntity))
                .forEach(entity -> entity.hurt(DamageSource.MAGIC, 9.0F));

        if (this.tickCount % 4 == 0) {
            this.level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.6F, 1.8F);
        }

        if (--this.lifeTicks <= 0) {
            this.discard();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.travelVector.equals(Vec3.ZERO)) {
            this.setDeltaMovement(this.travelVector.x, this.getDeltaMovement().y, this.travelVector.z);
        }
    }

    private void handleRotationLock() {
        if (!this.level.isClientSide && !this.entityData.get(DATA_ROTATION_STORED)) {
            this.entityData.set(DATA_INITIAL_YAW, this.getYRot());
            this.entityData.set(DATA_ROTATION_STORED, true);
        }

        if (this.entityData.get(DATA_ROTATION_STORED)) {
            float initialYaw = this.entityData.get(DATA_INITIAL_YAW);
            this.setYRot(initialYaw);
            this.yBodyRot = initialYaw;
            this.yHeadRot = initialYaw;
            this.setXRot(0);
        }
    }

    public void setDirection(Vec3 direction, double speed) {
        this.travelVector = new Vec3(direction.x, 0, direction.z).normalize().scale(speed);
    }

    @Override
    protected void registerGoals() { }

    @Override
    public void die(DamageSource pDamageSource) {
        if (!this.level.isClientSide) {
            this.discard();
        }
        super.die(pDamageSource);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.fixed(4.0F, 4.0F);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_INITIAL_YAW, 0.0F);
        this.entityData.define(DATA_ROTATION_STORED, false);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("spin", true));
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
}