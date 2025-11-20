package com.wassuples.demonslayer.entity.custom;

import com.mojang.authlib.GameProfile;
import com.wassuples.demonslayer.entity.ai.DomaCloneAttackGoal;
import com.wassuples.demonslayer.styles.BloodDemonArts;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CrystallineDivineChildEntity extends AbstractFormUsingEntity {
    private static final EntityDataAccessor<CompoundTag> OWNER_PROFILE_TAG =
            SynchedEntityData.defineId(CrystallineDivineChildEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> IS_NPC_CLONE =
            SynchedEntityData.defineId(CrystallineDivineChildEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_BDA_STYLE =
            SynchedEntityData.defineId(CrystallineDivineChildEntity.class, EntityDataSerializers.STRING);


    @Nullable private LivingEntity owner;

    public CrystallineDivineChildEntity(EntityType<? extends CrystallineDivineChildEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setBdaStyle(BloodDemonArts.CRYO);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(1, new DomaCloneAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (entity) -> !isOwner(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public @Nullable BreathingStyles getBreathingStyle() {
        return null;
    }

    @Override
    public @Nullable BloodDemonArts getDemonArt() {
        try {
            BloodDemonArts art = BloodDemonArts.valueOf(this.entityData.get(DATA_BDA_STYLE));
            return art == BloodDemonArts.NONE ? null : art;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setBdaStyle(BloodDemonArts style) {
        this.entityData.set(DATA_BDA_STYLE, style.name());
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

    public LivingEntity getOwner() { return this.owner; }

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
        this.entityData.define(DATA_BDA_STYLE, BloodDemonArts.NONE.name());
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

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }
}