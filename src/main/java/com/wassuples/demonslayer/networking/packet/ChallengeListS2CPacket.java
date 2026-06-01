package com.wassuples.demonslayer.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client: delivers the list of demons this player may currently challenge.
 *
 * <p>The client caches the list in {@link #CACHED_LIST} for the challenge UI to read.
 * The list is cleared on screen open and refreshed via {@link RequestChallengeListC2SPacket}.
 */
public class ChallengeListS2CPacket {

    /**
     * A single challengeable target as seen by the client.
     *
     * @param id              UUID of the NPC demon in {@code DemonHierarchyManager}.
     * @param name            Display name of the demon.
     * @param bloodPoints     Current BP (used for sorting and display).
     * @param rankLabel       Human-readable rank string ("Fodder", "Lower Moon 6", etc.).
     * @param isRankChallenge {@code true} if winning this fight grants the player that Kizuki rank;
     *                        {@code false} if the reward is BP only.
     */
    public record Entry(UUID id, String name, int bloodPoints, String rankLabel, boolean isRankChallenge) {}

    /** Client-side cache; updated each time this packet arrives. */
    public static final List<Entry> CACHED_LIST = new ArrayList<>();

    private final List<Entry> entries;

    public ChallengeListS2CPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public ChallengeListS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID id           = buf.readUUID();
            String name       = buf.readUtf(64);
            int bloodPoints   = buf.readVarInt();
            String rankLabel  = buf.readUtf(32);
            boolean isRank    = buf.readBoolean();
            entries.add(new Entry(id, name, bloodPoints, rankLabel, isRank));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeUUID(e.id());
            buf.writeUtf(e.name(), 64);
            buf.writeVarInt(e.bloodPoints());
            buf.writeUtf(e.rankLabel(), 32);
            buf.writeBoolean(e.isRankChallenge());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            CACHED_LIST.clear();
            CACHED_LIST.addAll(entries);
        });
        return true;
    }
}
