package com.wassuples.demonslayer.cooldown;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerCooldownsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = new ResourceLocation(DemonSlayer.MOD_ID, "player_cooldowns");
    public static Capability<PlayerCooldowns> PLAYER_COOLDOWNS = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerCooldowns cooldowns = null;
    private final LazyOptional<PlayerCooldowns> optional = LazyOptional.of(this::createPlayerCooldowns);

    private PlayerCooldowns createPlayerCooldowns() {
        if (this.cooldowns == null) {
            this.cooldowns = new PlayerCooldowns();
        }
        return this.cooldowns;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction dir) {
        return cap == PLAYER_COOLDOWNS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerCooldowns().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerCooldowns().loadNBTData(nbt);
    }
}