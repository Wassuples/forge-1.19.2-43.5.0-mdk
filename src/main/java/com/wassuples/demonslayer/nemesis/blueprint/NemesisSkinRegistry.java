package com.wassuples.demonslayer.nemesis.blueprint;

import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central skin registry/resolution for Nemesis demons.
 */
public final class NemesisSkinRegistry {
    private NemesisSkinRegistry() {}

    public static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/common_demon01.png");

    private static final Map<String, ResourceLocation> CANON_BLUEPRINT_SKINS = ImmutableMap.<String, ResourceLocation>builder()
            .put("LM6", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_six.png"))
            .put("LM5", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_five.png"))
            .put("LM4", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_four.png"))
            .put("LM3", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_three.png"))
            .put("LM2", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_two.png"))
            .put("LM1", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_one.png"))
            .put("UM6", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/common_demon01.png"))
            .put("UM5", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_five.png"))
            .put("UM4", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_four.png"))
            .put("UM3", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_three.png"))
            .put("UM2", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_two.png"))
            .put("UM1", new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_one.png"))
            .build();

    // Pool for random fallback skins. Canon Kizuki skins are intentionally excluded.
    private static final List<ResourceLocation> RANDOM_NON_CANON_SKINS = List.of(
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/common_demon01.png"),
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/fodder_demon01.png"),
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lowly_demon01.png"),
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/greater_demon01.png"),
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/elite_demon01.png")
    );

    @Nullable
    public static ResourceLocation getCanonicalSkinForBlueprint(@Nullable String blueprintId) {
        if (blueprintId == null || blueprintId.isBlank()) {
            return null;
        }
        return CANON_BLUEPRINT_SKINS.get(blueprintId.toUpperCase());
    }

    /**
     * Parses a skin location string from blueprint/trait values.
     *
     * Supported forms:
     * - Full resource location: "namespace:textures/entity/custom.png"
     * - Mod-relative texture path: "textures/entity/custom.png"
     * - Shorthand file/path under entity folder: "custom.png" or "skins/custom.png"
     */
    @Nullable
    public static ResourceLocation parseSkinLocation(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim().replace('\\', '/');
        try {
            if (value.contains(":")) {
                return new ResourceLocation(value);
            }

            if (value.startsWith("textures/")) {
                return new ResourceLocation(DemonSlayer.MOD_ID, ensurePng(value));
            }

            if (value.startsWith("entity/")) {
                return new ResourceLocation(DemonSlayer.MOD_ID, ensurePng("textures/" + value));
            }

            return new ResourceLocation(DemonSlayer.MOD_ID, ensurePng("textures/entity/" + value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static ResourceLocation pickRandomNonCanonSkin() {
        int idx = ThreadLocalRandom.current().nextInt(RANDOM_NON_CANON_SKINS.size());
        return RANDOM_NON_CANON_SKINS.get(idx);
    }

    public static ResourceLocation pickDeterministicNonCanonSkin(UUID stableId) {
        if (RANDOM_NON_CANON_SKINS.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        int idx = Math.floorMod(stableId.hashCode(), RANDOM_NON_CANON_SKINS.size());
        return RANDOM_NON_CANON_SKINS.get(idx);
    }

    private static String ensurePng(String path) {
        return path.endsWith(".png") ? path : path + ".png";
    }
}

