package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * One of the five ores the Ubuyashiki twins lay out on a lacquered tray at the end of the Final Selection. It sits on
 * the plaza in front of them, turning slowly; look at it to read what it is, right-click (or strike) it to choose it
 * for your Nichirin blade. Only the candidate it was laid out for can choose; never saved.
 */
public class OreOfferingEntity extends Entity {
    private static final EntityDataAccessor<Integer> ORE = SynchedEntityData.defineId(OreOfferingEntity.class, EntityDataSerializers.INT);
    private UUID owner;

    public OreOfferingEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public OreOfferingEntity(Level level, ServerPlayer owner, int ore, double x, double y, double z) {
        this(ModEntityTypes.ORE_OFFERING.get(), level);
        this.owner = owner.getUUID();
        this.entityData.set(ORE, ore);
        this.setPos(x, y, z);
    }

    public int ore() {
        return this.entityData.get(ORE);
    }

    public boolean isFor(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ORE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        // the tray is cleared away once its candidate has gone (logged out, or the ceremony ended)
        if (!this.level.isClientSide && this.tickCount % 20 == 0
                && (owner == null || !(this.level.getPlayerByUUID(owner) instanceof ServerPlayer p) || !FinalSelectionMission.isChoosingOre(p))) {
            this.discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level.isClientSide && player instanceof ServerPlayer sp && isFor(sp)) FinalSelectionMission.chooseOre(sp, ore());
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    /** Striking it chooses it too (and never breaks it). */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!this.level.isClientSide && attacker instanceof ServerPlayer sp && isFor(sp)) FinalSelectionMission.chooseOre(sp, ore());
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
