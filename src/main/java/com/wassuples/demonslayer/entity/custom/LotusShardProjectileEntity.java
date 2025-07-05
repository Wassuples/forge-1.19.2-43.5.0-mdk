package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class LotusShardProjectileEntity extends Projectile implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public LotusShardProjectileEntity(EntityType<LotusShardProjectileEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public LotusShardProjectileEntity(Level pLevel, LivingEntity pShooter) {
        super(ModEntityTypes.LOTUS_SHARD.get(), pLevel);
        this.setOwner(pShooter);
        this.setPos(pShooter.getX(), pShooter.getEyeY() - 0.1, pShooter.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 80) {
            this.discard();
        }

        HitResult hitresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);

        if (this.level.isClientSide) {
            this.level.addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (pResult.getEntity() instanceof LivingEntity target && this.getOwner() instanceof LivingEntity caster) {
            // Don't hit the owner
            if (target.is(caster)) return;

            target.hurt(DamageSource.mobAttack(caster), 8.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.5F);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {}
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        // Even though our model is empty, we still need a predicate.
        // This won't do anything visually but is required to compile.
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