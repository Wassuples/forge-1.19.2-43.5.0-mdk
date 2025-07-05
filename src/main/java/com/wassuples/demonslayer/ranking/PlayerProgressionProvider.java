package com.wassuples.demonslayer.ranking;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.styles.BreathingForms;
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

    /**
     * NEW: Central static method to handle mastery gain.
     * This should be called every time a player successfully uses a form.
     */
    public static void grantMasteryOnUse(ServerPlayer player, BreathingForms form) {
        if (player == null || form == null) return;

        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> {
            // Grant 1 mastery point to the form itself.
            progression.addFormMastery(form, 1);

            progression.addStyleMastery(form.getStyle(), 1);

            // Here you could add more complex logic, like a small chance
            // for a "Mastery Bonus!" where the player gains extra points.
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