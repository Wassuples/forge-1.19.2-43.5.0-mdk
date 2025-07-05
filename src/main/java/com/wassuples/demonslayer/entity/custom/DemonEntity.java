package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class DemonEntity extends Monster {
    private static final Logger LOGGER = LogManager.getLogger();
    // Data tracker to sync the demon's rank
    private static final EntityDataAccessor<Integer> DATA_DEMON_RANK =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);

    protected DemonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // --- BASE ATTRIBUTES ---
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    // --- AI GOALS ---
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Targeting AI
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Target any player.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Target any Hashira
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HashiraEntity.class, true));
    }

    // --- CORE DEMON LOGIC ---
    @Override
    public void aiStep() {
        // This makes the demon burn in daylight
        if (this.isAlive() && !this.level.isClientSide) {
            boolean isSunValid = this.level.isDay() && this.level.canSeeSky(this.blockPosition());
            if (isSunValid) {
                this.setSecondsOnFire(8);
            }
        }
        super.aiStep();
    }

    // --- RANK DATA ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DEMON_RANK, DemonRank.FODDER.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("DemonRank", this.getDemonRank().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("DemonRank")) {
            this.setDemonRank(DemonRank.values()[pCompound.getInt("DemonRank")]);
        }
    }

    public DemonRank getDemonRank() {
        return DemonRank.values()[this.entityData.get(DATA_DEMON_RANK)];
    }

    public void setDemonRank(DemonRank rank) {
        this.entityData.set(DATA_DEMON_RANK, rank.ordinal());
    }

    public static <T extends DemonEntity> boolean canSpawn(EntityType<T> pEntityType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        boolean isSolidGround = !(pLevel.getBlockState(pPos.below()).isAir());

        float time = pLevel.getTimeOfDay(1.0F);
        boolean isNightTime = time > 0.25F && time < 0.75F;

        return isSolidGround && isNightTime;
    }
}