package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.ClearLesserEyesS2CPacket;
import com.wassuples.demonslayer.networking.packet.SyncLesserEyesS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MotherEyeEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    @Nullable
    private UUID casterUUID;
    private int lifeTime = 1200; // 60 seconds max
    private final List<BlockPos> lesserEyePositions = new ArrayList<>();

    public MotherEyeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;

        Entity caster = getCaster();
        if (caster == null || !this.getPassengers().contains(caster)) {
            ModMessages.sendToClients(new ClearLesserEyesS2CPacket());
            this.discard();
            return;
        }

        if (this.tickCount > lifeTime) {
            ModMessages.sendToClients(new ClearLesserEyesS2CPacket());
            this.discard();
            return;
        }

        if (this.tickCount % 40 == 0) {
            spawnLesserEyes();
        }
    }

    private void spawnLesserEyes() {
        if (this.level.isClientSide) return;

        List<BlockPos> newEyePositions = new ArrayList<>();
        double radius = 20.0;

        AABB searchArea = this.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != this && e != getCaster());

        for (LivingEntity target : targets) {
            if (target instanceof Player p && p.getInventory().getArmor(3).is(Items.CARVED_PUMPKIN)) {
                continue;
            }

            double distanceInFront = 5.0 + this.random.nextDouble() * 5.0;
            Vec3 spawnVec = target.position().add(target.getLookAngle().scale(distanceInFront));

            BlockPos groundPos = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(spawnVec));
            if (groundPos.distSqr(this.blockPosition()) > radius * radius) continue;
            if (groundPos.distSqr(target.blockPosition()) > 15*15) continue;

            newEyePositions.add(groundPos);
        }

        this.lesserEyePositions.clear();
        this.lesserEyePositions.addAll(newEyePositions);

        if (!newEyePositions.isEmpty()) {
            ModMessages.sendToClients(new SyncLesserEyesS2CPacket(newEyePositions));
        }
    }


    public List<BlockPos> getLesserEyePositions() {
        return this.lesserEyePositions;
    }

    @Nullable
    public Entity getCaster() {
        if (this.casterUUID != null && !this.level.isClientSide) {
            return ((ServerLevel)this.level).getEntity(this.casterUUID);
        }
        return null;
    }

    public void setCaster(@Nullable Entity caster) {
        this.casterUUID = caster != null ? caster.getUUID() : null;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.hasUUID("CasterUUID")) {
            this.casterUUID = pCompound.getUUID("CasterUUID");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.casterUUID != null) {
            pCompound.putUUID("CasterUUID", this.casterUUID);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity pEntity) { }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.isBypassInvul()) return super.hurt(pSource, pAmount);
        // Can only be hurt by direct attacks
        if (pSource.getDirectEntity() instanceof LivingEntity) {
            return super.hurt(pSource, pAmount);
        }
        return false;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() { }

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

    @Nullable
    public UUID getCasterUUID() {
        return this.casterUUID;
    }

    @Override
    public double getPassengersRidingOffset() {
        // Adjust the 0.25F value to move the player up or down as needed.
        return this.getBbHeight() * 0.001F;
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        ModMessages.sendToClients(new ClearLesserEyesS2CPacket());
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) { }
}
