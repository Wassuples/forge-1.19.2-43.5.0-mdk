package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.UUID;

public class ToonAnvilEntity extends Entity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private UUID ownerUUID;
    private int lifeTicks = 100;

    public ToonAnvilEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public ToonAnvilEntity(Level level, LivingEntity owner) {
        super(ModEntityTypes.TOON_ANVIL.get(), level);
        this.ownerUUID = owner.getUUID();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.hasUUID("Owner")) {
            this.ownerUUID = nbt.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (this.ownerUUID != null) {
            nbt.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));
        }
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        if (!this.level.isClientSide) {
            if (this.onGround || this.horizontalCollision) {
                this.impact();
            }

            if (this.lifeTicks-- <= 0) {
                this.discard();
            }
        }
    }

    private void impact() {
        this.level.playSound(null, this.blockPosition(), ModSounds.TOON_CRASH.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 18, 0.7, 0.25, 0.7, 0.02);
            ToonForceHelper.spawnToonText(serverLevel, this.position().add(0, 1.0, 0),
                    net.minecraft.network.chat.Component.literal("KLANG!").withStyle(net.minecraft.ChatFormatting.GRAY), 24);

            AABB box = this.getBoundingBox().inflate(1.5, 1.0, 1.5);
            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (ownerUUID != null && target.getUUID().equals(ownerUUID)) continue;

                Entity owner = ownerUUID != null ? serverLevel.getEntity(ownerUUID) : null;
                DamageSource ds = owner instanceof LivingEntity livingOwner ? DamageSource.ANVIL : DamageSource.ANVIL;
                target.hurt(ds, 15.0f);
                ToonForceHelper.applyFlattened(target, 40);
                ToonForceHelper.tryApplyDizzy(target, owner instanceof LivingEntity l ? l : null, 0.5f);
                ToonForceHelper.spawnBonkText(serverLevel, target.position());
                FormAnimations.play(target, FormAnimations.TOON_BONKED);
            }
        }
        this.discard();
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
