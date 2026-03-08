package com.wassuples.demonslayer.cooldown;

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
 * Capability provider for {@link PlayerCooldowns}.
 *
 * <p>Important: cooldown persistence depends on the current {@link net.minecraft.world.level.Level} game time.
 * Because {@link INBTSerializable} serialization does not provide a level reference, this provider intentionally
 * does not serialize itself.
 *
 * <p>Instead, cooldown save/load is performed in the player lifecycle/event code where a level reference is
 * available.
 */
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
        // This capability needs a Level context to convert cooldowns to remaining ticks.
        // We handle persistence across death/respawn explicitly in ModEvents where we have access to the player level.
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // No-op; see serializeNBT().
    }
}
