package com.wassuples.demonslayer.demon;

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

public class PlayerDemonProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = new ResourceLocation(DemonSlayer.MOD_ID, "demon_data");
    public static Capability<PlayerDemon> PLAYER_DEMON = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerDemon demon = null;
    private final LazyOptional<PlayerDemon> optional = LazyOptional.of(this::createPlayerDemon);

    private PlayerDemon createPlayerDemon() {
        if (this.demon == null) {
            this.demon = new PlayerDemon();
        }
        return this.demon;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction dir) {
        return cap == PLAYER_DEMON ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerDemon().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerDemon().loadNBTData(nbt);
    }
}