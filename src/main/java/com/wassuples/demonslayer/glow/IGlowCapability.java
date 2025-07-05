package com.wassuples.demonslayer.glow;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public interface IGlowCapability extends INBTSerializable<CompoundTag> {
    String getGlowColor();
    void setGlowColor(String color);

    UUID getExclusiveViewer();
    void setExclusiveViewer(UUID player);
    boolean hasExclusiveViewer();

    boolean isGlowing();
    void setGlowing(boolean glowing);

    void clear();
}
