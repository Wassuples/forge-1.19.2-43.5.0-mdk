package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ThrownNichirinEntity extends Projectile implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private int lifeTicks = 0;
    private boolean isReturning = false;
    private final Set<UUID> hitEntities = new HashSet<>();

    public ThrownNichirinEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrownNichirinEntity(Level pLevel, LivingEntity pShooter) {
        super(ModEntityTypes.THROWN_NICHIRIN.get(), pLevel);
        this.setOwner(pShooter);
        this.setPos(pShooter.getEyePosition());
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        // Start returning after 1 second or if it slows down too much
        if (!this.isReturning && (this.lifeTicks > 20 || this.getDeltaMovement().lengthSqr() < 0.1)) {
            this.isReturning = true;
            this.hitEntities.clear(); // Clear hit list for the return trip
        }

        if (this.isReturning) {
            Entity owner = this.getOwner();
            if (owner != null && owner.isAlive()) {
                // Home in on the owner
                Vec3 returnVec = owner.getEyePosition().subtract(this.position()).normalize();
                this.setDeltaMovement(returnVec.scale(1.5));
                // If close enough to the owner, discard
                if (this.distanceToSqr(owner) < 4.0) {
                    this.discard();
                }
            } else {
                this.discard(); // Discard if owner is gone
            }
        }

        // Standard projectile collision
        HitResult hitresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);

        if (this.level.isClientSide) {
            this.level.addParticle(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        Entity target = pResult.getEntity();
        if (target.is(this.getOwner()) || this.hitEntities.contains(target.getUUID())) {
            return; // Don't hit owner or already-hit targets
        }

        if (target instanceof LivingEntity livingTarget && this.getOwner() instanceof LivingEntity caster) {
            livingTarget.hurt(DamageSource.mobAttack(caster), 10.0F);
            this.hitEntities.add(target.getUUID());
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.2F);
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        // When hitting a block, just start returning immediately
        if (!this.isReturning) {
            this.isReturning = true;
            this.hitEntities.clear();
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // --- Geckolib ---
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("spin", true));
        return PlayState.CONTINUE;
    }
    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }
}