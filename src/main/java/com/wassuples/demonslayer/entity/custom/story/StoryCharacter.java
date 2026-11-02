package com.wassuples.demonslayer.entity.custom.story;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Named story characters played by {@link StoryNpcEntity}. Skins live in {@code textures/entity/npc/} (Tanjiro keeps
 * his original path). Names use {@code entity.demonslayer.<key>}, epithets {@code npc.demonslayer.<key>.title}.
 */
public enum StoryCharacter {
    TANJIRO("tanjiro_kamado", "textures/entity/tanjiro_kamado.png", false, 1.0F, true, 1, 0xFF3E8E5A),
    ZENITSU("zenitsu_agatsuma", "textures/entity/npc/zenitsu_agatsuma.png", false, 1.0F, true, 1, 0xFFE8C030),
    INOSUKE("inosuke_hashibira", "textures/entity/npc/inosuke_hashibira.png", false, 1.0F, true, 2, 0xFF6E86B8),
    KANAO("kanao_tsuyuri", "textures/entity/npc/kanao_tsuyuri.png", true, 0.96F, true, 1, 0xFFE07AA8),
    GENYA("genya_shinazugawa", "textures/entity/npc/genya_shinazugawa.png", false, 1.04F, true, 1, 0xFF8A6AB0),
    KIRIYA("kiriya_ubuyashiki", "textures/entity/npc/kiriya_ubuyashiki.png", true, 0.72F, false, 0, 0xFF9A7AE0),
    KANATA("kanata_ubuyashiki", "textures/entity/npc/kanata_ubuyashiki.png", true, 0.72F, false, 0, 0xFFB9A2EC),
    /** One of the ~twenty other candidates: own name and look per entity ({@link StoryNpcEntity#variant()}); can die. */
    CANDIDATE("candidate", "textures/entity/npc/candidate_0.png", false, 1.0F, true, 1, 0xFFB8B0A0),
    /** A swordsmith from the hidden village, in a hyottoko mask, who brings a new slayer their Nichirin blade. */
    SWORDSMITH("swordsmith", "textures/entity/npc/swordsmith.png", false, 1.0F, false, 0, 0xFFC8A060);

    /** Number of looks for {@link #CANDIDATE} (even = broad-shouldered, odd = slim). */
    public static final int CANDIDATE_VARIANTS = 6;

    private final String key;
    private final ResourceLocation texture;
    private final boolean slim;
    private final float scale;
    private final boolean fighter;
    private final int blades;
    private final int color;

    StoryCharacter(String key, String texture, boolean slim, float scale, boolean fighter, int blades, int color) {
        this.key = key;
        this.texture = new ResourceLocation(DemonSlayer.MOD_ID, texture);
        this.slim = slim;
        this.scale = scale;
        this.fighter = fighter;
        this.blades = blades;
        this.color = color;
    }

    public String key() {
        return key;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public boolean slim() {
        return slim;
    }

    public float scale() {
        return scale;
    }

    /** Candidates fight demons; the twins never do (and can't be harmed). */
    public boolean fighter() {
        return fighter;
    }

    /** Swords carried: Inosuke fights with two. */
    public int blades() {
        return blades;
    }

    /** Accent colour for their name plate, speech bubbles and dialogue frame. */
    public int color() {
        return color;
    }

    /** Named characters are protected by the story; the anonymous candidates are not. */
    public boolean named() {
        return this != CANDIDATE;
    }

    public static ResourceLocation candidateTexture(int variant) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/npc/candidate_" + Math.floorMod(variant, CANDIDATE_VARIANTS) + ".png");
    }

    public boolean isTwin() {
        return this == KIRIYA || this == KANATA;
    }

    public Component displayName() {
        return Component.translatable("entity.demonslayer." + key);
    }

    public Component title() {
        return Component.translatable("npc.demonslayer." + key + ".title");
    }

    public static StoryCharacter byId(int id) {
        StoryCharacter[] all = values();
        return id >= 0 && id < all.length ? all[id] : TANJIRO;
    }

    /** The candidates who wait at the shrine and survive the Final Selection alongside the player. */
    public static final StoryCharacter[] CANDIDATES = {TANJIRO, ZENITSU, INOSUKE, KANAO, GENYA};
}
