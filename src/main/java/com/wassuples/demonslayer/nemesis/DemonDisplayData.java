package com.wassuples.demonslayer.nemesis;

import net.minecraft.network.FriendlyByteBuf;

public record DemonDisplayData(String rank, String name, String title, int bloodPoints) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(rank);
        buf.writeUtf(name);
        buf.writeUtf(title);
        buf.writeInt(bloodPoints);
    }

    public static DemonDisplayData decode(FriendlyByteBuf buf) {
        return new DemonDisplayData(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt()
        );
    }
}