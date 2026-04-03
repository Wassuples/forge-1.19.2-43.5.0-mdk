package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.crow.CrowManager;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.OpenCrowDispatchS2CPacket;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
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
import java.util.*;

/**
 * Custom entity: Crow.
 */
public class CrowEntity extends TamableAnimal implements IAnimatable {
    private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ALERTED = SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double CRUISE_HEIGHT = 4.0D;
    private static final double FOLLOW_RADIUS = 6.0D;
    private static final double MIN_DISTANCE = 2.0D;
    private final AnimationFactory factory = new AnimationFactory(this);

    public CrowEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.navigation = new FlyingPathNavigation(this, level);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.COCOA, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 3.0D);
    }

    public static boolean checkCrowSpawnRules(EntityType<CrowEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                && level.getBrightness(LightLayer.SKY, pos) > 8;
    }

    public static CrowEntity spawnFor(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        CrowEntity crow = ModEntityTypes.CROW.get().create(level);
        if (crow == null) return null;
        Vec3 spawn = getHoverPosition(player);
        crow.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0);
        crow.bindToOwner(player);
        level.addFreshEntity(crow);
        return crow;
    }

    private static Vec3 getHoverPosition(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() == 0) {
            look = new Vec3(0, 0, 1);
        }
        Vec3 offset = look.normalize().scale(-FOLLOW_RADIUS).add(0, CRUISE_HEIGHT, 0);
        return player.position().add(offset);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new CrowPerchGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        navigation.setCanOpenDoors(false);
        return navigation;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntityTypes.CROW.get().create(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PERCHED, false);
        this.entityData.define(DATA_ALERTED, false);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.getOwnerUUID() == null) {
            if (!player.level.isClientSide) {
                bindToOwner(player);
                player.displayClientMessage(Component.literal("The crow recognises you."), true);
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        }

        if (!player.getUUID().equals(this.getOwnerUUID())) {
            if (!player.level.isClientSide) {
                player.displayClientMessage(Component.literal("This crow already serves another slayer."), true);
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        }

        if (player.isCrouching()) {
            if (!player.level.isClientSide) {
                boolean newPerch = !this.isPerched();
                this.setPerched(newPerch);
                if (newPerch) {
                    this.setDeltaMovement(Vec3.ZERO);
                    player.displayClientMessage(Component.literal("The crow settles down."), true);
                } else {
                    player.displayClientMessage(Component.literal("The crow takes to the air."), true);
                }
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        } else {
            if (!player.level.isClientSide) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                DemonHierarchyManager hierarchyManager = DemonHierarchyManager.get(serverPlayer.getLevel());
                List<DemonDisplayData> kizuki = hierarchyManager.getDemonDisplayData().kizuki();
                List<DemonDisplayData> fodder = hierarchyManager.getDemonDisplayData().fodder();
                List<Mission> active = new ArrayList<>();
                List<Mission> available = new ArrayList<>();
                List<Mission> completed = new ArrayList<>();
                // Populate mission lists from player data
                serverPlayer.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(playerMissions -> {
                    List<Mission> allMissions = MissionRegistry.getAllMissions();
                    for (Mission mission : allMissions) {
                        if (playerMissions.isActive(mission.id())) {
                            active.add(mission);
                        } else if (playerMissions.isCompleted(mission.id())) {
                            completed.add(mission);
                        } else {
                            serverPlayer.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                                if (progression.getRank().ordinal() >= mission.requirement().ordinal()) {
                                    available.add(mission);
                                }
                            });
                        }
                    }
                });
                ModMessages.sendToPlayer(new OpenCrowDispatchS2CPacket(kizuki, fodder, active, available, completed), serverPlayer);
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        }
    }

    public void bindToOwner(@Nullable Player player) {
        if (player != null) {
            this.tame(player);
        } else {
            this.setOwnerUUID(null);
            this.setTame(false);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Perched", this.isPerched());
        tag.putBoolean("Alerted", this.isAlerted());
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPerched(tag.getBoolean("Perched"));
        this.setAlerted(tag.getBoolean("Alerted"));
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
            this.setTame(true);
        }
    }

    public boolean isPerched() {
        return this.entityData.get(DATA_PERCHED);
    }

    public void setPerched(boolean perched) {
        this.entityData.set(DATA_PERCHED, perched);
    }

    public boolean isAlerted() {
        return this.entityData.get(DATA_ALERTED);
    }

    public void setAlerted(boolean alerted) {
        this.entityData.set(DATA_ALERTED, alerted);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    public boolean isFlying() {
        return !this.isOnGround() && !this.isPerched();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!this.level.isClientSide && this.isAlive()) {
            this.setPerched(false);
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level.isClientSide) {
            CrowManager.handleCrowDeath(this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide()) {
            return;
        }

        LivingEntity owner = this.getOwner();
        if (owner == null) {
            if (this.getOwnerUUID() != null && this.tickCount > 200) {
                this.bindToOwner(null);
            }
            return;
        }

        if (this.isPerched()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        double distance = this.distanceTo(owner);
        if (distance <= MIN_DISTANCE) {
            this.setPerched(true);
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 desired = owner.position().add(0, CRUISE_HEIGHT, 0).subtract(this.position());
            Vec3 velocity = desired.normalize().scale(0.05D);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.92D).add(velocity));
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (this.isPerched()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("perched", true));
        } else if (this.isFlying()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("fly", true));
        } else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        }
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

    static class CrowPerchGoal extends MoveToBlockGoal {
        private final CrowEntity crow;

        CrowPerchGoal(CrowEntity crow, double speedModifier) {
            super(crow, speedModifier, 8, 4);
            this.crow = crow;
        }

        @Override
        public boolean canUse() {
            return !crow.isAlerted() && crow.getOwnerUUID() == null && !crow.isPerched() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !crow.isAlerted() && crow.getOwnerUUID() == null && !crow.isPerched() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            if (this.isReachedTarget()) {
                crow.setPerched(true);
                crow.setDeltaMovement(Vec3.ZERO);
                crow.getNavigation().stop();
            }
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            BlockState blockstate = level.getBlockState(pos);
            return crow.getOwnerUUID() == null && blockstate.isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below());
        }
    }

    private static class CrowMenuProvider implements MenuProvider {
        private final CrowEntity crow;

        private CrowMenuProvider(CrowEntity crow) {
            this.crow = crow;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Kasugai Crow");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new AbstractContainerMenu(null, id) {
                @Override
                public ItemStack quickMoveStack(Player player, int i) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(Player player) {
                    return crow.isAlive() && crow.distanceTo(player) < 32;
                }
            };
        }
    }
}
