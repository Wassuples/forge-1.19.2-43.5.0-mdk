package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.reputation.concealment.SinRecord;
import com.wassuples.demonslayer.reputation.rank.KillRecord;
import com.wassuples.demonslayer.reputation.rank.SlayerRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PlayerRepRecord {
    public static final int MIN_REP = -500;
    public static final int MAX_REP = 1000;
    public static final int MIN_SUSPICION = 0;
    public static final int MAX_SUSPICION = 500;
    private static final int MAX_RECENT_SINS = 50;

    private int corpsRep;
    private int tanjiroRep;
    private int muzanRep;
    private int demonRep;
    private int suspicion;

    private SlayerRank slayerRank = SlayerRank.MIZUNOTO;
    private KillRecord killRecord = new KillRecord();
    private final List<SinRecord> recentSins = new ArrayList<>();
    private final Set<HardlineFlag> hardlineFlags = EnumSet.noneOf(HardlineFlag.class);

    public int getCorpsRep() {
        return corpsRep;
    }

    public int getTanjiroRep() {
        return tanjiroRep;
    }

    public int getMuzanRep() {
        return muzanRep;
    }

    public int getDemonRep() {
        return demonRep;
    }

    public int getSuspicion() {
        return suspicion;
    }

    public SlayerRank getSlayerRank() {
        return slayerRank;
    }

    public KillRecord getKillRecord() {
        return killRecord;
    }

    public List<SinRecord> getRecentSins() {
        return recentSins;
    }

    public Set<HardlineFlag> getHardlineFlags() {
        return hardlineFlags;
    }

    public void setDimension(ReputationDimension dimension, int value) {
        switch (dimension) {
            case CORPS -> corpsRep = clampRep(value);
            case TANJIRO -> tanjiroRep = clampRep(value);
            case MUZAN -> muzanRep = clampRep(value);
            case DEMON -> demonRep = clampRep(value);
            case SUSPICION -> suspicion = clampSuspicion(value);
        }
        enforceHardlineLocks();
    }

    public void addDimension(ReputationDimension dimension, int delta) {
        switch (dimension) {
            case CORPS -> corpsRep = clampRep(corpsRep + delta);
            case TANJIRO -> tanjiroRep = clampRep(tanjiroRep + delta);
            case MUZAN -> muzanRep = clampRep(muzanRep + delta);
            case DEMON -> demonRep = clampRep(demonRep + delta);
            case SUSPICION -> suspicion = clampSuspicion(suspicion + delta);
        }
        enforceHardlineLocks();
    }

    public void applyDelta(RepDelta delta) {
        corpsRep = clampRep(corpsRep + delta.corps());
        tanjiroRep = clampRep(tanjiroRep + delta.tanjiro());
        muzanRep = clampRep(muzanRep + delta.muzan());
        demonRep = clampRep(demonRep + delta.demon());
        suspicion = clampSuspicion(suspicion + delta.suspicion());
        enforceHardlineLocks();
    }

    public void addHardline(HardlineFlag flag) {
        hardlineFlags.add(flag);
        enforceHardlineLocks();
    }

    public void removeHardline(HardlineFlag flag) {
        hardlineFlags.remove(flag);
        enforceHardlineLocks();
    }

    public void clearForReset() {
        corpsRep = 0;
        tanjiroRep = 0;
        muzanRep = 0;
        demonRep = 0;
        suspicion = 0;
        recentSins.clear();
        hardlineFlags.clear();
    }

    public void setSlayerRank(SlayerRank slayerRank) {
        this.slayerRank = slayerRank;
    }

    public void setKillRecord(KillRecord killRecord) {
        this.killRecord = killRecord;
    }

    public void addSin(SinRecord sinRecord) {
        recentSins.add(sinRecord);
        if (recentSins.size() > MAX_RECENT_SINS) {
            recentSins.remove(0);
        }
    }

    public void pruneSinsOlderThan(long earliestDay) {
        recentSins.removeIf(sin -> sin.worldDay() < earliestDay);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("corpsRep", corpsRep);
        tag.putInt("tanjiroRep", tanjiroRep);
        tag.putInt("muzanRep", muzanRep);
        tag.putInt("demonRep", demonRep);
        tag.putInt("suspicion", suspicion);
        tag.putString("slayerRank", slayerRank.name());
        tag.put("killRecord", killRecord.save());

        ListTag sinsTag = new ListTag();
        for (SinRecord sin : recentSins) {
            sinsTag.add(sin.save());
        }
        tag.put("recentSins", sinsTag);

        ListTag flagsTag = new ListTag();
        for (HardlineFlag flag : hardlineFlags) {
            CompoundTag flagTag = new CompoundTag();
            flagTag.putString("name", flag.name());
            flagsTag.add(flagTag);
        }
        tag.put("hardlineFlags", flagsTag);
        return tag;
    }

    public static PlayerRepRecord load(CompoundTag tag) {
        PlayerRepRecord out = new PlayerRepRecord();
        out.corpsRep = clampRep(tag.getInt("corpsRep"));
        out.tanjiroRep = clampRep(tag.getInt("tanjiroRep"));
        out.muzanRep = clampRep(tag.getInt("muzanRep"));
        out.demonRep = clampRep(tag.getInt("demonRep"));
        out.suspicion = clampSuspicion(tag.getInt("suspicion"));

        if (tag.contains("slayerRank", Tag.TAG_STRING)) {
            try {
                out.slayerRank = SlayerRank.valueOf(tag.getString("slayerRank"));
            } catch (IllegalArgumentException ignored) {
                out.slayerRank = SlayerRank.MIZUNOTO;
            }
        }

        if (tag.contains("killRecord", Tag.TAG_COMPOUND)) {
            out.killRecord = KillRecord.load(tag.getCompound("killRecord"));
        }

        ListTag sinsTag = tag.getList("recentSins", Tag.TAG_COMPOUND);
        for (Tag entry : sinsTag) {
            out.recentSins.add(SinRecord.load((CompoundTag) entry));
        }

        ListTag flagsTag = tag.getList("hardlineFlags", Tag.TAG_COMPOUND);
        for (Tag entry : flagsTag) {
            String name = ((CompoundTag) entry).getString("name");
            try {
                out.hardlineFlags.add(HardlineFlag.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid/missing flags from old saves.
            }
        }

        out.enforceHardlineLocks();
        return out;
    }

    private void enforceHardlineLocks() {
        if (hardlineFlags.contains(HardlineFlag.DSHQ_HASHIRA_KILL) || hardlineFlags.contains(HardlineFlag.DSHQ_AIDED_KIZUKI_ESCAPE)) {
            corpsRep = MIN_REP;
        }
        if (hardlineFlags.contains(HardlineFlag.TANJIRO_CREW_MEMBER_KILLED)) {
            tanjiroRep = MIN_REP;
        }
        if (hardlineFlags.contains(HardlineFlag.MUZAN_MEDICINE_DELIVERED)) {
            muzanRep = MIN_REP;
        }
    }

    private static int clampRep(int value) {
        return Math.max(MIN_REP, Math.min(MAX_REP, value));
    }

    private static int clampSuspicion(int value) {
        return Math.max(MIN_SUSPICION, Math.min(MAX_SUSPICION, value));
    }
}

