package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
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

public class PoisonNeedleProjectileEntity extends AbstractArrow implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public PoisonNeedleProjectileEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public PoisonNeedleProjectileEntity(Level pLevel, LivingEntity pShooter) {
        super(ModEntityTypes.POISON_NEEDLE.get(), pShooter, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide && !this.inGround) {
            this.level.addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        Entity owner = this.getOwner();
        if (target == owner) return;

        // Deal very minor damage
        target.hurt(DamageSource.thrown(this, owner), 2.0F);

        // Apply the paralyzing poison
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(ModEffects.PARALYZING_POISON.get(), 60, 0));
        }
        super.onHitEntity(pResult);
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
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

}