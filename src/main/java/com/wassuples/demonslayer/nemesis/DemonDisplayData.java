package com.wassuples.demonslayer.nemesis;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Lightweight DTO for sending Nemesis demon information to the client for UI display.
 *
 * <p>Contains only the data needed for the hierarchy screen: rank label, name, title,
 * and blood points. Serializable via {@link net.minecraft.network.FriendlyByteBuf}.
 *
 * @see com.wassuples.demonslayer.nemesis.DemonHierarchyManager
 */
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
