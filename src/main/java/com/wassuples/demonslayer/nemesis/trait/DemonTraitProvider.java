package com.wassuples.demonslayer.nemesis.trait;

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
 * Forge capability provider for {@link IDemonTrait}.
 *
 * <p>Attached to {@link com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity} instances
 * to store their unique identity, personality, and combat profile.
 *
 * @see IDemonTrait
 * @see DemonTrait
 */
public class DemonTraitProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<IDemonTrait> DEMON_TRAIT =
            CapabilityManager.get(new CapabilityToken<>() {});

    private DemonTrait trait = null;
    private final LazyOptional<IDemonTrait> optional = LazyOptional.of(this::createDemonTrait);

    private IDemonTrait createDemonTrait() {
        if (this.trait == null) {
            this.trait = new DemonTrait();
        }
        return this.trait;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == DEMON_TRAIT ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createDemonTrait().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createDemonTrait().deserializeNBT(nbt);
    }
}
