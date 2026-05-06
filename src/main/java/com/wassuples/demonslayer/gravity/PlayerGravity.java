package com.wassuples.demonslayer.gravity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerGravity implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerGravity> GRAVITY_CAPABILITY = CapabilityManager.get(new CapabilityToken<PlayerGravity>() {});
    public static final ResourceLocation GRAVITY_RESOURCE = new ResourceLocation("demonslayer", "gravity");

    private GravityDirection gravity = GravityDirection.DOWN;
    private GravityDirection previousGravity = GravityDirection.DOWN;
    private float transitionProgress = 1.0f;
    private int surfaceDetachTicks = 0;

    public GravityDirection getGravity() {
        return gravity;
    }

    public boolean setGravity(GravityDirection gravity) {
        GravityDirection target = gravity == null ? GravityDirection.DOWN : gravity;
        if (this.gravity != target) {
            this.previousGravity = this.gravity;
            this.gravity = target;
            this.transitionProgress = 0.0f;
            return true;
        }
        return false;
    }

    public void tickInterpolation() {
        if (transitionProgress < 1.0f) {
            transitionProgress += 0.05f; // Adjust speed as needed
            if (transitionProgress > 1.0f) {
                transitionProgress = 1.0f;
            }
        }
    }

    public void tickTransientState() {
        if (surfaceDetachTicks > 0) {
            surfaceDetachTicks--;
        }
    }

    public void beginSurfaceDetach(int ticks) {
        surfaceDetachTicks = Math.max(surfaceDetachTicks, Math.max(0, ticks));
    }

    public boolean isSurfaceDetachActive() {
        return surfaceDetachTicks > 0;
    }

    public float getTransitionProgress() {
        return transitionProgress;
    }

    public GravityDirection getPreviousGravity() {
        return previousGravity;
    }

    public void applySyncedState(GravityDirection gravity, GravityDirection previousGravity, float transitionProgress) {
        this.gravity = gravity == null ? GravityDirection.DOWN : gravity;
        this.previousGravity = previousGravity == null ? this.gravity : previousGravity;
        this.transitionProgress = Math.max(0.0f, Math.min(1.0f, transitionProgress));
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("gravity_direction", gravity.name());
        nbt.putString("previous_gravity", previousGravity.name());
        nbt.putFloat("transition_progress", transitionProgress);
    }

    public void loadNBTData(CompoundTag nbt) {
        try {
            gravity = GravityDirection.valueOf(nbt.getString("gravity_direction"));
            previousGravity = nbt.contains("previous_gravity") ? GravityDirection.valueOf(nbt.getString("previous_gravity")) : gravity;
            transitionProgress = nbt.contains("transition_progress") ? nbt.getFloat("transition_progress") : 1.0f;
        } catch (IllegalArgumentException e) {
            gravity = GravityDirection.DOWN;
            previousGravity = GravityDirection.DOWN;
            transitionProgress = 1.0f;
        }
    }

    public void copyFrom(PlayerGravity source) {
        this.gravity = source.gravity;
        this.previousGravity = source.previousGravity;
        this.transitionProgress = source.transitionProgress;
        this.surfaceDetachTicks = source.surfaceDetachTicks;
    }

    private final LazyOptional<PlayerGravity> optional = LazyOptional.of(() -> this);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if(cap == GRAVITY_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loadNBTData(nbt);
    }
}
