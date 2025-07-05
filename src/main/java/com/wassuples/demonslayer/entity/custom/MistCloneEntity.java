package com.wassuples.demonslayer.entity.custom;

import com.mojang.authlib.GameProfile;
import com.wassuples.demonslayer.styles.BreathingStyles;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MistCloneEntity extends Monster {
    private int lifeTicks = 0;

    // This synced data will hold the owner's profile to get the skin on the client
    private static final EntityDataAccessor<CompoundTag> OWNER_PROFILE_TAG =
            SynchedEntityData.defineId(MistCloneEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final EntityDataAccessor<Boolean> IS_NPC_CLONE =
            SynchedEntityData.defineId(MistCloneEntity.class, EntityDataSerializers.BOOLEAN);

    public MistCloneEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setSilent(true);
    }

    // Register the entity's attributes, like health.
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D); // It doesn't move
    }

    // --- Profile and Skin Logic ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_PROFILE_TAG, new CompoundTag());
        this.entityData.define(IS_NPC_CLONE, false);
    }

    public void setOwnerProfile(GameProfile profile) {
        CompoundTag tag = new CompoundTag();
        NbtUtils.writeGameProfile(tag, profile);
        this.entityData.set(OWNER_PROFILE_TAG, tag);
    }

    public void setIsNpcClone(boolean isNpc) {
        this.entityData.set(IS_NPC_CLONE, isNpc);
    }

    public boolean isNpcClone() {
        return this.entityData.get(IS_NPC_CLONE);
    }

    public Optional<GameProfile> getOwnerProfile() {
        CompoundTag tag = this.entityData.get(OWNER_PROFILE_TAG);
        // If the tag is empty, return nothing. Otherwise, read the profile from it.
        return tag.isEmpty() ? Optional.empty() : Optional.of(NbtUtils.readGameProfile(tag));
    }

    @Nullable
    public LivingEntity getOwner() {
        // Get the profile from our synced data
        Optional<GameProfile> profile = getOwnerProfile();
        if (profile.isPresent() && !this.level.isClientSide) {
            // Get the UUID from the profile and find the entity in the server world
            return this.level.getPlayerByUUID(profile.get().getId());
        }
        return null;
    }

    // --- Overriding Behavior ---
    // This is what happens when the clone is "killed" (hit once).
    @Override
    public void die(DamageSource pDamageSource) {
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(),
                    50, 0.5, 0.5, 0.5, 0.1);
            this.level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
        // Remove the entity from the world without a death animation or drops.
        this.discard();
    }

    // Prevent standard monster despawning
    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void tick() {
        super.tick(); // Handle basic entity updates

        this.lifeTicks++;
        // The mist lasts for 200 ticks (10 seconds). We'll make the clone do the same.
        if (this.lifeTicks > 200) {
            // We call our existing die() method to trigger the "puff of smoke" effect.
            this.die(DamageSource.MAGIC);
            return; // Stop any further processing for this tick
        }

        // We only want the server to run this logic.
        if (!this.level.isClientSide) {
            LivingEntity finalTarget = null;
            AABB searchBox = this.getBoundingBox().inflate(16.0);

            // --- Step 1: Prioritize Players ---
            // First, search for players who are not the owner and are in survival/adventure mode.
            List<Player> nearbyPlayers = this.level.getEntitiesOfClass(Player.class, searchBox,
                    player -> player.isAlive() && !player.is(this.getOwner()) && !player.isCreative() && !player.isSpectator()
            );

            if (!nearbyPlayers.isEmpty()) {
                // If we found at least one player, find the closest one among them.
                finalTarget = nearbyPlayers.stream()
                        .min(Comparator.comparingDouble(p -> p.distanceToSqr(this)))
                        .orElse(null);
            }

            // --- Step 2: If no player was found, fallback to other entities ---
            if (finalTarget == null) {
                // Search for any other LivingEntity that isn't a player or another clone.
                List<LivingEntity> nearbyMobs = this.level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        entity -> entity.isAlive() && entity != this && !(entity instanceof Player) && !entity.getTags().contains("mist_clone")
                );

                if (!nearbyMobs.isEmpty()) {
                    // If we found other mobs, find the closest one.
                    finalTarget = nearbyMobs.stream()
                            .min(Comparator.comparingDouble(m -> m.distanceToSqr(this)))
                            .orElse(null);
                }
            }

            // --- Step 3: Set the Look Target ---
            if (finalTarget != null) {
                // If we found any valid target (player or mob), look at it.
                this.getLookControl().setLookAt(finalTarget, 10.0F, 10.0F);
            }
        }
    }

    @Override
    protected void registerGoals() {
        // We leave this empty on purpose.
        // This gives the entity an active AI, but no goals to execute, so it will stand still.
        // This allows its LookControl to still function.
    }
}