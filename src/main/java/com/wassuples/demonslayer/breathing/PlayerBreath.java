package com.wassuples.demonslayer.breathing;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayerBreathSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class PlayerBreath {
    // Saves the data, and does the basic stuff around the data.
    private int breath = 100;
    public int maxBreath = 100;
    public final int MIN_BREATH = 0;

    public int getBreath() {
        return breath;
    }

    public int getMaxBreath() {
        return maxBreath;
    }

    public void addBreath(int add) {
        this.breath = Math.min(breath + add, maxBreath);
        // Just so that we do not go above the max breath amount!
    }

    public void subBreath(int sub) {
        this.breath = Math.max(breath - sub, MIN_BREATH);
        // Just so that we do not go below the min breath amount!
    }

    public void setBreath(int breath) {
        this.breath = Math.min(breath, maxBreath);
    }

    public void setMaxBreath(int amount) {
        this.maxBreath = Math.max(1, amount);
        if(this.breath > this.maxBreath) {
            this.breath = this.maxBreath;
        }
    }

    public void copyFrom(PlayerBreath source) {
        this.breath = source.breath;
        this.maxBreath = source.maxBreath;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("breath", breath);
        nbt.putInt("max_breath", maxBreath);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.breath = nbt.getInt("breath");
        // Check if the tag exists for compatibility with old saves
        if (nbt.contains("max_breath")) {
            this.maxBreath = nbt.getInt("max_breath");
        } else {
            this.maxBreath = 100; // Default if loading an old save
        }
    }
}
