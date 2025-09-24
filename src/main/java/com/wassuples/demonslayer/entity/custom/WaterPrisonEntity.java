package com.wassuples.demonslayer.entity.custom;

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

import javax.annotation.Nullable;
import java.util.UUID;

public class WaterPrisonEntity extends Monster implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);
    private final int lifeTime = 1200; // 60 seconds
    @Nullable
    private UUID trappedUUID;

    public WaterPrisonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setTrappedEntity(LivingEntity target) {
        if (target != null) {
            this.trappedUUID = target.getUUID();
            target.startRiding(this, true);
        }
    }

    @Nullable
    private LivingEntity getTrappedEntity() {
        if (this.trappedUUID != null && this.level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.trappedUUID);
            return entity instanceof LivingEntity ? (LivingEntity) entity : null;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;

        LivingEntity trapped = getTrappedEntity();

        if (trapped == null || !trapped.isAlive() || this.tickCount > lifeTime) {
            this.discard();
            return;
        }

        if (!trapped.isPassenger()) {
            trapped.startRiding(this, true);
        }
        trapped.setPos(this.getX(), this.getY(), this.getZ());
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight() * 0.5;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.getEntity() != null && this.getPassengers().contains(pSource.getEntity())) {
            return false;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.trappedUUID != null) {
            pCompound.putUUID("TrappedUUID", this.trappedUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.hasUUID("TrappedUUID")) {
            this.trappedUUID = pCompound.getUUID("TrappedUUID");
        }
    }

    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void doPush(Entity pEntity) { }
    @Override
    public void knockback(double pStrength, double pX, double pZ) { }
    @Override
    protected void registerGoals() { }
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    public void travel(Vec3 pTravelVector) { }

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
}