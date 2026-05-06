package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.ranking.RankRequirements;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static com.wassuples.demonslayer.ranking.PlayerProgressionProvider.PLAYER_PROGRESSION;

/**
 * Miscellaneous helper utilities used across the mod.
 *
 * <p>This class primarily contains <b>server-side</b> synchronization helpers which read a capability's
 * current server state and send the appropriate S2C sync packet(s) to the owning player.
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>These helpers should be called on the logical server.</li>
 *   <li>They are intentionally tolerant: if a capability is missing, they do nothing.</li>
 * </ul>
 */
public final class ModUtils {
    private ModUtils() {}

    /**
     * Sends the player's progression state to the client.
     *
     * <p>Includes rank, unlocked styles, and mastery maps.
     */
    public static void syncProgression(ServerPlayer player) {
        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> {
            ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(
                    player.getId(),
                    progression.getRank(),
                    progression.getUnlockedStyles(),
                    progression.getStyleMasteryMap(),
                    progression.getFormMasteryMap()
            ), player);
        });
    }

    /**
     * Sends the player's mission data to the client.
     *
     * <p>Serialization is performed via the missions capability NBT.
     */
    public static void syncMissions(ServerPlayer player) {
        player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            CompoundTag nbt = new CompoundTag();
            missions.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(nbt), player);
        });
    }

    /** Sends the player's currently selected breathing style/form to the client. */
    public static void syncStyle(ServerPlayer player) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), player);
        });
    }

    /** Sends the player's current breath state to the client for UI display. */
    public static void syncBreath(ServerPlayer player) {
        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player);
        });
    }

    /** Sends the player's demon state (BDA, passives, etc) to the client. */
    public static void syncDemon(ServerPlayer player) {
        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            CompoundTag nbt = new CompoundTag();
            demon.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(nbt), player);
        });
    }

    /** Convenience method that calls all sync helpers for the player. */
    public static void syncAll(ServerPlayer player) {
        syncProgression(player);
        syncMissions(player);
        syncStyle(player);
        syncBreath(player);
        syncDemon(player);
        syncGravity(player);
    }

    /** Sends the player's gravity orientation to clients so camera/model orientation stays in sync. */
    public static void syncGravity(ServerPlayer player) {
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            ModMessages.sendToClients(new PlayerGravitySyncS2CPacket(
                    player.getId(),
                    gravity.getGravity(),
                    gravity.getPreviousGravity(),
                    gravity.getTransitionProgress()
            ));
        });
    }

    /**
     * Applies all stat bonuses for the given rank to the player.
     * Called on login, respawn, and rank-up.
     *
     * <p>Reward ladder (cumulative — each rank keeps prior bonuses):
     * <pre>
     *  Mizunoto  — ++Speed
     *  Mizunoe   — ++Breath
     *  Kanoto    — ++Strength
     *  Kanoe     — ++Breath
     *  Tsuchinoto — ++Resistance
     *  Tsuchinoe  — ++Breath
     *  Hinoto    — ++Regen
     *  Hinoe     — ++Strength
     *  Kinoto    — ++Speed
     *  Kinoe     — ++Resistance
     *  Hashira   — ++Regen
     * </pre>
     */
    public static void applyRankStatBonuses(ServerPlayer player, Rank rank) {
        int ordinal = rank.ordinal(); // CIVILIAN=0, MIZUNOTO=1 … HASHIRA=11

        // Max Breath
        // Granted at Mizunoe(2), Kanoe(4), Tsuchinoe(6) → 1 bonus per threshold crossed
        int breathBonusCount = 0;
        if (ordinal >= 2) breathBonusCount++;
        if (ordinal >= 4) breathBonusCount++;
        if (ordinal >= 6) breathBonusCount++;
        int newMaxBreath = 100 + (breathBonusCount * 25);

        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.setMaxBreath(newMaxBreath);
            if (breath.getBreath() > newMaxBreath) {
                breath.setBreath(newMaxBreath);
            }
            syncBreath(player);
        });

        // Max Health
        double newMaxHealth = 20.0 + (ordinal * 2.0);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(newMaxHealth);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        // Permanent potion effects
        // Speed: Mizunoto(1) → Speed I,  Kinoto(9) → Speed II
        int speedAmplifier = -1;
        if (ordinal >= 1)  speedAmplifier = 0; // Speed I
        if (ordinal >= 9)  speedAmplifier = 1; // Speed II

        // Strength: Kanoto(3) → Strength I,  Hinoe(8) → Strength II
        int strengthAmplifier = -1;
        if (ordinal >= 3)  strengthAmplifier = 0;
        if (ordinal >= 8)  strengthAmplifier = 1;

        // Resistance: Tsuchinoto(5) → Resistance I,  Kinoe(10) → Resistance II
        int resistanceAmplifier = -1;
        if (ordinal >= 5)  resistanceAmplifier = 0;
        if (ordinal >= 10) resistanceAmplifier = 1;

        // Regeneration: Hinoto(7) → Regen I,  Hashira(11) → Regen II
        int regenAmplifier = -1;
        if (ordinal >= 7)  regenAmplifier = 0;
        if (ordinal >= 11) regenAmplifier = 1;

        applyPermanentEffect(player, MobEffects.MOVEMENT_SPEED,    speedAmplifier);
        applyPermanentEffect(player, MobEffects.DAMAGE_BOOST,      strengthAmplifier);
        applyPermanentEffect(player, MobEffects.DAMAGE_RESISTANCE, resistanceAmplifier);
        applyPermanentEffect(player, MobEffects.REGENERATION,      regenAmplifier);
    }

    /**
     * Evaluates rank-up requirements for the player and advances their rank if any are met.
     * Should be called after any event that could satisfy a requirement: demon kill, mastery gain, mission complete.
     */
    public static void checkAndApplyRankUp(ServerPlayer player) {
        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression ->
            player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                Rank current = progression.getRank();
                Rank[] ranks = Rank.values();
                int nextOrdinal = current.ordinal() + 1;
                if (nextOrdinal >= ranks.length) return;

                RankRequirements.Context ctx = new RankRequirements.Context(progression, missions);
                if (!RankRequirements.isMet(current, ctx)) return;

                Rank newRank = ranks[nextOrdinal];
                progression.setRank(newRank);
                applyRankStatBonuses(player, newRank);
                syncProgression(player);
                player.sendSystemMessage(
                    Component.literal("You have advanced to ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(newRank.getDisplayName())
                        .append(Component.literal("!").withStyle(ChatFormatting.GOLD))
                );
            })
        );
    }

    /**
     * Applies a permanent (max-duration, hidden-particles) mob effect at the given amplifier,
     * or removes it if {@code amplifier < 0}.
     */
    private static void applyPermanentEffect(ServerPlayer player, MobEffect effect, int amplifier) {
        if (amplifier < 0) {
            player.removeEffect(effect);
            return;
        }
        player.addEffect(new MobEffectInstance(effect, Integer.MAX_VALUE, amplifier, true, false, true));
    }
}
