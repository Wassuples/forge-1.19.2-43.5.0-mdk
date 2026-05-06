package com.wassuples.demonslayer.reputation.concealment;

import com.wassuples.demonslayer.reputation.Faction;
import com.wassuples.demonslayer.reputation.RepEventType;
import net.minecraft.nbt.CompoundTag;

public record SinRecord(RepEventType eventType, Faction faction, long worldDay) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("eventType", eventType.name());
        tag.putString("faction", faction.name());
        tag.putLong("worldDay", worldDay);
        return tag;
    }

    public static SinRecord load(CompoundTag tag) {
        return new SinRecord(
                RepEventType.valueOf(tag.getString("eventType")),
                Faction.valueOf(tag.getString("faction")),
                tag.getLong("worldDay")
        );
    }
}

