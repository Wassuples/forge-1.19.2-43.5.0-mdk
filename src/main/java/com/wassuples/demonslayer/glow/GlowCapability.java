package com.wassuples.demonslayer.glow;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Default implementation of {@link IGlowCapability}.
 *
 * <p>Stores a hex color string, an optional exclusive viewer UUID, and a glowing flag.
 * Persisted via NBT serialization.
 */
public class GlowCapability implements IGlowCapability {
    private String glowColor = "";
    private UUID exclusiveViewer = null;
    private boolean isGlowing = false;

    @Override
    public String getGlowColor() {
        return glowColor;
    }

    @Override
    public void setGlowColor(String color) {
        this.glowColor = color;
    }

    @Override
    public UUID getExclusiveViewer() {
        return exclusiveViewer;
    }

    @Override
    public void setExclusiveViewer(UUID player) {
        this.exclusiveViewer = player;
    }

    @Override
    public boolean hasExclusiveViewer() {
        return this.exclusiveViewer != null;
    }

    @Override
    public boolean isGlowing() {
        return isGlowing;
    }

    @Override
    public void setGlowing(boolean glowing) {
        isGlowing = glowing;
    }

    @Override
    public void clear() {
        this.glowColor = "";
        this.exclusiveViewer = null;
        this.isGlowing = false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("glowColor", glowColor);
        if (exclusiveViewer != null) {
            nbt.putUUID("exclusiveViewer", exclusiveViewer);
        }
        nbt.putBoolean("isGlowing", isGlowing);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        glowColor = nbt.getString("glowColor");
        if (nbt.hasUUID("exclusiveViewer")) {
            exclusiveViewer = nbt.getUUID("exclusiveViewer");
        } else {
            exclusiveViewer = null;
        }
        isGlowing = nbt.getBoolean("isGlowing");
    }
}
