package com.wassuples.demonslayer.missions;

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

public class PlayerMissionsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = new ResourceLocation(DemonSlayer.MOD_ID, "missions");
    public static Capability<PlayerMissions> PLAYER_MISSIONS = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerMissions missions = null;
    private final LazyOptional<PlayerMissions> optional = LazyOptional.of(this::createPlayerMissions);

    private PlayerMissions createPlayerMissions() {
        if (this.missions == null) {
            this.missions = new PlayerMissions();
        }
        return this.missions;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction dir) {
        return cap == PLAYER_MISSIONS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerMissions().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerMissions().loadNBTData(nbt);
    }
}