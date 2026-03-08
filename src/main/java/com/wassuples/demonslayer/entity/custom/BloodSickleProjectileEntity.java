package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.mojang.math.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
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

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Custom entity: Blood Sickle Projectile.
 */
public class BloodSickleProjectileEntity extends Projectile implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private UUID targetUUID;
    private int life = 60; // Despawns after 3 seconds

    public BloodSickleProjectileEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BloodSickleProjectileEntity(Level pLevel, LivingEntity pShooter, @Nullable LivingEntity pTarget) {
        super(ModEntityTypes.BLOOD_SICKLE.get(), pLevel);
        this.setOwner(pShooter);
        if (pTarget != null) {
            this.targetUUID = pTarget.getUUID();
        }
        this.setPos(pShooter.getX(), pShooter.getEyeY() - 0.1, pShooter.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.life-- <= 0) {
            this.discard();
            return;
        }

        if (!this.level.isClientSide && this.targetUUID != null) {
            Entity target = ((ServerLevel) this.level).getEntity(this.targetUUID);
            if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                Vec3 vectorToTarget = livingTarget.getEyePosition().subtract(this.position()).normalize();
                this.setDeltaMovement(this.getDeltaMovement().lerp(vectorToTarget.scale(0.8), 0.2));
            } else {
                this.targetUUID = null;
            }
        }

        HitResult hitresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);

        if (this.level.isClientSide) {
            final DustParticleOptions bloodRed = new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 0.8f);
            this.level.addParticle(bloodRed, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        Entity owner = this.getOwner();
        if (pResult.getEntity() == owner) return;

        if (pResult.getEntity() instanceof LivingEntity target && owner instanceof LivingEntity caster) {
            target.hurt(DamageSource.mobAttack(caster), 6.0F);
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.2F);
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