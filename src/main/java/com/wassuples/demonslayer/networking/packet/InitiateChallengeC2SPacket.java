package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: the player has chosen a specific demon to challenge.
 *
 * <p>The server validates that the target is in the player's legal challenge pool,
 * spawns the demon entity near the player, and registers a
 * {@link DemonHierarchyManager.PendingChallenge} so that the kill event can grant
 * the correct reward.
 */
public class InitiateChallengeC2SPacket {

    private final UUID targetId;

    public InitiateChallengeC2SPacket(UUID targetId) {
        this.targetId = targetId;
    }

    public InitiateChallengeC2SPacket(FriendlyByteBuf buf) {
        this.targetId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level instanceof ServerLevel level)) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) return;

                DemonHierarchyManager manager = DemonHierarchyManager.get(level);

                // Validate target is in the player's legal pool
                List<ChallengeListS2CPacket.Entry> validTargets =
                        manager.getChallengeableTargets(player.getUUID(), demon.getRank());
                ChallengeListS2CPacket.Entry target = validTargets.stream()
                        .filter(e -> e.id().equals(targetId))
                        .findFirst().orElse(null);

                if (target == null) {
                    player.sendSystemMessage(Component.literal(
                            "That demon is not a valid challenge target.").withStyle(ChatFormatting.RED));
                    return;
                }

                // Prevent double-challenge of the same demon
                if (level.getEntity(targetId) != null) {
                    player.sendSystemMessage(Component.literal(
                            "That demon is already engaged in another battle.").withStyle(ChatFormatting.RED));
                    return;
                }

                // Register challenge BEFORE spawning so kill event can resolve it
                DemonRank targetKizukiRank = target.isRankChallenge()
                        ? manager.getKizukiRank(targetId).orElse(null) : null;
                manager.registerPlayerChallenge(player.getUUID(), targetId,
                        target.isRankChallenge(), targetKizukiRank);

                // Spawn the demon 3–8 blocks away in a random cardinal direction
                BlockPos spawnPos = player.blockPosition().offset(
                        level.random.nextInt(12) - 6, 0,
                        level.random.nextInt(12) - 6);
                boolean spawned = manager.spawnDemonFromData(targetId, level, spawnPos, "Player Challenge");

                if (spawned) {
                    String label = target.isRankChallenge()
                            ? "§dRank Challenge" : "§6BP Challenge";
                    player.sendSystemMessage(Component.literal(
                            label + " §7— defeat §f" + target.name()
                            + " §7[" + target.rankLabel() + ", " + target.bloodPoints() + " BP]!")
                            .withStyle(ChatFormatting.WHITE));
                } else {
                    // Rollback if spawn failed
                    manager.clearPlayerChallenge(player.getUUID());
                    player.sendSystemMessage(Component.literal(
                            "The demon could not be summoned. Try again later.").withStyle(ChatFormatting.RED));
                }
            });
        });
        return true;
    }
}
