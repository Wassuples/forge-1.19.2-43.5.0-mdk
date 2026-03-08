package com.wassuples.demonslayer.breathing;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayerBreathSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative "breath" resource used for Breathing Style forms.
 *
 * <p>This is stored as a Forge capability and is persisted via NBT. The value is intended to be modified
 * solely on the logical server and then synchronized to the client for UI/display.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code 0 <= breath <= maxBreath}</li>
 *   <li>{@code maxBreath >= 1}</li>
 * </ul>
 */
public class PlayerBreath {
    /** Current breath value (clamped to {@link #maxBreath}). */
    private int breath = 100;

    /** Maximum breath value; used as the clamp upper bound. */
    public int maxBreath = 100;

    /** Lower clamp bound for breath. */
    public final int MIN_BREATH = 0;

    /** @return current breath in [0, maxBreath] */
    public int getBreath() {
        return breath;
    }

    /** @return maximum breath (always >= 1) */
    public int getMaxBreath() {
        return maxBreath;
    }

    /**
     * Adds breath.
     *
     * <p>Contract: breath is clamped to {@link #maxBreath}.
     */
    public void addBreath(int add) {
        this.breath = Math.min(breath + add, maxBreath);
    }

    /**
     * Subtracts breath.
     *
     * <p>Contract: breath is clamped to {@link #MIN_BREATH}.
     */
    public void subBreath(int sub) {
        this.breath = Math.max(breath - sub, MIN_BREATH);
    }

    /**
     * Sets breath directly.
     *
     * <p>Contract: clamps to {@link #maxBreath}. This does not clamp below zero; callers should ensure
     * non-negative input.
     */
    public void setBreath(int breath) {
        this.breath = Math.min(breath, maxBreath);
    }

    /**
     * Sets max breath.
     *
     * <p>Contract:
     *
     * <ul>
     *   <li>{@code maxBreath} is clamped to at least 1.</li>
     *   <li>If current breath is above the new max, breath is reduced to match the max.</li>
     * </ul>
     */
    public void setMaxBreath(int amount) {
        this.maxBreath = Math.max(1, amount);
        if (this.breath > this.maxBreath) {
            this.breath = this.maxBreath;
        }
    }

    /** Copies breath values from another instance (used during cloning on respawn). */
    public void copyFrom(PlayerBreath source) {
        this.breath = source.breath;
        this.maxBreath = source.maxBreath;
    }

    /** Writes breath values to the provided NBT tag. */
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("breath", breath);
        nbt.putInt("max_breath", maxBreath);
    }

    /**
     * Reads breath values from NBT.
     *
     * <p>Backwards compatibility: if {@code max_breath} is missing, defaults to 100.
     */
    public void loadNBTData(CompoundTag nbt) {
        this.breath = nbt.getInt("breath");
        if (nbt.contains("max_breath")) {
            this.maxBreath = nbt.getInt("max_breath");
        } else {
            this.maxBreath = 100;
        }
    }

    /**
     * Convenience method to sync the current breath state to a specific client player.
     *
     * <p>Note: most code uses {@code ModUtils.syncBreath(...)}; this helper exists for cases where this
     * capability class is the most convenient place to trigger sync.
     */
    public void syncTo(ServerPlayer player) {
        ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(getBreath(), getMaxBreath()), player);
    }
}
