package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
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

public class ToonFakeTunnelEntity extends Entity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int MAX_LIFE_TICKS = 20 * 15;
    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = MAX_LIFE_TICKS;
    private Vec3 passageDirection = Vec3.ZERO;
    private int ownerPassCooldown;

    public ToonFakeTunnelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonFakeTunnelEntity(Level level, Entity owner, Vec3 facing) {
        this(ModEntityTypes.TOON_FAKE_TUNNEL.get(), level);
        this.ownerUUID = owner.getUUID();
        this.setYRot((float) (Math.atan2(-facing.x, facing.z) * (180.0 / Math.PI)));
    }

    public void setPassageDirection(Vec3 direction) {
        this.passageDirection = direction.normalize();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            if (this.ownerPassCooldown > 0) this.ownerPassCooldown--;

            if (this.ownerUUID != null && this.level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && serverLevel.getEntity(this.ownerUUID) instanceof net.minecraft.world.entity.LivingEntity owner
                    && owner.distanceToSqr(this) <= 12.25) {
                passOwnerThroughWall(owner);
            }
            for (net.minecraft.world.entity.LivingEntity entity : this.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, this.getBoundingBox().inflate(0.75),
                    entity -> this.ownerUUID == null || !this.ownerUUID.equals(entity.getUUID()))) {
                if (entity.getDeltaMovement().lengthSqr() > 0.04) {
                    com.wassuples.demonslayer.util.ToonForceHelper.tryApplyDizzy(entity, null, 0.5f);
                    com.wassuples.demonslayer.util.FormAnimations.play(entity, com.wassuples.demonslayer.util.FormAnimations.TOON_TUNNEL_SLAM);
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(-0.5));
                    entity.hurtMarked = true;
                    this.level.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.8f);
                    com.wassuples.demonslayer.util.ToonForceHelper.spawnBonkText((net.minecraft.server.level.ServerLevel) this.level, entity.position().add(0, entity.getBbHeight(), 0));
                }
            }
            if (--this.lifeTicks <= 0) {
                this.discard();
            }
        }
    }

    private void passOwnerThroughWall(net.minecraft.world.entity.LivingEntity owner) {
        if (this.ownerPassCooldown > 0 || this.passageDirection.lengthSqr() < 0.5
                || owner.getLookAngle().dot(this.passageDirection) <= 0.25) {
            return;
        }

        // Collision has already zeroed movement when a player reaches a wall. Use look direction,
        // require that the scan actually crosses solid geometry, then accept only a far-side exit.
        boolean crossedWall = false;
        for (double distance = 0.2; distance <= 5.0; distance += 0.2) {
            Vec3 candidate = this.position().add(this.passageDirection.scale(distance));
            boolean safe = this.level.noCollision(owner, owner.getBoundingBox().move(candidate.subtract(owner.position())));
            if (!safe) {
                crossedWall = true;
            } else if (crossedWall) {
                owner.teleportTo(candidate.x, candidate.y, candidate.z);
                owner.fallDistance = 0.0f;
                owner.hurtMarked = true;
                this.ownerPassCooldown = 12;
                this.level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.PAINTING_PLACE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.7f);
                if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                            owner.getX(), owner.getY() + 1.0, owner.getZ(), 18, 0.3, 0.6, 0.3, 0.02);
                }
                return;
            }
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.lifeTicks = tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : MAX_LIFE_TICKS;
        this.passageDirection = new Vec3(tag.getDouble("PassageX"), tag.getDouble("PassageY"), tag.getDouble("PassageZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putDouble("PassageX", this.passageDirection.x);
        tag.putDouble("PassageY", this.passageDirection.y);
        tag.putDouble("PassageZ", this.passageDirection.z);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
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
