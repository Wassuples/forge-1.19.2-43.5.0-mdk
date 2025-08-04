package com.wassuples.demonslayer.entity.custom.demon;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.ai.FormUserAttackGoal;
import com.wassuples.demonslayer.entity.custom.AbstractFormUsingEntity;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class UpperMoonOneEntity extends AbstractFormUsingEntity {
    private static final EntityDataAccessor<Integer> DATA_DEMON_RANK =
            SynchedEntityData.defineId(UpperMoonOneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEMON_BDA =
            SynchedEntityData.defineId(UpperMoonOneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BREATHING_STYLE =
            SynchedEntityData.defineId(UpperMoonOneEntity.class, EntityDataSerializers.INT);


    public UpperMoonOneEntity(EntityType<? extends AbstractFormUsingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setDemonRank(DemonRank.UPPER_MOON_1);
        this.setDemonArt(BloodDemonArts.CRESCENT);
        this.setBreathingStyle(BreathingStyles.MOON);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DemonEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(1, new FormUserAttackGoal(this, 1.0D, true));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HashiraEntity.class, true));
    }

    @Override
    public void aiStep() {
        if (this.isAlive() && this.level.isDay() && !this.level.isClientSide()) {
            if (this.level.canSeeSky(this.blockPosition())) {
                this.setSecondsOnFire(8);
            }
        }
        super.aiStep();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DEMON_RANK, DemonRank.FODDER.ordinal());
        this.entityData.define(DATA_DEMON_BDA, BloodDemonArts.NONE.ordinal());
        this.entityData.define(DATA_BREATHING_STYLE, BreathingStyles.NONE.ordinal());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("DemonRank", this.getDemonRank().ordinal());
        pCompound.putInt("DemonBDA", this.getDemonArt().ordinal());
        pCompound.putInt("BreathingStyle", this.getBreathingStyle().ordinal());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("DemonRank")) this.setDemonRank(DemonRank.values()[pCompound.getInt("DemonRank")]);
        if (pCompound.contains("DemonBDA")) this.setDemonArt(BloodDemonArts.values()[pCompound.getInt("DemonBDA")]);
        if (pCompound.contains("BreathingStyle")) this.setBreathingStyle(BreathingStyles.values()[pCompound.getInt("BreathingStyle")]);
    }


    public DemonRank getDemonRank() { return DemonRank.values()[this.entityData.get(DATA_DEMON_RANK)]; }
    public void setDemonRank(DemonRank rank) { this.entityData.set(DATA_DEMON_RANK, rank.ordinal()); }
    public void setBreathingStyle(BreathingStyles style) { this.entityData.set(DATA_BREATHING_STYLE, style.ordinal()); }
    public void setDemonArt(BloodDemonArts art) { this.entityData.set(DATA_DEMON_BDA, art.ordinal()); }

    // --- Implement the abstract methods from the parent class ---
    @Nullable @Override
    public BreathingStyles getBreathingStyle() {
        return BreathingStyles.values()[this.entityData.get(DATA_BREATHING_STYLE)];
    }

    @Nullable
    @Override
    public BloodDemonArts getDemonArt() {
        return BloodDemonArts.values()[this.entityData.get(DATA_DEMON_BDA)];
    }
}