package com.wassuples.demonslayer.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
import java.util.EnumSet;
import java.util.UUID;

public class KillerGoldfishEntity extends Monster implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private UUID ownerUUID;

    public KillerGoldfishEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SpitNeedlesGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true, (e) -> !isOwner(e)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true, (e) -> !isOwner(e) && !(e instanceof KillerGoldfishEntity)));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 300) {
            this.discard();
        }
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }

    public boolean isOwner(Entity entity) {
        return entity != null && entity.getUUID().equals(this.ownerUUID);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.ownerUUID != null) {
            pCompound.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.hasUUID("OwnerUUID")) {
            this.ownerUUID = pCompound.getUUID("OwnerUUID");
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

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

    static class SpitNeedlesGoal extends Goal {
        private final KillerGoldfishEntity fish;
        private int attackCooldown;
        private int volleyTime;

        public SpitNeedlesGoal(KillerGoldfishEntity pFish) {
            this.fish = pFish;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.fish.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.attackCooldown = 20;
            this.volleyTime = 0;
        }

        @Override
        public void stop() {
            this.volleyTime = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.fish.getTarget();
            if (target == null) {
                return;
            }

            this.fish.getLookControl().setLookAt(target, 30.0F, 30.0F);

            this.attackCooldown--;

            if (this.volleyTime > 0) {
                this.volleyTime--;
                if (this.volleyTime % 5 == 0) {
                    Vec3 direction = target.getEyePosition().subtract(this.fish.position()).normalize();
                    PoisonNeedleProjectileEntity needle = new PoisonNeedleProjectileEntity(this.fish.level, this.fish);
                    needle.setPos(this.fish.getX(), this.fish.getEyeY(), this.fish.getZ());
                    needle.shoot(direction.x, direction.y, direction.z, 1.8f, 10.0f);
                    this.fish.level.addFreshEntity(needle);
                    this.fish.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.2F / (this.fish.getRandom().nextFloat() * 0.4F + 0.8F));
                }

                if (this.volleyTime == 0) {
                    this.attackCooldown = 60 + this.fish.random.nextInt(20);
                }
            }
            else if (this.attackCooldown <= 0) {
                this.volleyTime = 40;
            }
        }
    }
}