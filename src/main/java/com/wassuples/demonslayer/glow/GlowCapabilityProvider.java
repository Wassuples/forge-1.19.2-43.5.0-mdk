package com.wassuples.demonslayer.glow;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forge capability provider for {@link IGlowCapability}.
 *
 * <p>Attached to living entities to enable per-entity colored glow effects.
 * Retrieve via:
 * <pre>{@code
 * entity.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(glow -> { ... });
 * }</pre>
 */
public class GlowCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<IGlowCapability> GLOW_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private IGlowCapability glow = null;
    private final LazyOptional<IGlowCapability> optional = LazyOptional.of(this::createGlowCapability);

    private IGlowCapability createGlowCapability() {
        if (this.glow == null) {
            this.glow = new GlowCapability();
        }
        return this.glow;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == GLOW_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createGlowCapability().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createGlowCapability().deserializeNBT(nbt);
    }
}
