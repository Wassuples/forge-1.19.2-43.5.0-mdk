package com.wassuples.demonslayer.entity.custom;

import com.mojang.authlib.GameProfile;
import com.wassuples.demonslayer.entity.ai.DomaCloneAttackGoal;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CrystallineDivineChildEntity extends HashiraEntity {
    private static final EntityDataAccessor<CompoundTag> OWNER_PROFILE_TAG =
            SynchedEntityData.defineId(CrystallineDivineChildEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> IS_NPC_CLONE =
            SynchedEntityData.defineId(CrystallineDivineChildEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private LivingEntity owner;

    public CrystallineDivineChildEntity(EntityType<? extends HashiraEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setBreathingStyle(BreathingStyles.CRYO);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DomaCloneAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (entity) -> !isOwner(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 800) { // 40 second lifespan
            this.shatter();
        }
    }

    @Override
    public void die(DamageSource pDamageSource) {
        this.shatter();
        super.die(pDamageSource);
    }

    private void shatter() {
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    this.getX(), this.getY(), this.getZ(), 150, 0.5, 1.0, 0.5, 0.5);
        }
        this.level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
        this.discard();
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        if (owner != null) {
            // Copy owner's stats
            copyAttribute(Attributes.MAX_HEALTH, owner);
            copyAttribute(Attributes.ATTACK_DAMAGE, owner);
            this.setHealth(this.getMaxHealth());
        }
    }

    private void copyAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute, LivingEntity owner) {
        AttributeInstance ownerInstance = owner.getAttribute(attribute);
        AttributeInstance cloneInstance = this.getAttribute(attribute);
        if (ownerInstance != null && cloneInstance != null) {
            cloneInstance.setBaseValue(ownerInstance.getBaseValue());
        }
    }

    public boolean isOwner(LivingEntity entity) { return this.owner != null && this.owner.getUUID().equals(entity.getUUID()); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_PROFILE_TAG, new CompoundTag());
        this.entityData.define(IS_NPC_CLONE, false);
    }

    public void setOwnerProfile(GameProfile profile) {
        CompoundTag tag = new CompoundTag();
        NbtUtils.writeGameProfile(tag, profile);
        this.entityData.set(OWNER_PROFILE_TAG, tag);
    }
    public Optional<GameProfile> getOwnerProfile() {
        CompoundTag tag = this.entityData.get(OWNER_PROFILE_TAG);
        return tag.isEmpty() ? Optional.empty() : Optional.of(NbtUtils.readGameProfile(tag));
    }
    public void setIsNpcClone(boolean isNpc) { this.entityData.set(IS_NPC_CLONE, isNpc); }
    public boolean isNpcClone() { return this.entityData.get(IS_NPC_CLONE); }
}