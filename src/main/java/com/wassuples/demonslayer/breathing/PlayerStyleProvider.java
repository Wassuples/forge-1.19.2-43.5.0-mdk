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
 * Capability provider for {@link PlayerStyle}.
 *
 * <p>This capability stores the player's currently selected breathing style and form index.
 * It is persisted via NBT and synced to the client via packets (see {@code PlayerStyleSyncS2CPacket}).
 */
public class PlayerStyleProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = new ResourceLocation(DemonSlayer.MOD_ID, "style");
    // The actual capability provider.
    public static Capability<PlayerStyle> PLAYER_STYLE = CapabilityManager.get(new CapabilityToken<PlayerStyle>() { });
    private PlayerStyle style = null;
    private final LazyOptional<PlayerStyle> optional = LazyOptional.of(this::createPlayerStyle);

    private PlayerStyle createPlayerStyle() {
        if(this.style == null) {
            this.style = new PlayerStyle();
        }

        return this.style;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction direction) {
        if(cap == PLAYER_STYLE) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerStyle().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerStyle().loadNBTData(nbt);
    }

}
