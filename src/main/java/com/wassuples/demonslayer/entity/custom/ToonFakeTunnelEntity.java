package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public class ToonFakeTunnelEntity extends Entity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int MAX_LIFE_TICKS = 20 * 15;
    private static final String OWNER_UUID_TAG = "OwnerUUID";
    private static final String FACING_X_TAG = "FacingX";
    private static final String FACING_Y_TAG = "FacingY";
    private static final String FACING_Z_TAG = "FacingZ";
    private java.util.UUID ownerUUID;
    private Vec3 facing = new Vec3(0.0, 0.0, 1.0);

    public ToonFakeTunnelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonFakeTunnelEntity(Level level, Entity owner, Vec3 facing) {
        this(ModEntityTypes.TOON_FAKE_TUNNEL.get(), level);
        this.ownerUUID = owner.getUUID();
        Vec3 horizontalFacing = new Vec3(facing.x, 0.0, facing.z);
        if (horizontalFacing.lengthSqr() > 0.0001) {
            this.facing = horizontalFacing.normalize();
        }
        this.setYRot((float) (net.minecraft.util.Mth.atan2(this.facing.z, this.facing.x) * (180.0f / Math.PI)) - 90.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            for (LivingEntity entity : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.65))) {
                if (this.ownerUUID != null && this.ownerUUID.equals(entity.getUUID())) {
                    continue;
                }
                if (entity.getDeltaMovement().lengthSqr() > 0.02) {
                    ToonForceHelper.tryApplyDizzy(entity, null, 0.45f);
                    FormAnimations.play(entity, FormAnimations.TOON_TUNNEL_SLAM);
                    Vec3 launch = this.facing.scale(-0.95).add(0.0, 0.12, 0.0);
                    entity.setDeltaMovement(launch);
                    entity.hurtMarked = true;
                    this.level.playSound(null, entity.blockPosition(), ModSounds.TOON_CRASH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.2f);
                    ToonForceHelper.spawnBonkText((net.minecraft.server.level.ServerLevel) this.level, entity.position().add(0, entity.getBbHeight(), 0));
                }
            }

            if (this.tickCount > MAX_LIFE_TICKS) {
                this.discard();
            }
        } else if (this.tickCount % 4 == 0) {
            this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.2, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            this.ownerUUID = tag.getUUID(OWNER_UUID_TAG);
        }
        if (tag.contains(FACING_X_TAG) && tag.contains(FACING_Y_TAG) && tag.contains(FACING_Z_TAG)) {
            this.facing = new Vec3(tag.getDouble(FACING_X_TAG), tag.getDouble(FACING_Y_TAG), tag.getDouble(FACING_Z_TAG));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID(OWNER_UUID_TAG, this.ownerUUID);
        }
        tag.putDouble(FACING_X_TAG, this.facing.x);
        tag.putDouble(FACING_Y_TAG, this.facing.y);
        tag.putDouble(FACING_Z_TAG, this.facing.z);
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
