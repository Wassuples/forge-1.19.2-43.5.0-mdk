package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ally.SummonAllies;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Custom entity: Cutting Thread Cage.
 */
public class CuttingThreadCageEntity extends Monster implements IAnimatable {
    private static final EntityDataAccessor<Float> DATA_SCALE_ID = SynchedEntityData.defineId(CuttingThreadCageEntity.class, EntityDataSerializers.FLOAT);
    private int lifeTicks = 100; // Lasts for 5 seconds
    @Nullable
    private UUID ownerUUID;

    public CuttingThreadCageEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0D);
    }

    @Override
    public void tick() {
        this.baseTick();
        if (!this.level.isClientSide) {
            this.setDeltaMovement(0, 0, 0);
        }

        if (!this.level.isClientSide) {
            if (this.tickCount % 10 == 0) {
                double currentRadius = (this.getBbWidth() / 2.0f);
                this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(),
                                entity -> !(entity instanceof DemonEntity)
                                        && !entity.getUUID().equals(this.ownerUUID)
                                        && !SummonAllies.areAllies(this, entity))
                        .forEach(entity -> {
                            if (this.position().distanceToSqr(entity.position()) > (currentRadius - entity.getBbWidth()) * (currentRadius - entity.getBbWidth())) {
                                entity.hurt(DamageSource.MAGIC, 10.0F);
                            }
                        });

                this.level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 2.0F);
            }

            float scale = (float)this.lifeTicks / 100.0F;
            this.setScale(Math.max(0.0F, scale));
            this.refreshDimensions();

            // Despawn timer
            this.lifeTicks--;
            if (this.lifeTicks <= 0) {
                this.discard();
            }
        }
    }

    @Override
    public void die(DamageSource pDamageSource) {
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.5F);
            this.discard();
        }
    }

    // --- Getters, Setters, and Data ---
    public float getScale() { return this.entityData.get(DATA_SCALE_ID); }
    public void setScale(float scale) { this.entityData.set(DATA_SCALE_ID, scale); }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SCALE_ID, 1.0f);
    }

    // --- Boilerplate ---
    @Override public boolean isInvulnerableTo(DamageSource pSource) { return pSource == DamageSource.OUT_OF_WORLD || super.isInvulnerableTo(pSource); }
    @Override protected void registerGoals() {}
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.hasUUID("OwnerUUID")) {
            this.ownerUUID = pCompound.getUUID("OwnerUUID");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.ownerUUID != null) {
            pCompound.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    // --- Geckolib Animation ---
    private final AnimationFactory factory = new AnimationFactory(this);
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;
    }
    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }
}
