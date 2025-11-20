package com.wassuples.demonslayer.crow;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerCrowData {
    private UUID crowUuid;
    private boolean needsRespawn;
    private ResourceKey<Level> ownerDimension;

    public boolean hasCrow() {
        return crowUuid != null;
    }

    public void setCrowUuid(@Nullable UUID uuid) {
        this.crowUuid = uuid;
    }

    @Nullable
    public UUID getCrowUuid() {
        return crowUuid;
    }

    public boolean needsRespawn() {
        return needsRespawn;
    }

    public void setNeedsRespawn(boolean needsRespawn) {
        this.needsRespawn = needsRespawn;
    }

    public void saveNBTData(CompoundTag tag) {
        if (crowUuid != null) {
            tag.putUUID("CrowUUID", crowUuid);
        }
        tag.putBoolean("CrowNeedsRespawn", needsRespawn);
        if (ownerDimension != null) {
            tag.putString("CrowDimension", ownerDimension.location().toString());
        }
    }

    public void loadNBTData(CompoundTag tag) {
        if (tag.hasUUID("CrowUUID")) {
            crowUuid = tag.getUUID("CrowUUID");
        } else {
            crowUuid = null;
        }
        needsRespawn = tag.getBoolean("CrowNeedsRespawn");
        if (tag.contains("CrowDimension")) {
            ownerDimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString("CrowDimension")));
        } else {
            ownerDimension = null;
        }
    }

    public ResourceKey<Level> getOwnerDimension() {
        return ownerDimension;
    }

    public void setOwnerDimension(@Nullable ResourceKey<Level> dimension) {
        this.ownerDimension = dimension;
    }

    public void copyFrom(PlayerCrowData other) {
        this.crowUuid = other.crowUuid;
        this.needsRespawn = other.needsRespawn;
        this.ownerDimension = other.ownerDimension;
    }
}
