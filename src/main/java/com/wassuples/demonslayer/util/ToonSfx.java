package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.entity.custom.ToonSfxTextEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Cartoon sound-effect lettering for Toon Force.
 *
 * <p>Every {@link Kind} owns a pool of spellings and a pool of palettes. Each spawn picks a random
 * spelling, palette, burst style and tilt, so the same hit never reads the same way twice.
 * Rendering lives in {@code ToonSfxTextRenderer}.
 */
public final class ToonSfx {
    // Burst backgrounds, picked per spawn (see ToonSfxTextRenderer).
    public static final int STYLE_STARBURST = 0;
    public static final int STYLE_CLOUD = 1;
    public static final int STYLE_ZIGZAG = 2;
    public static final int STYLE_BLOCK_3D = 3;
    public static final int STYLE_SPEED_LINES = 4;
    public static final int STYLE_COUNT = 5;

    /** fill, outline, burst, burst-outline (ARGB). */
    private static final int[][] PALETTE_HOT = {
            {0xFFFFE23B, 0xFF2A1305, 0xFFFF4A2E, 0xFF2A1305},
            {0xFFFFFFFF, 0xFF1D0F35, 0xFFFF2E88, 0xFF1D0F35},
            {0xFFFF5A2A, 0xFF241008, 0xFFFFE86A, 0xFF241008}};
    private static final int[][] PALETTE_COOL = {
            {0xFF7DF2FF, 0xFF06213A, 0xFF2E7BFF, 0xFF06213A},
            {0xFFFFFFFF, 0xFF0B2547, 0xFF35D0FF, 0xFF0B2547},
            {0xFFB6FF6A, 0xFF0E2A10, 0xFF2BB673, 0xFF0E2A10}};
    private static final int[][] PALETTE_YELLOW = {
            {0xFFFFE135, 0xFF3A2405, 0xFF8BE04A, 0xFF1F3305},
            {0xFFFFF7A8, 0xFF3A2405, 0xFFFFB627, 0xFF3A2405},
            {0xFFFFE135, 0xFF16120A, 0xFFFFFFFF, 0xFF16120A}};
    private static final int[][] PALETTE_INK = {
            {0xFFE9E1FF, 0xFF0A0710, 0xFF4B2C7A, 0xFF0A0710},
            {0xFFFFFFFF, 0xFF09080D, 0xFF24212D, 0xFF000000},
            {0xFFC69BFF, 0xFF0A0710, 0xFF15121C, 0xFF000000}};
    private static final int[][] PALETTE_METAL = {
            {0xFFE6EEF5, 0xFF141A22, 0xFFFF9B2E, 0xFF141A22},
            {0xFFFFD86B, 0xFF1A1208, 0xFF8C96A3, 0xFF1A1208},
            {0xFFFFFFFF, 0xFF1A1208, 0xFFE0412F, 0xFF1A1208}};
    private static final int[][] PALETTE_CREAM = {
            {0xFFFFFBEF, 0xFF4A2A12, 0xFFF29BC1, 0xFF4A2A12},
            {0xFFFFE9B8, 0xFF4A2A12, 0xFFFFFFFF, 0xFF4A2A12},
            {0xFFFFFFFF, 0xFF6B3A12, 0xFFD9A05B, 0xFF6B3A12}};
    private static final int[][] PALETTE_DANGER = {
            {0xFFFF3B3B, 0xFF1A0505, 0xFFFFE23B, 0xFF1A0505},
            {0xFFFFFFFF, 0xFF1A0505, 0xFFE0211F, 0xFF1A0505}};
    private static final int[][] PALETTE_GOLD = {
            {0xFFFFD84A, 0xFF2B1A00, 0xFF8A3DFF, 0xFF1A0B33},
            {0xFFFFFFFF, 0xFF2B1A00, 0xFFFFB800, 0xFF2B1A00}};

    public enum Kind {
        POP(PALETTE_COOL, 18, "POP!", "PLOP!", "POIK!", "PA-POP!", "BLOOP!", "POIT!"),
        BOING(PALETTE_COOL, 14, "BOING!", "BOIOIOING!", "SPROING!", "BWOING!", "BOINK!", "DOINK!"),
        BONK(PALETTE_HOT, 24, "BONK!", "BONK!!", "WHACK!", "THWACK!", "BOP!", "KONK!", "POW!"),
        CLONK(PALETTE_HOT, 26, "CLONK!", "THUD!", "SMACK!", "KER-SMASH!", "BONG!"),
        WHIFF(PALETTE_YELLOW, 20, "WHIFF!", "WHOOPS!", "SKREEEE!", "ZWIP!", "WHOA-OA!"),
        YEET(PALETTE_YELLOW, 16, "YEET!", "FWOOSH!", "HUP!", "ZING!", "FWIP!"),
        SNAP(PALETTE_INK, 18, "SNAP!", "SHLOOP!", "ZIP!", "VWOOP!", "SHWIP!"),
        NOPE(PALETTE_INK, 18, "NOPE!", "NUH-UH!", "FZZT!", "NOT TODAY!"),
        TOSS(PALETTE_INK, 18, "TOSS!", "FLING!", "SHWOOP!", "FWUMP!"),
        KLANG(PALETTE_METAL, 24, "KLANG!", "CLANNG!", "KA-THUNK!", "BWANG!", "KRUNCH!"),
        PAINT(PALETTE_DANGER, 30, "PAINT A WALL!", "NEED A WALL!", "NO WALL?!"),
        TOO_THICK(PALETTE_DANGER, 30, "TOO THICK!", "SOLID ROCK?!", "NO WAY THROUGH!"),
        SPLAT(PALETTE_CREAM, 26, "SPLAT!", "SPLORCH!", "SPLOOSH!", "SMUSH!", "SPLUT!"),
        HAMMER(PALETTE_METAL, 22, "HAMMER!", "WHAM!", "KA-BLAM!", "SMASH!"),
        TICK(PALETTE_DANGER, 26, "TICK TICK!", "TIK-TOK!", "UH OH!", "FZZZ..."),
        FISH(PALETTE_COOL, 20, "FISH!", "SLAP!", "FWAP!", "SPLAP!"),
        JACKPOT(PALETTE_GOLD, 30, "JACKPOT!", "WINNER!", "BINGO!"),
        TWEET(PALETTE_YELLOW, 16, "TWEET!", "CHIRP!", "TWEET TWEET!"),
        POW(PALETTE_HOT, 24, "POW!", "KA-POW!", "BAM!", "SOCK!", "WHAMMO!"),
        BAWK(PALETTE_YELLOW, 22, "BAWK!", "SQUAWK!", "BUH-GAWK!", "BWAAK!"),
        ZAP(PALETTE_COOL, 18, "ZAP!", "BZZT!", "KRZZK!", "FZZAP!"),
        CUCKOO(PALETTE_GOLD, 26, "CUCKOO!", "COO-COO!", "CUCKOO CUCKOO!"),
        THWOOMP(PALETTE_METAL, 24, "THWOOMP!", "KA-CHUNK!", "SHHHLUNK!", "CLANK!"),
        /** Any text that doesn't match a known kind keeps its spelling but still gets full styling. */
        CUSTOM(PALETTE_HOT, 20);

        private final int[][] palettes;
        private final int defaultDuration;
        private final String[] variants;

        Kind(int[][] palettes, int defaultDuration, String... variants) {
            this.palettes = palettes;
            this.defaultDuration = defaultDuration;
            this.variants = variants;
        }

        public int defaultDuration() {
            return defaultDuration;
        }

        String pickText(RandomSource random) {
            // The canonical spelling shows up a bit more often so the move stays recognisable.
            if (variants.length == 0) return "";
            return random.nextFloat() < 0.35f ? variants[0] : variants[random.nextInt(variants.length)];
        }

        int[] pickPalette(RandomSource random) {
            return palettes[random.nextInt(palettes.length)];
        }

        boolean matches(String normalized) {
            for (String variant : variants) {
                if (normalize(variant).equals(normalized)) return true;
            }
            return false;
        }
    }

    private ToonSfx() {
    }

    public static ToonSfxTextEntity spawn(ServerLevel level, Vec3 pos, Kind kind) {
        return spawn(level, pos, kind, null, -1, kind.defaultDuration(), 1.0f);
    }

    public static ToonSfxTextEntity spawn(ServerLevel level, Vec3 pos, Kind kind, float scale) {
        return spawn(level, pos, kind, null, -1, kind.defaultDuration(), scale);
    }

    /**
     * Full control spawn.
     *
     * @param textOverride  exact text to show instead of a random variant (null = random variant)
     * @param fillOverride  ARGB fill colour to force (-1 = palette colour)
     */
    public static ToonSfxTextEntity spawn(ServerLevel level, Vec3 pos, Kind kind, String textOverride,
                                          int fillOverride, int durationTicks, float scale) {
        RandomSource random = level.random;
        String text = textOverride != null ? textOverride : kind.pickText(random);
        int[] palette = kind.pickPalette(random);
        int fill = fillOverride != -1 ? fillOverride : palette[0];

        int style;
        if (text.endsWith("?")) {
            style = STYLE_CLOUD; // questions read best in a thought-puff
        } else if (text.length() > 10) {
            style = random.nextBoolean() ? STYLE_BLOCK_3D : STYLE_SPEED_LINES; // long phrases stay legible
        } else {
            style = random.nextInt(STYLE_COUNT);
        }

        ToonSfxTextEntity entity = new ToonSfxTextEntity(level);
        entity.setPos(pos.x + (random.nextDouble() - 0.5) * 0.25, pos.y, pos.z + (random.nextDouble() - 0.5) * 0.25);
        entity.setup(text, fill, palette[1], palette[2], palette[3], style, random.nextInt(), durationTicks,
                scale * (0.9f + random.nextFloat() * 0.25f));
        level.addFreshEntity(entity);
        return entity;
    }

    /** Back-compat path used by {@code ToonForceHelper.spawnToonText}: classify plain text into a kind. */
    public static ToonSfxTextEntity spawnFromComponent(ServerLevel level, Vec3 pos, Component component, int durationTicks) {
        String raw = component.getString();
        String normalized = normalize(raw);
        boolean question = raw.trim().endsWith("?"); // "HAMMER?" (a whiff) must not become "WHAM!"
        for (Kind kind : Kind.values()) {
            if (!question && kind != Kind.CUSTOM && kind.matches(normalized)) {
                return spawn(level, pos, kind, null, -1, durationTicks, 1.0f);
            }
        }
        TextColor color = component.getStyle().getColor();
        int fill = color != null ? (0xFF000000 | color.getValue()) : -1;
        return spawn(level, pos, Kind.CUSTOM, raw.toUpperCase(Locale.ROOT), fill, durationTicks,
                raw.length() > 14 ? 0.8f : 1.0f);
    }

    private static String normalize(String text) {
        return text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z ]", "").trim();
    }
}
