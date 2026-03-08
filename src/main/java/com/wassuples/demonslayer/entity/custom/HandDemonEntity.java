package com.wassuples.demonslayer.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Custom entity: Hand Demon.
 */
public class HandDemonEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public HandDemonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D) // Slow and lumbering
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 8.0D) // A tough, fleshy hide
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MultiArmSlamGoal(this));
        this.goalSelector.addGoal(2, new DesperateGrabGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- Sound Events ---
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override protected void playStepSound(BlockPos pPos, net.minecraft.world.level.block.state.BlockState pState) { this.playSound(SoundEvents.RAVAGER_STEP, 0.5F, 1.0F); }

    // --- Geckolib Animation ---
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hand_demon.walk", true));
        } else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.hand_demon.idle", true));
        }
        return PlayState.CONTINUE;
    }
    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }

    // =================================================================================
    // CUSTOM AI GOALS
    // =================================================================================

    static class MultiArmSlamGoal extends Goal {
        private final HandDemonEntity demon;
        private int attackTick;
        private final int attackDelay = 20; // 1 second wind-up
        private final int cooldown = 60; // 3 second cooldown

        public MultiArmSlamGoal(HandDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.demon.getTarget();
            return target != null && target.isAlive() && this.demon.distanceToSqr(target) < 25.0; // 5 block range
        }

        @Override
        public void start() {
            this.attackTick = 0;
            this.demon.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.demon.getTarget();
            if (target == null) return;

            this.demon.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
            this.attackTick++;

            if (this.attackTick == this.attackDelay) {
                // Perform the slam
                this.demon.level.playSound(null, this.demon.blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5f, 0.8f);
                AABB slamZone = this.demon.getBoundingBox().inflate(4.0, 0.0, 4.0).move(this.demon.getLookAngle().scale(2.0));
                List<LivingEntity> hitEntities = this.demon.level.getEntitiesOfClass(LivingEntity.class, slamZone, e -> e != this.demon && e.isAlive());

                for (LivingEntity entity : hitEntities) {
                    entity.hurt(DamageSource.mobAttack(this.demon), (float)this.demon.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    entity.knockback(1.2F, -this.demon.getLookAngle().x, -this.demon.getLookAngle().z);
                }

                if (this.demon.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), this.demon.getX(), this.demon.getY(), this.demon.getZ(), 100, 3.0, 0.5, 3.0, 0.5);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.attackTick <= this.attackDelay + 5;
        }

        @Override
        public void stop() {
            this.demon.setAggressive(false);
        }
    }

    static class DesperateGrabGoal extends Goal {
        private final HandDemonEntity demon;
        private LivingEntity target;
        private int grabTick;
        private final int grabDuration = 100; // Grabs for 5 seconds

        public DesperateGrabGoal(HandDemonEntity demon) {
            this.demon = demon;
        }

        @Override
        public boolean canUse() {
            // Only use this when health is low and has a target
            return this.demon.getHealth() < this.demon.getMaxHealth() * 0.4 && this.demon.getTarget() != null;
        }

        @Override
        public void start() {
            this.target = this.demon.getTarget();
            this.grabTick = 0;
            this.demon.level.playSound(null, this.demon.blockPosition(), SoundEvents.HOGLIN_ANGRY, SoundSource.HOSTILE, 1.0f, 0.5f);
        }

        @Override
        public void tick() {
            if (this.target == null || !this.target.isAlive() || this.grabTick > this.grabDuration) {
                this.stop();
                return;
            }

            this.grabTick++;
            this.demon.getLookControl().setLookAt(this.target);

            // Pull the target in and immobilize them
            Vec3 pullVec = this.demon.getEyePosition().subtract(this.target.position()).normalize().scale(0.5);
            this.target.setDeltaMovement(pullVec);
            this.target.hurt(DamageSource.mobAttack(this.demon), 1.0f); // Small continuous damage
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}
