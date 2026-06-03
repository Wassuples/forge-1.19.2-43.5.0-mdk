package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.reputation.HardlineFlag;
import com.wassuples.demonslayer.reputation.PlayerRepRecord;
import com.wassuples.demonslayer.reputation.ReputationDimension;
import com.wassuples.demonslayer.reputation.ReputationManager;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Evaluates whether a {@link ServerPlayer} meets the access conditions for a given style master.
 *
 * <p>Conditions mirror the Style Master Table from the game design doc (s. 15):
 * <ul>
 *   <li>Minimum {@link Rank}</li>
 *   <li>Minimum Corps Rep</li>
 *   <li>Minimum Tanjiro Rep (affinity)</li>
 *   <li>Maximum Suspicion</li>
 *   <li>Maximum Muzan Favor</li>
 *   <li>Required {@link HardlineFlag} flags (player must have them)</li>
 *   <li>Blocking {@link HardlineFlag} flags (master refuses if any present)</li>
 *   <li>Misc boolean flags (e.g. must have killed a Kizuki)</li>
 * </ul>
 *
 * <p>Call {@link #evaluate(ServerPlayer, ServerLevel)} to get a {@link Result} with a boolean and
 * optional denial message.
 */
public class StyleMasterAccess {

    // ── immutable fields ──────────────────────────────────────────────────────

    private final Rank minRank;
    private final int minCorpsRep;
    private final int minTanjiroRep;
    private final int maxSuspicion;
    private final int maxMuzanRep;
    private final Set<HardlineFlag> requiredFlags;
    private final Set<HardlineFlag> blockingFlags;
    private final boolean requireKizukiKill;

    private StyleMasterAccess(Builder b) {
        this.minRank        = b.minRank;
        this.minCorpsRep    = b.minCorpsRep;
        this.minTanjiroRep  = b.minTanjiroRep;
        this.maxSuspicion   = b.maxSuspicion;
        this.maxMuzanRep    = b.maxMuzanRep;
        this.requiredFlags  = b.requiredFlags;
        this.blockingFlags  = b.blockingFlags;
        this.requireKizukiKill = b.requireKizukiKill;
    }

    // ── evaluation ────────────────────────────────────────────────────────────

    public Result evaluate(ServerPlayer player, ServerLevel level) {
        PlayerProgression progression = player
                .getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                .orElseThrow(() -> new IllegalStateException("Missing PlayerProgression cap"));

        PlayerRepRecord repRecord = ReputationManager.get(level)
                .getOrCreateRecord(player.getUUID());

        // Rank check
        if (progression.getRank().ordinal() < minRank.ordinal()) {
            return Result.deny(Component.literal(
                    "You must be at least rank " + minRank.getDisplayName().getString()
                    + " to train here.").withStyle(ChatFormatting.RED));
        }

        // Corps rep check
        if (repRecord.getCorpsRep() < minCorpsRep) {
            return Result.deny(Component.literal(
                    "Your Corps reputation is too low. Required: " + minCorpsRep + ".")
                    .withStyle(ChatFormatting.RED));
        }

        // Tanjiro affinity check
        if (repRecord.getTanjiroRep() < minTanjiroRep) {
            return Result.deny(Component.literal(
                    "Your affinity with Tanjiro is too low.")
                    .withStyle(ChatFormatting.RED));
        }

        // Suspicion check (Obanai)
        if (repRecord.getSuspicion() > maxSuspicion) {
            return Result.deny(Component.literal(
                    "I sense disloyalty in you. Come back when your record is cleaner.")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        // Muzan favor check (Shinobu)
        if (repRecord.getMuzanRep() > maxMuzanRep) {
            return Result.deny(Component.literal(
                    "You carry the scent of demons. I will not teach you.")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        // Required flags
        Set<HardlineFlag> playerFlags = repRecord.getHardlineFlags();
        for (HardlineFlag flag : requiredFlags) {
            if (!playerFlags.contains(flag)) {
                return Result.deny(getDenialForMissingFlag(flag));
            }
        }

        // Blocking flags
        for (HardlineFlag flag : blockingFlags) {
            if (playerFlags.contains(flag)) {
                return Result.deny(getDenialForBlockingFlag(flag));
            }
        }

        // Kizuki kill
        if (requireKizukiKill && !progression.hasKilledKizuki()) {
            return Result.deny(Component.literal(
                    "You have not yet proven yourself against the Twelve Kizuki.")
                    .withStyle(ChatFormatting.RED));
        }

        return Result.allow();
    }

    // ── flag denial messages ──────────────────────────────────────────────────

    private static Component getDenialForMissingFlag(HardlineFlag flag) {
        return switch (flag) {
            case MET_RENGOKU ->
                Component.literal("You must meet the Flame Hashira before speaking to his father.")
                         .withStyle(ChatFormatting.RED);
            case KILLED_LOWER_MOON ->
                Component.literal("Face a Moon demon first. Then we'll talk.")
                         .withStyle(ChatFormatting.RED);
            default -> Component.literal("You have not yet met the prerequisite.")
                               .withStyle(ChatFormatting.RED);
        };
    }

    private static Component getDenialForBlockingFlag(HardlineFlag flag) {
        return switch (flag) {
            case PROTECTED_NEZUKO ->
                Component.literal("I witnessed you shielding that demon. I refuse to teach Corps traitors.")
                         .withStyle(ChatFormatting.DARK_RED);
            default -> Component.literal("I will not teach you.")
                               .withStyle(ChatFormatting.DARK_RED);
        };
    }

    // ── result record ─────────────────────────────────────────────────────────

    public record Result(boolean allowed, @Nullable Component denialMessage) {
        public static Result allow()                    { return new Result(true, null); }
        public static Result deny(Component message)    { return new Result(false, message); }
    }

    // ── builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Rank minRank           = Rank.CIVILIAN;
        private int  minCorpsRep       = 0;
        private int  minTanjiroRep     = 0;
        private int  maxSuspicion      = Integer.MAX_VALUE;
        private int  maxMuzanRep       = Integer.MAX_VALUE;
        private Set<HardlineFlag> requiredFlags = EnumSet.noneOf(HardlineFlag.class);
        private Set<HardlineFlag> blockingFlags = EnumSet.noneOf(HardlineFlag.class);
        private boolean requireKizukiKill = false;

        public Builder minRank(Rank rank)               { this.minRank = rank; return this; }
        public Builder minCorpsRep(int v)               { this.minCorpsRep = v; return this; }
        public Builder minTanjiroRep(int v)             { this.minTanjiroRep = v; return this; }
        public Builder maxSuspicion(int v)              { this.maxSuspicion = v; return this; }
        public Builder maxMuzanRep(int v)               { this.maxMuzanRep = v; return this; }
        public Builder requireFlag(HardlineFlag... flags) {
            requiredFlags = EnumSet.noneOf(HardlineFlag.class);
            requiredFlags.addAll(Arrays.asList(flags));
            return this;
        }
        public Builder blockFlag(HardlineFlag... flags) {
            blockingFlags = EnumSet.noneOf(HardlineFlag.class);
            blockingFlags.addAll(Arrays.asList(flags));
            return this;
        }
        public Builder requireKizukiKill()              { this.requireKizukiKill = true; return this; }

        public StyleMasterAccess build()                { return new StyleMasterAccess(this); }
    }
}
