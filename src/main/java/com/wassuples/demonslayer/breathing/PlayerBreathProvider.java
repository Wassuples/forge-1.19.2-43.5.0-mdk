package com.wassuples.demonslayer.breathing;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability provider for {@link PlayerBreath}.
 *
 * <p>This provider is attached to player entities and is responsible for:
 *
 * <ul>
 *   <li>Lazy creation of the {@link PlayerBreath} instance</li>
 *   <li>Exposing the capability via Forge's {@link ICapabilityProvider}</li>
 *   <li>Persisting the breath data via NBT</li>
 * </ul>
 */
public class PlayerBreathProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = new ResourceLocation(DemonSlayer.MOD_ID, "properties");

    /** The capability token used to retrieve {@link PlayerBreath} instances from entities. */
    public static final Capability<PlayerBreath> PLAYER_BREATH =
            CapabilityManager.get(new CapabilityToken<PlayerBreath>() { });

    private PlayerBreath breath = null;
    private final LazyOptional<PlayerBreath> optional = LazyOptional.of(this::createPlayerBreath);

    /** Lazily constructs the underlying {@link PlayerBreath} instance. */
    private PlayerBreath createPlayerBreath() {
        if(this.breath == null) {
            this.breath = new PlayerBreath();
        }

        return this.breath;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction direction) {
        if(cap == PLAYER_BREATH) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerBreath().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerBreath().loadNBTData(nbt);
    }

}
