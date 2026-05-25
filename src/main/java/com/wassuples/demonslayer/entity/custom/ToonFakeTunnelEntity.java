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

public class ToonFakeTunnelEntity extends Entity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    public ToonFakeTunnelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonFakeTunnelEntity(Level level, Entity owner, Vec3 facing) {
        this(ModEntityTypes.TOON_FAKE_TUNNEL.get(), level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            for (net.minecraft.world.entity.LivingEntity entity : this.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, this.getBoundingBox().inflate(0.5))) {
                if (entity.getDeltaMovement().lengthSqr() > 0.04) {
                    com.wassuples.demonslayer.util.ToonForceHelper.tryApplyDizzy(entity, null, 0.5f);
                    com.wassuples.demonslayer.util.FormAnimations.play(entity, com.wassuples.demonslayer.util.FormAnimations.TOON_TUNNEL_SLAM);
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(-0.5));
                    entity.hurtMarked = true;
                    this.level.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.8f);
                    com.wassuples.demonslayer.util.ToonForceHelper.spawnBonkText((net.minecraft.server.level.ServerLevel) this.level, entity.position().add(0, entity.getBbHeight(), 0));
                    this.discard();
                    return;
                }
            }
            if (this.tickCount > 200) {
                this.discard();
            }
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
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
