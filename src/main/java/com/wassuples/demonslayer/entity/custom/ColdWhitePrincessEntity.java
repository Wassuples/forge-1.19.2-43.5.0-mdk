package com.wassuples.demonslayer.entity.custom;

// --- Add all necessary imports ---
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColdWhitePrincessEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private LivingEntity owner;

    public ColdWhitePrincessEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        // This is required for all entities, even if it's empty.
        super.defineSynchedData();
    }

    @Override
    public @NotNull Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new IcyBreathAttackGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, (entity) -> !isOwner(entity) && !(entity instanceof ColdWhitePrincessEntity)));
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {}

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return false;
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public boolean isOwner(LivingEntity entity) {
        return this.owner != null && this.owner.getUUID().equals(entity.getUUID());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 400) {
            this.discard();
        }
        if (this.level.isClientSide && this.level.random.nextInt(10) == 0) {
            this.level.addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0, 0);
        }
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource pSource) {
        return pSource == DamageSource.FREEZE || (pSource.getEntity() != null && isOwner((LivingEntity) pSource.getEntity())) || super.isInvulnerableTo(pSource);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (this.getTarget() != null) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.cold_white_princess.attack", true));
        } else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.cold_white_princess.idle", true));
        }
        return PlayState.CONTINUE;
    }
    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }

    static class IcyBreathAttackGoal extends Goal {
        private final ColdWhitePrincessEntity princess;
        private LivingEntity target;
        private int attackTime;

        public IcyBreathAttackGoal(ColdWhitePrincessEntity princess) {
            this.princess = princess;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.target = this.princess.getTarget();
            return this.target != null && this.target.isAlive();
        }

        @Override
        public void start() {
            this.attackTime = 0;
            this.princess.level.playSound(null, this.princess.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.8f);
        }

        @Override
        public void stop() {
            this.target = null;
        }

        @Override
        public void tick() {
            if (this.target == null || !this.target.isAlive() || this.princess.distanceToSqr(this.target) > 20 * 20) {
                this.stop();
                return;
            }

            this.attackTime++;
            this.princess.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
                       // Play a continuous "breathing" sound
            if (this.attackTime % 4 == 0) {
                this.princess.level.playSound(null, this.princess.getX(), this.princess.getEyeY(), this.princess.getZ(), SoundEvents.PLAYER_BREATH, SoundSource.HOSTILE, 1.5f, 0.5f);
            }
            
            Vec3 breathOrigin = this.princess.getEyePosition().subtract(0, 0.2, 0);
            Vec3 direction = this.target.getEyePosition().subtract(breathOrigin).normalize();
            double breathLength = 15.0; // Long reach
            double coneAngle = 0.4; // How wide the cone gets
            
            // Set to keep track of who we hit this tick to prevent multi-damage
            final Set<LivingEntity> hitThisTick = new HashSet<>();
            
            // Create the cone effect
            for (double d = 1.0; d < breathLength; d += 0.5) {
                double currentRadius = d * coneAngle;
                Vec3 centerPoint = breathOrigin.add(direction.scale(d));
                
                // Spawn a ring of particles to form the cone
                for (int i = 0; i < 8; i++) {
                    Vec3 randomOffset = new Vec3(this.princess.level.random.nextDouble() - 0.5, this.princess.level.random.nextDouble() - 0.5, this.princess.level.random.nextDouble() - 0.5)
                            .normalize().scale(this.princess.level.random.nextDouble() * currentRadius);
                    Vec3 particlePos = centerPoint.add(randomOffset);
                    this.princess.level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 0, -0.05, 0);
                }
                
                // Damage entities within the cone
                AABB damageZone = new AABB(centerPoint, centerPoint).inflate(currentRadius);
                List<LivingEntity> entitiesInCone = this.princess.level.getEntitiesOfClass(LivingEntity.class, damageZone, (entity) -> !this.princess.isOwner(entity) && entity != this.princess && entity.isAlive());
                hitThisTick.addAll(entitiesInCone);
                
                // Freeze blocks within the cone
                BlockPos centerBlockPos = new BlockPos(centerPoint);
                for (BlockPos pos : BlockPos.betweenClosed(centerBlockPos.offset((int)-currentRadius, (int)-currentRadius, (int)-currentRadius), centerBlockPos.offset((int)currentRadius, (int)currentRadius, (int)currentRadius))) {
                    if (this.princess.level.getBlockState(pos).is(Blocks.WATER)) {
                        this.princess.level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    }
                    // New: Place snow on top of non-air blocks to "freeze" them
                    if (this.princess.level.getBlockState(pos).isSolidRender(this.princess.level, pos) && !this.princess.level.getBlockState(pos).is(Blocks.SNOW) && this.princess.level.getBlockState(pos.above()).isAir()) {
                        if (this.princess.level.random.nextInt(20) == 0) { // Don't freeze every single block
                            this.princess.level.setBlockAndUpdate(pos.above(), Blocks.SNOW.defaultBlockState());
                        }
                    }
                }
            }
            
            // Apply damage and effects to all entities hit in the cone this tick
            for (LivingEntity hitEntity : hitThisTick) {
                hitEntity.hurt(DamageSource.mobAttack(this.princess), 2.0f);
                hitEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
        }
    }
}