package com.wassuples.demonslayer.ranking;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.styles.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerProgressionProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(DemonSlayer.MOD_ID, "progression");
    public static Capability<PlayerProgression> PLAYER_PROGRESSION = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerProgression progression = null;
    private final LazyOptional<PlayerProgression> optional = LazyOptional.of(this::createPlayerProgression);

    private PlayerProgression createPlayerProgression() {
        if (this.progression == null) {
            this.progression = new PlayerProgression();
        }
        return this.progression;
    }


    public static void grantMasteryOnUse(ServerPlayer player, IForm form) {
        if (player == null || form == null) return;

        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> {
            if (form instanceof BreathingForms bf) {
                progression.addFormMastery(bf, 1);
            } else if (form instanceof DemonForms df) {
                progression.addDemonFormMastery(df, 1);
            }

            Object style = form.getStyle();
            if (style instanceof BreathingStyles breathingStyle) {
                progression.addStyleMastery(breathingStyle, 1);
            } else if (style instanceof BloodDemonArts bloodDemonArt) {
                progression.addDemonArtMastery(bloodDemonArt, 1);
            }
        });
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction dir) {
        return cap == PLAYER_PROGRESSION ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerProgression().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerProgression().loadNBTData(nbt);
    }
}