package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Client → Server: request the current list of demons this player may challenge.
 *
 * <p>The server responds with a {@link ChallengeListS2CPacket} containing the
 * available targets based on the player's {@link DemonRank}.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@link DemonRank#ELITE_DEMON}: rank-taking challenge against LM6 + BP challenges vs fodder.</li>
 *   <li>Any Kizuki rank: rank-taking challenge against the rank directly above + BP challenges
 *       against weaker Kizuki and all fodder.</li>
 *   <li>All lower non-Kizuki ranks: BP challenges against fodder only.</li>
 * </ul>
 */
public class RequestChallengeListC2SPacket {

    public RequestChallengeListC2SPacket() {}
    public RequestChallengeListC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level instanceof ServerLevel level)) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) return;

                DemonHierarchyManager manager = DemonHierarchyManager.get(level);
                List<ChallengeListS2CPacket.Entry> entries =
                        manager.getChallengeableTargets(player.getUUID(), demon.getRank());

                ModMessages.sendToPlayer(new ChallengeListS2CPacket(entries), player);
            });
        });
        return true;
    }
}
