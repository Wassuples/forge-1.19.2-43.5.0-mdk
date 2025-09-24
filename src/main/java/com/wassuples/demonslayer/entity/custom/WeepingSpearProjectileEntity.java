package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WeepingSpearProjectileEntity extends AbstractArrow implements IAnimatable {
    private int life = 80;
    private final int maxPierces = 3;
    private final List<UUID> hitEntities = new ArrayList<>();

    private final AnimationFactory factory = new AnimationFactory(this);

    public WeepingSpearProjectileEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public WeepingSpearProjectileEntity(Level pLevel, LivingEntity pShooter) {
        super(ModEntityTypes.WEEPING_SPEAR.get(), pShooter, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (--this.life <= 0) {
            this.discard();
        }
        if (this.level.isClientSide && !this.inGround) {
            this.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        Entity owner = this.getOwner();

        if (target == owner || this.hitEntities.contains(target.getUUID())) {
            return;
        }

        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource = DamageSource.indirectMagic(this, owner).setProjectile().bypassArmor();
            livingTarget.hurt(damageSource, 15.0F);

            this.hitEntities.add(target.getUUID());
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_HIT, this.getSoundSource(), 1.0F, 1.0F);

            if (this.hitEntities.size() >= this.maxPierces) {
                this.discard();
            }
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        if (pResult.getType() == HitResult.Type.ENTITY) {
            super.onHit(pResult);
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
        event.getController().setAnimation(new AnimationBuilder().addAnimation("spin", true));
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