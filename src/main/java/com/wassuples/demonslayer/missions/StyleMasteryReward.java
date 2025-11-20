package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public record StyleMasteryReward(@Nullable BreathingStyles style, int amount) implements MissionReward {
    public StyleMasteryReward(FriendlyByteBuf buf) {
        this(buf.readBoolean() ? buf.readEnum(BreathingStyles.class) : null, buf.readInt());
    }

    @Override
    public Component getDescription() {
        if (style != null) {
            return Component.literal("+" + amount + " " + style.getDisplayName() + " Mastery");
        } else {
            return Component.literal("+" + amount + " Current Style Mastery");
        }
    }

    @Override
    public void grant(ServerPlayer player) {
        if (this.style != null) {
            player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                progression.addStyleMastery(this.style, this.amount);
            });
        } else {
            player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(playerStyle -> {
                player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                    progression.addStyleMastery(playerStyle.getCurrentStyles(), this.amount);
                });
            });
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(style != null);
        if (style != null) {
            buf.writeEnum(style);
        }
        buf.writeInt(amount);
    }

    @Override
    public RewardType<?> getType() {
        return RewardType.STYLE_MASTERY;
    }

    public static StyleMasteryReward specific(BreathingStyles style, int amount) {
        return new StyleMasteryReward(style, amount);
    }

    public static StyleMasteryReward current(int amount) {
        return new StyleMasteryReward(null, amount);
    }
}