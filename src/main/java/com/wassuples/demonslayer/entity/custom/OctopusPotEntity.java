package com.wassuples.demonslayer.entity.custom;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

import java.util.List;
import java.util.UUID;

public class OctopusPotEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final int lifeTime = 400;
    private UUID ownerUUID;

    // A flag to track the animation state.
    private final boolean hasEmerged = false;

    public OctopusPotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D) // Very durable
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setOwner(LivingEntity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;
        if (this.tickCount > lifeTime) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 == 0) {
            float damage = 12.0f;
            double radius = 10.0;

            LivingEntity owner = null;
            if (this.ownerUUID != null && this.level instanceof ServerLevel serverLevel) {
                Entity ownerEntity = serverLevel.getEntity(this.ownerUUID);
                if (ownerEntity instanceof LivingEntity) {
                    owner = (LivingEntity) ownerEntity;
                }
            }

            DamageSource damageSource = owner != null ? DamageSource.mobAttack(owner) : DamageSource.mobAttack(this);

            List<LivingEntity> nearbyEntities = this.level.getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(radius)
            );

            for (LivingEntity target : nearbyEntities) {
                if (target == this || target.equals(owner)) {
                    continue;
                }

                if (this.distanceToSqr(target) < radius * radius) {
                    target.hurt(damageSource, damage);
                }
            }

            this.level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, this.getSoundSource(), 1.0f, 1.5f);
        }
    }

    @Override
    public void travel(Vec3 pTravelVector) {}
    @Override
    public void knockback(double pStrength, double pX, double pZ) {}
    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void doPush(Entity pEntity) {}
    @Override
    protected void registerGoals() {}
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // --- Animation ---
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationController<E> controller = event.getController();
        if (this.tickCount < 15) {
            controller.setAnimation(new AnimationBuilder().addAnimation("emerge", false));
        } else {
            controller.setAnimation(new AnimationBuilder().addAnimation("flail", true));
        }

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
