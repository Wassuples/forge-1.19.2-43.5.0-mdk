package com.wassuples.demonslayer.reputation.rank;

import com.wassuples.demonslayer.reputation.PlayerRepRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RankAdvancementChecker {

    public static void checkAdvancement(ServerPlayer player, PlayerRepRecord record) {
        SlayerRank current = record.getSlayerRank();
        KillRecord kr = record.getKillRecord();
        SlayerRank newRank = current;

        int kills = kr.getKillsAtCurrentRank();

        if (current == SlayerRank.MIZUNOTO && kills >= 10) newRank = SlayerRank.MIZUNOE;
        if (current == SlayerRank.MIZUNOE && kills >= 30) newRank = SlayerRank.KANOTO;
        if (current == SlayerRank.KANOTO && kills >= 60) newRank = SlayerRank.KANOE;
        if (current == SlayerRank.KANOE && kills >= 100) newRank = SlayerRank.TSUCHINOTO;
        if (current == SlayerRank.TSUCHINOTO && kills >= 150) newRank = SlayerRank.TSUCHINOE;

        if (current == SlayerRank.TSUCHINOE && kills >= 250) newRank = SlayerRank.HINOTO;
        if (current == SlayerRank.HINOTO && kills >= 300) newRank = SlayerRank.HINOE;
        if (current == SlayerRank.HINOE && kills >= 400) newRank = SlayerRank.KINOTO;
        if (current == SlayerRank.KINOTO && kills >= 500) newRank = SlayerRank.KINOE;
        if (current == SlayerRank.KINOE && (kills >= 500 || kr.getKizukiSoloKills() > 0)) newRank = SlayerRank.HASHIRA;

        if (kr.getKizukiSoloKills() > 0 && current != SlayerRank.HASHIRA) {
            newRank = SlayerRank.HASHIRA;
        }

        if (newRank != current) {
            record.setSlayerRank(newRank);
            kr.resetForRankAdvance();

            player.sendSystemMessage(Component.literal("You have been promoted to " + newRank.getDisplayName().getString() + "!").withStyle(newRank.getDisplayName().getStyle()));
        }
    }
}
