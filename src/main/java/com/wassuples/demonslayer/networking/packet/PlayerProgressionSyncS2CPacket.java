package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Server-to-client packet: player progression sync.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class PlayerProgressionSyncS2CPacket {

    private final int entityId;
    private final Rank rank;
    private final Set<BreathingStyles> unlockedStyles;
    private final Map<BreathingStyles, Integer> styleMastery;
    private final Map<BreathingForms, Integer> formMastery;

    // Updated constructor
    public PlayerProgressionSyncS2CPacket(int entityId, Rank rank, Set<BreathingStyles> unlockedStyles, Map<BreathingStyles, Integer> styleMastery, Map<BreathingForms, Integer> formMastery) {
        this.entityId = entityId;
        this.rank = rank;
        this.unlockedStyles = unlockedStyles;
        this.styleMastery = styleMastery;
        this.formMastery = formMastery;
    }

    // Updated constructor from buffer
    public PlayerProgressionSyncS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        // Read rank and unlocked styles (unchanged)
        this.rank = buf.readEnum(Rank.class);
        int unlockedSize = buf.readVarInt();
        this.unlockedStyles = new HashSet<>();
        for (int i = 0; i < unlockedSize; i++) {
            unlockedStyles.add(buf.readEnum(BreathingStyles.class));
        }

        // Read style mastery map
        int styleMasterySize = buf.readVarInt();
        this.styleMastery = new HashMap<>();
        for (int i = 0; i < styleMasterySize; i++) {
            this.styleMastery.put(buf.readEnum(BreathingStyles.class), buf.readVarInt());
        }

        // Read form mastery map
        int formMasterySize = buf.readVarInt();
        this.formMastery = new HashMap<>();
        for (int i = 0; i < formMasterySize; i++) {
            this.formMastery.put(buf.readEnum(BreathingForms.class), buf.readVarInt());
        }
    }

    // Updated toBytes method
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        // Write rank and unlocked styles (unchanged)
        buf.writeEnum(this.rank);
        buf.writeVarInt(this.unlockedStyles.size());
        for (BreathingStyles style : this.unlockedStyles) {
            buf.writeEnum(style);
        }

        // Write style mastery map
        buf.writeVarInt(styleMastery.size());
        styleMastery.forEach((style, mastery) -> {
            buf.writeEnum(style);
            buf.writeVarInt(mastery);
        });

        // Write form mastery map
        buf.writeVarInt(formMastery.size());
        formMastery.forEach((form, mastery) -> {
            buf.writeEnum(form);
            buf.writeVarInt(mastery);
        });
    }

    // Updated handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // This code runs on the CLIENT side.
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity targetEntity = mc.level.getEntity(this.entityId);

                if (targetEntity instanceof Player targetPlayer) {

                    targetPlayer.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                        progression.setRank(this.rank);
                        progression.setUnlockedStyles(this.unlockedStyles);
                        progression.setAllStyleMastery(this.styleMastery);
                        progression.setAllFormMastery(this.formMastery);
                    });
                }
            }
        });
        return true;
    }
}