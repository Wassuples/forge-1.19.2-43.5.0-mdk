package com.wassuples.demonslayer.nemesis.ai;

import com.wassuples.demonslayer.demon.ai.DemonGenerator;
import com.wassuples.demonslayer.entity.ai.AvoidWisteriaLeavesGoal;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.util.DemonSunlightHelper;
import com.wassuples.demonslayer.nemesis.ai.goals.NemesisAttackGoal;
import com.wassuples.demonslayer.nemesis.ai.goals.StayInBiomeGoal;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.IDemonTrait;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.UUID;

/**
 * The living entity representing a named Nemesis demon in the world.
 *
 * <p>Unlike regular {@link com.wassuples.demonslayer.entity.custom.DemonEntity} mobs,
 * a Nemesis demon is a unique, persistent character managed by the
 * {@link com.wassuples.demonslayer.nemesis.DemonHierarchyManager}. It has:
 * <ul>
 *   <li>A {@link com.wassuples.demonslayer.nemesis.trait.DemonTrait} capability with personality, memories, and quirks</li>
 *   <li>Dynamically scaled attributes based on rank, growth, and Blood Points</li>
 *   <li>Behavioral AI that adapts based on {@link Personality} and {@link Behavior}</li>
 *   <li>Taunt system — delivers context-aware dialogue during combat</li>
 * </ul>
 *
 * @see com.wassuples.demonslayer.nemesis.DemonHierarchyManager
 * @see com.wassuples.demonslayer.nemesis.trait.DemonTrait
 * @see Personality
 */

/**
 * Nemesis Demon Entity.
 */
public class NemesisDemonEntity extends Monster {
    private static final UUID AGGRESSIVE_FOLLOW_RANGE_MODIFIER_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");

    /**
     * Synched data accessor for the source blueprint ID. An empty string means no blueprint (procedural demon).
     * This is synced to the client so the renderer can pick the correct skin texture.
     */
    private static final EntityDataAccessor<String> DATA_BLUEPRINT_ID =
            SynchedEntityData.defineId(NemesisDemonEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_SKIN_LOCATION =
            SynchedEntityData.defineId(NemesisDemonEntity.class, EntityDataSerializers.STRING);

    public NemesisDemonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BLUEPRINT_ID, "");
        this.entityData.define(DATA_SKIN_LOCATION, "");
    }

    public String getBlueprintId() {
        return this.entityData.get(DATA_BLUEPRINT_ID);
    }

    public void setBlueprintId(String id) {
        this.entityData.set(DATA_BLUEPRINT_ID, id != null ? id : "");
    }

    public String getSkinLocation() {
        return this.entityData.get(DATA_SKIN_LOCATION);
    }

    public void setSkinLocation(String skinLocation) {
        this.entityData.set(DATA_SKIN_LOCATION, skinLocation != null ? skinLocation : "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        String bpId = this.getBlueprintId();
        if (!bpId.isEmpty()) {
            tag.putString("BlueprintId", bpId);
        }

        String skinLocation = this.getSkinLocation();
        if (!skinLocation.isEmpty()) {
            tag.putString("SkinLocation", skinLocation);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BlueprintId")) {
            this.setBlueprintId(tag.getString("BlueprintId"));
        }
        if (tag.contains("SkinLocation")) {
            this.setSkinLocation(tag.getString("SkinLocation"));
        }
    }

    @Override
    protected void registerGoals() {
        // Add a baseline, default AI.
        // This will be OVERWRITTEN by applyBehavioralAI when the trait is applied,
        // but it ensures the entity initializes correctly.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidWisteriaLeavesGoal(this, 4, 1.25D));
        this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.4D, true, 0.75f));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void applyTraitData(IDemonTrait trait) {
        // TODO make custom name be top and title be below it somehow, as a subtitle or something
        this.setCustomName(Component.literal(trait.getName() + " " + trait.getTitle()));
        this.setCustomNameVisible(true);

        // Sync the blueprint ID to the client for skin selection
        this.setBlueprintId(trait.getSourceBlueprintId());
        this.setSkinLocation(trait.getSkinLocation());

        this.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(cap -> {
            cap.copyFrom(trait);
        });

        var healthAttribute = this.getAttribute(Attributes.MAX_HEALTH);
        var speedAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        var damageAttribute = this.getAttribute(Attributes.ATTACK_DAMAGE);


        double healthMultiplier = 1.0D;
        double damageMultiplier = 1.0D;
        double speedMultiplier = 1.0D;
        double followRangeBonus = 0.0D;
        double knockbackResistanceBonus = 0.0D;

        for (Quirk quirk : trait.getQuirks()) {
            switch (quirk) {
                case HEAVY_HITTER -> damageMultiplier *= 1.12D;
                case BLOOD_SIPHON -> healthMultiplier *= 1.05D;
                case SHOCKWAVE_STOMP -> damageMultiplier *= 1.08D;
                case VENOMOUS -> damageMultiplier *= 1.06D;
                case EXECUTIONER -> damageMultiplier *= 1.05D;
                case FIRE_RESISTANT -> healthMultiplier *= 1.06D;
                case FROST_BLOODED -> healthMultiplier *= 1.04D;
                case ARROW_PROOF -> healthMultiplier *= 1.05D;
                case TENACIOUS -> healthMultiplier *= 1.08D;
                case UNSHAKABLE -> {
                    healthMultiplier *= 1.05D;
                    knockbackResistanceBonus += 0.30D;
                }
                case RAPID_RECOVERY -> healthMultiplier *= 1.04D;
                case DEATH_DEFIANT -> healthMultiplier *= 1.10D;
                case NIMBLE -> speedMultiplier *= 1.12D;
                case NIGHT_STALKER -> speedMultiplier *= 1.05D;
                case KEEN_Scent -> followRangeBonus += 10.0D;
                case PACK_CALLER -> healthMultiplier *= 1.04D;
                case FEAR_OF_WATER -> speedMultiplier *= 0.94D;
                case BRITTLE -> healthMultiplier *= 0.90D;
                case SUNLIGHT_SICKNESS -> healthMultiplier *= 0.92D;
                case FLAMMABLE_FLESH -> healthMultiplier *= 0.90D;
                case PURIFIED_WEAKNESS -> healthMultiplier *= 0.94D;
                case OVERCONFIDENT -> speedMultiplier *= 0.95D;
                case GLASS_CANNON -> {
                    damageMultiplier *= 1.15D;
                    healthMultiplier *= 0.78D;
                }
                case WIND_UP -> {
                    damageMultiplier *= 1.06D;
                    speedMultiplier *= 0.92D;
                }
                case BLIND_RAGE -> speedMultiplier *= 0.95D;
                case OBSESSIVE_HUNTER -> speedMultiplier *= 0.97D;
                case SOUND_SENSITIVE -> speedMultiplier *= 0.92D;
                case MAGPIE -> speedMultiplier *= 0.95D;
                case EASILY_TAUNTED -> speedMultiplier *= 0.94D;
                case DISHONORED -> {
                    healthMultiplier *= 0.92D;
                    damageMultiplier *= 0.95D;
                }
            }
        }

        if (healthAttribute != null) {
            // 10 base health + 5 health per vigor point, may change in future
            double newHealth = (10.0 + (trait.getVigor() * 5.0)) * healthMultiplier;
            healthAttribute.setBaseValue(newHealth);
            this.setHealth(this.getMaxHealth());
        }
        if (speedAttribute != null) {
            // 0.23 base speed + a small bonus from agility
            double newSpeed = (0.23 + (trait.getAgility() * 0.005)) * speedMultiplier;
            speedAttribute.setBaseValue(newSpeed);
        }
        if (damageAttribute != null) {
            // 2 base damage + 1 damage per strength point
            double newDamage = (2.0 + trait.getStrength()) * damageMultiplier;
            damageAttribute.setBaseValue(newDamage);
        }
        var followRangeAttribute = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRangeAttribute != null && followRangeBonus > 0.0D) {
            followRangeAttribute.setBaseValue(followRangeAttribute.getBaseValue() + followRangeBonus);
        }
        var knockbackAttribute = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute != null && knockbackResistanceBonus > 0.0D) {
            knockbackAttribute.setBaseValue(Math.min(1.0D, knockbackAttribute.getBaseValue() + knockbackResistanceBonus));
        }

        // This call is correct. It will clear the default goals
        // from registerGoals() and add the trait-specific ones.
        applyBehavioralAI(trait);
    }

    private void applyBehavioralAI(IDemonTrait trait) {
        if (this.level.isClientSide) return;

        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();

        // Global Goals (applied to almost all behaviors)
        this.goalSelector.addGoal(0, new FloatGoal(this)); // Always float in water
        this.goalSelector.addGoal(1, new AvoidWisteriaLeavesGoal(this, 4, 1.25D)); // Keep distance from wisteria leaves
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this)); // Look around when idle

        // Behavior-Specific Goals
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.removeModifier(AGGRESSIVE_FOLLOW_RANGE_MODIFIER_ID);
        }
        BlockPos homePos = trait.getHomePosition();

        // TODO Add more global goals like looking at players, reacting to being hurt, etc.
        switch (trait.getBehavior()) {
            case AGGRESSIVE:
            case BERSERKER:
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.4D, true, trait.getBehavior() == Behavior.BERSERKER ? 0.6f : 0.75f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                // Aggressive demons hunt from further away.
                if (followRange != null) {
                    followRange.addPermanentModifier(new AttributeModifier(AGGRESSIVE_FOLLOW_RANGE_MODIFIER_ID, "Aggressive follow range", 20.0, AttributeModifier.Operation.ADDITION));
                }
                break;

            case DUELIST:
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.2D, true, 1.0f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                // Duelists are honorable and will focus on one target.
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;

            case STALWART:
                // Slower, more deliberate approach.
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.0D, true, 1.2f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;

            case BODYGUARD:
            case LOYALIST:
                // TODO: Requires custom goals to find and follow/protect a "master" or ally demon.
                // As a placeholder, they will attack anyone who hurts them or other nearby monsters.
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.2D, true, 1.0f));
                this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0D));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Monster.class)); // Key goal
                break;

            case TERRITORIAL:
                // Becomes more aggressive in its territory
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.3D, true, 0.9f));
                // Add the new goal to retreat back to its home biome if it strays too far
                this.goalSelector.addGoal(3, new StayInBiomeGoal(this, 1.2D));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                if (homePos != null) {
                    this.restrictTo(homePos, 128);
                }
                break;

            case CALM:
                // Only fights if provoked.
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.2D, true, 1.0f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // Will not seek targets on its own.
                break;

            case COWARDLY:
                // Goal to flee when at low health.
                this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.6D, 1.4D, (entity) -> this.getHealth() / this.getMaxHealth() < 0.4f));
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.1D, true, 1.5f)); // Hesitant attacks
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;

            case STRATEGIST:
            case TRICKSTER:
            case OVERLORD:
            case SADIST:
                // These behaviors are primarily expressed through the getUsagePriority logic.
                // Their movement AI is standard, allowing their form choices to define them.
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.2D, true, 1.0f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;

            case SKIRMISHER:
                // Tries to maintain distance. Flees if the player gets too close.
                this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.3D, 1.2D));
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.3D, true, 1.1f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;

            case SCAVENGER:
                // Flees from healthy players.
                this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.4D, 1.3D,
                        (entity) -> entity instanceof Player && ((Player) entity).getHealth() / ((Player) entity).getMaxHealth() > 0.7f));
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.3D, true, 0.8f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                // Only targets players who are already wounded.
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                        (entity) -> entity.getHealth() / entity.getMaxHealth() < 0.7f));
                break;

            case AMBITIOUS:
                // TODO: Requires custom goal to check player rank/gear to determine if they are a "worthy" target.
                // For now, it will behave like a standard AGGRESSIVE demon.
                this.goalSelector.addGoal(2, new NemesisAttackGoal(this, 1.3D, true, 0.9f));
                this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
                this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
                break;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
        // FOLLOW_RANGE is included in Monster.createMonsterAttributes() by default
    }

    @Nullable @Override public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor pLevel, @Nonnull DifficultyInstance pDifficulty,
                                                            @Nonnull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData,
                                                            @Nullable CompoundTag pDataTag) {

        boolean isWorldSpawn = pReason == MobSpawnType.NATURAL || pReason == MobSpawnType.CHUNK_GENERATION;
        ServerLevel serverLevel = pLevel instanceof ServerLevel level ? level : null;

        if (isWorldSpawn && serverLevel != null) {
            DemonGenerator.initializeProcedural(this, serverLevel);
        }

        this.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(this::applyTraitData);

        if (isWorldSpawn && serverLevel != null) {
            DemonHierarchyManager manager = DemonHierarchyManager.get(serverLevel);
            UUID demonId = this.getUUID();
            if (manager.getDemonTrait(demonId) == null) {
                this.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(traitCap -> {
                    DemonTrait snapshot = new DemonTrait();
                    snapshot.deserializeNBT(traitCap.serializeNBT());
                    manager.addDemon(demonId, snapshot);
                });
            }
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    // TODO Entity is silent, we need to override getAmbientSound, getHurtSound, getDeathSound, etc. to give it a voice. Each personality should have a different sound set.
    // TODO Implement demon-specific loot tables

    @Override
    public void tick() {
        super.tick();
        // Update lastSeenTime on the hierarchy cache every ~10 seconds (C3)
        if (!this.level.isClientSide && this.isAlive() && this.tickCount % 200 == 0) {
            if (this.level instanceof ServerLevel serverLevel) {
                com.wassuples.demonslayer.nemesis.DemonHierarchyManager manager =
                        com.wassuples.demonslayer.nemesis.DemonHierarchyManager.get(serverLevel);
                com.wassuples.demonslayer.nemesis.trait.DemonTrait trait = manager.getDemonTrait(this.getUUID());
                if (trait != null) {
                    trait.setLastSeenTime(serverLevel.getGameTime());
                }
            }
        }
    }

    @Override
    public void aiStep() {
        DemonSunlightHelper.applySunlightWeakness(this);
        if (!this.level.isClientSide && this.tickCount % 20 == 0) {
            this.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(this::applyQuirkRuntimeEffects);
        }
        super.aiStep();
    }

    private void applyQuirkRuntimeEffects(IDemonTrait trait) {
        boolean isNight = this.level.getDayTime() % 24000L >= 13000L;

        if (trait.hasQuirk(Quirk.NIGHT_STALKER) && isNight) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
        }
        if (trait.hasQuirk(Quirk.TENACIOUS) && this.getHealth() / Math.max(1.0F, this.getMaxHealth()) < 0.35F) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false));
        }
        if (trait.hasQuirk(Quirk.BLIND_RAGE) && this.getHealth() / Math.max(1.0F, this.getMaxHealth()) < 0.35F) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, true, false));
        }

        if (trait.hasQuirk(Quirk.FEAR_OF_WATER) && this.isInWaterRainOrBubble()) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false));
        }

        if (trait.hasQuirk(Quirk.RAPID_RECOVERY) && this.tickCount % 40 == 0) {
            this.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::getEffect)
                    .findFirst()
                    .ifPresent(this::removeEffect);
        }

        if (trait.hasQuirk(Quirk.MAGPIE) && this.getTarget() != null && this.tickCount % 40 == 0) {
            this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(5.0D))
                    .stream()
                    .min(Comparator.comparingDouble(item -> item.distanceToSqr(this)))
                    .ifPresent(item -> {
                        this.setTarget(null);
                        this.getNavigation().moveTo(item, 1.1D);
                    });
        }

        if (!trait.hasQuirk(Quirk.DEATH_DEFIANT) || this.getTarget() != null) {
            return;
        }
        if (this.tickCount % 40 == 0 && this.getHealth() > this.getMaxHealth() * 0.75F) {
            this.getPersistentData().remove("nemesis_death_defiant_used");
        }
    }
}
