package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ai.HashiraAttackGoal;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
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

public class HashiraEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_BREATHING_STYLE =
            SynchedEntityData.defineId(HashiraEntity.class, EntityDataSerializers.INT);

    public HashiraEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // This is where we define the base stats for the Hashira.
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D) // Very high health
                .add(Attributes.MOVEMENT_SPEED, 0.8D) // Faster than a normal mob
                .add(Attributes.ATTACK_DAMAGE, 10.0D) // Strong base attack
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D); // Natural armor
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

    public BreathingStyles getBreathingStyle() {
        return BreathingStyles.values()[this.entityData.get(DATA_BREATHING_STYLE)];
    }

    public void setBreathingStyle(BreathingStyles style) {
        this.entityData.set(DATA_BREATHING_STYLE, style.ordinal());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Non-combat AI (lowest priority)
        this.goalSelector.addGoal(0, new FloatGoal(this)); // Swim if in water
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D)); // Walk around
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F)); // Look at nearby players
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this)); // Look around when idle

        // Combat AI
        this.goalSelector.addGoal(1, new HashiraAttackGoal(this, 1.0D, true)); // Our custom attack goal

        // Targeting AI (what to attack)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // Attack whatever hurt it
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide()) {
            FormUtils.ActiveForm activeForm = FormUtils.ActiveFormManager.getActiveForm(this);
            if (activeForm != null) {
                activeForm.tick();
            }
        }
    }
}
