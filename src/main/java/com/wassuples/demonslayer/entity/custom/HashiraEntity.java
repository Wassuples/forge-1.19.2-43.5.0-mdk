package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ai.FormUserAttackGoal;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class HashiraEntity extends AbstractFormUsingEntity {
    private static final EntityDataAccessor<Integer> DATA_BREATHING_STYLE =
            SynchedEntityData.defineId(HashiraEntity.class, EntityDataSerializers.INT);

    public HashiraEntity(EntityType<? extends AbstractFormUsingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.8D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BREATHING_STYLE, BreathingStyles.WATER.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("BreathingStyle", this.getBreathingStyle().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if(pCompound.contains("BreathingStyle")) {
            int styleIndex = pCompound.getInt("BreathingStyle");
            if (styleIndex >= 0 && styleIndex < BreathingStyles.values().length) {
                this.setBreathingStyle(BreathingStyles.values()[styleIndex]);
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(1, new FormUserAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Nullable @Override public BreathingStyles getBreathingStyle() {
        return BreathingStyles.values()[this.entityData.get(DATA_BREATHING_STYLE)];
    }

    @Nullable @Override public BloodDemonArts getDemonArt() {
        return null;
    }

    public void setBreathingStyle(BreathingStyles style) {
        this.entityData.set(DATA_BREATHING_STYLE, style.ordinal());
    }
}