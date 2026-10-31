package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ToonEyeProjectileEntity extends ThrowableItemProjectile implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int MAX_LIFE_TICKS = 20 * 30;
    private static final int MAX_BOUNCES = 128;
    private int lifeTicks = MAX_LIFE_TICKS;
    private int bounces;

    public ToonEyeProjectileEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public ToonEyeProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntityTypes.TOON_EYE.get(), owner, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_EYE;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) {
            Vec3 velocity = this.getDeltaMovement();
            this.level.addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                    -velocity.x * 0.03, -velocity.y * 0.03, -velocity.z * 0.03);
            if (this.tickCount % 2 == 0) {
                this.level.addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(),
                        -velocity.x * 0.05, -velocity.y * 0.05, -velocity.z * 0.05);
            }
        }

        if (!this.level.isClientSide && --this.lifeTicks <= 0) {
            this.discard();
        }

        if (!this.level.isClientSide && this.tickCount > 15) {
            LivingEntity target = this.level.getEntitiesOfClass(LivingEntity.class,
                            this.getBoundingBox().inflate(24.0), this::isEligibleTarget)
                    .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (target != null) {
                Vec3 desired = target.getEyePosition().subtract(this.position()).normalize().scale(3.6);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.9).add(desired.scale(0.1)).normalize().scale(3.6));
            }
        }
    }

    private boolean isEligibleTarget(LivingEntity target) {
        return target.isAlive() && target != this.getOwner() && !target.isSpectator();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return (entity != this.getOwner() || this.tickCount >= 8) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level.isClientSide) {
            return;
        }

        Entity hit = result.getEntity();
        if (hit instanceof LivingEntity target && this.getOwner() instanceof LivingEntity owner && hit != owner) {
            target.hurt(DamageSource.mobAttack(owner), 4.0f);
            ToonForceHelper.tryApplyDizzy(target, owner, 0.3f);
            ToonForceHelper.spawnBonkText((net.minecraft.server.level.ServerLevel) this.level, target.position());
            FormAnimations.play(target, FormAnimations.TOON_BONKED);
            this.level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 2.0f);
        }

        this.discard();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.level.isClientSide) {
            return;
        }

        if (hitResult instanceof BlockHitResult blockHit) {
            this.bounces++;
            Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
            Vec3 velocity = this.getDeltaMovement();
            double dot = velocity.dot(normal);
            Vec3 reflected = velocity.subtract(normal.scale(2.0 * dot));

            if (reflected.lengthSqr() < 0.04) {
                reflected = normal.scale(3.0);
            } else {
                reflected = reflected.normalize().scale(Math.max(2.8, Math.min(4.6, reflected.length())));
            }

            this.setDeltaMovement(reflected.add(0, Mth.clamp(0.04, 0, 0.12), 0));
            if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 8, 0.14, 0.14, 0.14, 0.02);
                if (this.bounces <= 4) {
                    ToonForceHelper.spawnToonText(serverLevel, this.position().add(0, 0.35, 0),
                            net.minecraft.network.chat.Component.literal("BOING!").withStyle(net.minecraft.ChatFormatting.AQUA), 12);
                }
            }
            this.level.playSound(null, blockPosition(), ModSounds.TOON_BOING.get(), SoundSource.PLAYERS, 0.8f, 0.9f + this.random.nextFloat() * 0.25f);
            if (this.bounces >= MAX_BOUNCES) {
                this.discard();
            }
            return;
        }

        super.onHit(hitResult);
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
