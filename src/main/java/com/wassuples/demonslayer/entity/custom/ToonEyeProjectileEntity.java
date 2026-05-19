package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.util.ToonForceHelper;
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
    private static final int MAX_LIFE_TICKS = 20 * 120; // 2 minutes
    private int lifeTicks = MAX_LIFE_TICKS;

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
            this.level.addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }

        if (!this.level.isClientSide && --this.lifeTicks <= 0) {
            this.discard();
        }
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
            this.level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.8f);
        }

        this.discard();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.level.isClientSide) {
            return;
        }

        if (hitResult instanceof BlockHitResult blockHit) {
            Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
            Vec3 velocity = this.getDeltaMovement();
            double dot = velocity.dot(normal);
            Vec3 reflected = velocity.subtract(normal.scale(2.0 * dot)).scale(0.9);

            if (reflected.lengthSqr() < 0.04) {
                reflected = reflected.normalize().scale(0.6);
            }

            this.setDeltaMovement(reflected);
            this.setDeltaMovement(this.getDeltaMovement().add(0, Mth.clamp(0.02, 0, 0.1), 0));
            this.level.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.5f, 1.6f);
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

