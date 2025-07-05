package com.wassuples.demonslayer.entity.custom;

// --- Add all necessary imports ---
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

public class ColdWhitePrincessEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private LivingEntity owner;

    public ColdWhitePrincessEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D) // Give them 20 health
                .add(Attributes.MOVEMENT_SPEED, 0.0D); // They don't move
    }

    @Override
    protected void defineSynchedData() {
        // This is required for all entities, even if it's empty.
        super.defineSynchedData();
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new IcyBreathAttackGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (entity) -> !isOwner(entity)));
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    public boolean isOwner(LivingEntity entity) {
        return this.owner != null && this.owner.getUUID().equals(entity.getUUID());
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);
        if (this.tickCount > 400) {
            this.discard();
        }
        if (this.level.isClientSide && this.level.random.nextInt(10) == 0) {
            this.level.addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0, 0);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
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
            this.princess.level.playSound(null, this.princess.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.0f, 1.5f);
        }

        @Override
        public void stop() {
            this.target = null;
        }

        @Override
        public void tick() {
            if (this.target == null || !this.target.isAlive()) {
                this.stop();
                return;
            }

            this.attackTime++;
            this.princess.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            Vec3 startPos = this.princess.getEyePosition();
            Vec3 endPos = this.target.getEyePosition();
            Vec3 path = endPos.subtract(startPos);
            double distance = path.length();

            for (double d = 0; d < distance; d += 0.5) {
                Vec3 point = startPos.add(path.normalize().scale(d));
                this.princess.level.addParticle(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 0, 0, 0);
                if (this.attackTime % 5 == 0) {
                    this.princess.level.addParticle(ParticleTypes.WHITE_ASH, point.x, point.y, point.z, 0, 0, 0);
                }

                BlockPos blockPos = new BlockPos(point);
                if (this.princess.level.getBlockState(blockPos).is(Blocks.WATER)) {
                    this.princess.level.setBlockAndUpdate(blockPos, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }

            if (this.attackTime % 10 == 0) {
                this.target.hurt(DamageSource.mobAttack(this.princess), 2.0f);
                this.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            }
        }
    }
}
