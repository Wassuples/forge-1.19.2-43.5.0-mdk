package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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

    private static final String OWNER_KEY = "OwnerUUID";
    private static final String THROW_KEY = "ThrowId";

    private UUID throwId = null;
    public void setThrowId(UUID id) { this.throwId = id; }
    public UUID getThrowId() { return this.throwId; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwner() != null) {
            tag.putUUID(OWNER_KEY, this.getOwner().getUUID());
        }
        if (throwId != null) {
            tag.putUUID(THROW_KEY, throwId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(OWNER_KEY)) {
            UUID ownerId = tag.getUUID(OWNER_KEY);
            // this will only resolve on the client after the world knows the entity
            Entity possibleOwner = ((ServerLevel) this.level).getEntity(ownerId);
            if (possibleOwner instanceof LivingEntity le) {
                this.setOwner(le);
            }
        }
        if (tag.hasUUID(THROW_KEY)) {
            this.throwId = tag.getUUID(THROW_KEY);
        }
    }

    public ThrownNichirinEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrownNichirinEntity(Level pLevel, LivingEntity pShooter) {
        super(ModEntityTypes.THROWN_NICHIRIN.get(), pLevel);
        this.setOwner(pShooter);
        this.setPos(pShooter.getEyePosition());
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();

        DemonSlayer.LOGGER.debug("[ThrownNichirin] tick: id={} ticks={} returning={} serverSide={}",
                this.getUUID(),
                this.lifeTicks,
                this.isReturning,
                !this.level.isClientSide);

        this.lifeTicks++;

        // Start returning after 1 second or if it slows down too much
        if (!this.isReturning && (this.lifeTicks > 20 || this.getDeltaMovement().lengthSqr() < 0.1)) {
            startReturning();
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

        HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
        switch (hit.getType()) {
            case ENTITY -> this.onHitEntity((EntityHitResult) hit);
            case BLOCK  -> this.onHit(hit);
            default     -> { /* nothing */ }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level.isClientSide) {
            this.level.addParticle(ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0);
        }
    }

    @Override
    public boolean canHitEntity(Entity target) {
        if (this.getOwner() == null) {
            return false;
        }

        return target instanceof LivingEntity
                && !target.getUUID().equals(this.getOwner().getUUID());
    }


    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        DemonSlayer.LOGGER.debug("[ThrownNichirin] onHitEntity: target={} owner={} returning={}",
                pResult.getEntity().getUUID(),
                this.getOwner() == null ? "null" : this.getOwner().getUUID(),
                this.isReturning);
        if (level.isClientSide) return;
        Entity target = pResult.getEntity();
        if (target instanceof LivingEntity living && getOwner() instanceof LivingEntity shooter) {
            DemonSlayer.LOGGER.debug("[ThrownNichirin] dealing damage to {}", living.getUUID());
            living.hurt(DamageSource.mobAttack(shooter), 10.0F);
            hitEntities.add(living.getUUID());
            level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, getSoundSource(), 1.0F, 1.2F);
        }
        if (!this.isReturning) {
            startReturning();
            this.hitEntities.clear();
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        // When hitting a block, just start returning immediately
        if (!this.isReturning) {
            startReturning();
            this.hitEntities.clear();
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void startReturning() {
        if (this.throwId == null) return;
        // server side only:
        ServerLevel server = (ServerLevel) this.level;
        // find all blades in the vicinity (or in the entire world!) with the same throwId
        server.getEntitiesOfClass(
                ThrownNichirinEntity.class,
                this.getBoundingBox().inflate(64),    // search radius 64 — adjust as needed
                other -> throwId.equals(other.getThrowId())
        ).forEach(other -> {
            other.isReturning = true;
            other.hitEntities.clear();
        });
    }


    // --- Geckolib ---
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("spin", true));
        return PlayState.CONTINUE;
    }
    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }
}