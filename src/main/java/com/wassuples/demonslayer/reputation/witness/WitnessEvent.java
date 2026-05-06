package com.wassuples.demonslayer.reputation.witness;

import com.wassuples.demonslayer.reputation.RepEventType;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record WitnessEvent(UUID playerId, RepEventType eventType, WitnessType witnessType, long gameTime) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        tag.putString("eventType", eventType.name());
        tag.putString("witnessType", witnessType.name());
        tag.putLong("gameTime", gameTime);
        return tag;
    }

    public static WitnessEvent load(CompoundTag tag) {
        return new WitnessEvent(
                tag.getUUID("playerId"),
                RepEventType.valueOf(tag.getString("eventType")),
                WitnessType.valueOf(tag.getString("witnessType")),
                tag.getLong("gameTime")
        );
    }
}

