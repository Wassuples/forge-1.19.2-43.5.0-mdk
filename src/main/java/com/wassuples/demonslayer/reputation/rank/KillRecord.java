package com.wassuples.demonslayer.reputation.rank;

import net.minecraft.nbt.CompoundTag;

public class KillRecord {
    private int killsAtCurrentRank;
    private int kizukiSoloKills;
    private int totalDemonKills;

    public int getKillsAtCurrentRank() {
        return killsAtCurrentRank;
    }

    public int getKizukiSoloKills() {
        return kizukiSoloKills;
    }

    public int getTotalDemonKills() {
        return totalDemonKills;
    }

    public void recordDemonKill() {
        killsAtCurrentRank++;
        totalDemonKills++;
    }

    public void recordKizukiSoloKill() {
        kizukiSoloKills++;
        recordDemonKill();
    }

    public void resetForRankAdvance() {
        killsAtCurrentRank = 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("killsAtCurrentRank", killsAtCurrentRank);
        tag.putInt("kizukiSoloKills", kizukiSoloKills);
        tag.putInt("totalDemonKills", totalDemonKills);
        return tag;
    }

    public static KillRecord load(CompoundTag tag) {
        KillRecord out = new KillRecord();
        out.killsAtCurrentRank = tag.getInt("killsAtCurrentRank");
        out.kizukiSoloKills = tag.getInt("kizukiSoloKills");
        out.totalDemonKills = tag.getInt("totalDemonKills");
        return out;
    }
}

