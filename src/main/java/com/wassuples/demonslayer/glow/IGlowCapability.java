package com.wassuples.demonslayer.glow;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

/**
 * Capability interface for per-entity glow rendering data.
 *
 * <p>Used by the Akaza "Compass Needle" form and other abilities that make entities glow
 * with a specific color, optionally visible to only one player (the "exclusive viewer").
 *
 * @see GlowCapability
 * @see GlowCapabilityProvider
 * @see com.wassuples.demonslayer.events.GlowRenderHandler
 */
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
