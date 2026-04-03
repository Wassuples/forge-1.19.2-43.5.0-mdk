package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.ai.AvoidWisteriaLeavesGoal;
import com.wassuples.demonslayer.entity.ai.FormUserAttackGoal;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.DemonSunlightHelper;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Abstract base class for all NPC demon entities (Fodder → Elite).
 *
 * <p>Extends {@link AbstractFormUsingEntity} with demon-specific data:
 * <ul>
 *   <li>Synched {@link DemonRank} and {@link BloodDemonArts} for client rendering</li>
 *   <li>Night-only spawn rules via {@link #canSpawn}</li>
 *   <li>Wisteria avoidance AI goal</li>
 *   <li>Automatic targeting of players and Hashira NPCs</li>
 * </ul>
 *
 * @see AbstractFormUsingEntity
 * @see com.wassuples.demonslayer.entity.custom.demon.FodderDemonEntity
 */
public abstract class DemonEntity extends AbstractFormUsingEntity {
    private static final EntityDataAccessor<Integer> DATA_DEMON_RANK =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_DEMON_BDA =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_BREATHING_STYLE =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);


    protected DemonEntity(EntityType<? extends AbstractFormUsingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Repelled by wisteria leaves.
        this.goalSelector.addGoal(0, new AvoidWisteriaLeavesGoal(this, 4, 1.25D));

        this.goalSelector.addGoal(1, new FormUserAttackGoal(this, 1.0D, true));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HashiraEntity.class, true));
    }

    @Override
    public void aiStep() {
        DemonSunlightHelper.applySunlightWeakness(this);
        super.aiStep();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DEMON_RANK, DemonRank.FODDER.ordinal());
        this.entityData.define(DATA_DEMON_BDA, BloodDemonArts.NONE.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("DemonRank", this.getDemonRank().ordinal());
        pCompound.putInt("DemonBDA", this.getDemonArt().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("DemonRank")) {
            this.setDemonRank(DemonRank.values()[pCompound.getInt("DemonRank")]);
        }
        if (pCompound.contains("DemonBDA")) {
            this.setDemonArt(BloodDemonArts.values()[pCompound.getInt("DemonBDA")]);
        }
    }

    @Nullable @Override public BreathingStyles getBreathingStyle() {
        return null;
    }

    @Nullable @Override public BloodDemonArts getDemonArt() {
        return BloodDemonArts.values()[this.entityData.get(DATA_DEMON_BDA)];
    }

    public DemonRank getDemonRank() {
        return DemonRank.values()[this.entityData.get(DATA_DEMON_RANK)];
    }

    public void setDemonRank(DemonRank rank) {
        this.entityData.set(DATA_DEMON_RANK, rank.ordinal());
    }

    public void setDemonArt(BloodDemonArts bda) {
        this.entityData.set(DATA_DEMON_BDA, bda.ordinal());
    }

    public static <T extends DemonEntity> boolean canSpawn(EntityType<T> pEntityType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        boolean isSolidGround = !pLevel.getBlockState(pPos.below()).isAir();

        float time = pLevel.getTimeOfDay(1.0F);
        boolean isNightTime = time > 0.25F && time < 0.75F;

        return isSolidGround && isNightTime;
    }
}
