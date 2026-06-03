package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.entity.custom.TamayoEntity;
import com.wassuples.demonslayer.reputation.ReputationService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet: tamayo offer response.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class TamayoOfferResponseC2SPacket {
    private final int tamayoEntityId;
    private final boolean accepted;

    public TamayoOfferResponseC2SPacket(int tamayoEntityId, boolean accepted) {
        this.tamayoEntityId = tamayoEntityId;
        this.accepted = accepted;
    }

    public TamayoOfferResponseC2SPacket(FriendlyByteBuf buf) {
        this.tamayoEntityId = buf.readInt();
        this.accepted = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.tamayoEntityId);
        buf.writeBoolean(this.accepted);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Validate Tamayo entity exists and is close enough.
            Level level = player.level;
            Entity e = level.getEntity(this.tamayoEntityId);
            if (!(e instanceof TamayoEntity tamayo)) {
                return;
            }

            double maxDistance = 8.0D;
            if (player.distanceToSqr(tamayo) > maxDistance * maxDistance) {
                player.sendSystemMessage(Component.literal("Tamayo is no longer close enough.").withStyle(ChatFormatting.RED));
                return;
            }

            // Validate health threshold at acceptance time.
            if (player.getHealth() > 1.0f) {
                player.sendSystemMessage(Component.literal("You're stable now. If you still need help, come back when you're truly at death's door.").withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Apply (or keep) a cooldown either way.
            player.getTags().removeIf(t -> t.startsWith("demonslayer:tamayo_offer_until:"));
            player.addTag("demonslayer:tamayo_offer_cooldown");
            player.addTag("demonslayer:tamayo_offer_until:" + (player.level.getGameTime() + (20L * 60L)));

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon()) {
                    player.sendSystemMessage(Component.literal("You're already a demon.").withStyle(ChatFormatting.YELLOW));
                    return;
                }

                if (!accepted) {
                    player.sendSystemMessage(Component.literal("Tamayo nods solemnly. \"I understand. The choice must be yours.\"").withStyle(ChatFormatting.GRAY));
                    return;
                }

                DemonProgressionHelper.initializeNewDemon(player, demon);
                ReputationService.onDemonPathAccepted(player);

                player.sendSystemMessage(Component.literal("Tamayo's medicine burns like fire through your veins...").withStyle(ChatFormatting.DARK_PURPLE));
           });
        });
        return true;
    }
}
