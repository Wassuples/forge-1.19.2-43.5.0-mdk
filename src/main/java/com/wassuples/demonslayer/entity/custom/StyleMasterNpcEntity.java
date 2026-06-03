package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Base entity for all non-Hashira style masters (Urokodaki, Shinjuro, Jigoro, Tengen-retired).
 *
 * <p>Style masters are peaceful NPCs that stand in fixed locations. They cannot attack,
 * do not target demons, and look at nearby players. Interaction (right-click) is handled
 * by {@link com.wassuples.demonslayer.events.StyleTeachingEvents}.
 *
 * <p>Each subclass stores a fixed {@link #masterId} string that matches a key in
 * {@link com.wassuples.demonslayer.training.StyleMasterRegistry}.
 */
public abstract class StyleMasterNpcEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_MASTER_ID =
            SynchedEntityData.defineId(StyleMasterNpcEntity.class, EntityDataSerializers.STRING);

    protected StyleMasterNpcEntity(EntityType<? extends StyleMasterNpcEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)  // Fixed — they don't wander
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MASTER_ID, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** Stable registry key matching {@link com.wassuples.demonslayer.training.StyleMasterRegistry}. */
    public String getMasterId() {
        return this.entityData.get(DATA_MASTER_ID);
    }

    protected void setMasterId(String id) {
        this.entityData.set(DATA_MASTER_ID, id);
    }

    /** Style this master teaches — drives interaction eligibility checks. */
    public abstract BreathingStyles getMasterStyle();

    @Override
    public boolean canBeLeashed(Player player) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("masterId", getMasterId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("masterId")) setMasterId(tag.getString("masterId"));
    }
}
