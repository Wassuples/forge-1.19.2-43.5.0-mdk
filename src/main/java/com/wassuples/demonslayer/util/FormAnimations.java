package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayPlayerAnimationS2CPacket;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-facing helper for form animation playback.
 *
 * <p>PlayerAnimator runs on clients, so forms call this class on the logical server and the packet
 * asks each client to play the named animation for the synced entity id.
 */
public final class FormAnimations {
    // Toon BDA Animations
    public static final String TOON_EYE_POP = "toon_eye_pop";
    public static final String TOON_ANVIL = "toon_anvil";
    public static final String TOON_BANANA = "toon_banana";
    public static final String TOON_SHADOW = "toon_shadow";
    public static final String TOON_TUNNEL = "toon_tunnel";
    public static final String TOON_POCKET = "toon_pocket";
    public static final String TOON_BONKED = "toon_bonked";
    public static final String TOON_SLIP = "toon_slip";
    public static final String TOON_PIE_SPLAT = "toon_pie_splat";
    public static final String TOON_TUNNEL_SLAM = "toon_tunnel_slam";
    public static final String TOON_RUN = "toon_run";
    public static final String TOON_FISH = "toon_fish";
    public static final String TOON_COOKIE = "toon_cookie";
    public static final String TOON_BOMB_PASS = "toon_bomb_pass";
    public static final String TOON_BOMB_GET = "toon_bomb_get";
    /** Rui: arms held out, a cat's cradle of red threads strung between the hands (Murderous Eye Basket). */
    public static final String RUI_CRADLE = "rui_cradle";
    /** Enmu: right hand raised palm-out toward the target while its mouth whispers (Whispers of Hypnosis). */
    public static final String ENMU_WHISPER = "enmu_whisper";
    /** Enmu: arms spread wide as the Mother Eye rises (Eyes of Forced Unconscious Sleep). */
    public static final String ENMU_EYES = "enmu_eyes";
    /** Anyone under Forced Sleep: slumped, head lolling, arms hanging. Loops until ENMU_WAKE replaces it. */
    public static final String ENMU_SLUMBER = "enmu_slumber";
    /** Ends the slumber pose. */
    public static final String ENMU_WAKE = "enmu_wake";
    /** Gyutaro: alternating overarm throws (Flying Blood Sickles). */
    public static final String GYUTARO_THROW = "gyutaro_throw";
    /** Gyutaro: crouched guard with both arms whirling (Rampant Arc Rampage). */
    public static final String GYUTARO_GUARD = "gyutaro_guard";
    /** Gyutaro: arms flung out wide in a spinning sweep (Rotating Circular Slashes). */
    public static final String GYUTARO_SPIN = "gyutaro_spin";
    /** Gyutaro: arms flung outward as a successful Rampant Arc parry explodes (replaces the guard). */
    public static final String GYUTARO_BURST = "gyutaro_burst";
    /** Daki: conducting her sashes, alternating arm flourishes for each of the eight layers. */
    public static final String DAKI_OBI = "daki_obi";
    /** Gyokko: both hands sweep forward and up, presenting a vase / releasing what's inside. */
    public static final String GYOKKO_VASE = "gyokko_vase";
    /** Hantengu (Aizetsu): draw back and hurl the cross spear (Weeping Spears). */
    public static final String HANTENGU_THRUST = "hantengu_thrust";
    /** Hantengu (Zohakuten): beating the drums on his back, one strike per wooden-dragon attack (Countless Striking Trees). */
    public static final String HANTENGU_DRUM = "hantengu_drum";
    /** Hantengu (Zohakuten): drumming wind-up, head thrown back for the cry, then conducting the lightning. */
    public static final String HANTENGU_CRY = "hantengu_cry";
    /** Hantengu (Zohakuten): lunging forward behind the dragon head's sound blast (Compressed Sound Waves). */
    public static final String HANTENGU_SOUND = "hantengu_sound";
    /** Ends a Hantengu channel early (cancelled Trees / Cry). */
    public static final String HANTENGU_END = "hantengu_end";
    /** Akaza: a straight right punch (Air Type, Crown Splitter lunge). */
    public static final String AKAZA_PUNCH = "akaza_punch";
    /** Akaza: crouched, fist drawn back, gathering power; held until replaced (Annihilation Type charge). */
    public static final String AKAZA_CHARGE = "akaza_charge";
    /** Akaza: the charged fist driven forward through the dash (Annihilation Type release). */
    public static final String AKAZA_DASH = "akaza_dash";
    /** Akaza: a rising kick (Crown Splitter). */
    public static final String AKAZA_UPPERKICK = "akaza_upperkick";
    /** Akaza: a blur of alternating punches for five seconds (Disorder). */
    public static final String AKAZA_BARRAGE = "akaza_barrage";
    /** Akaza: eight heavy alternating punches, five ticks apart (Eight-Layered Demon Core). */
    public static final String AKAZA_EIGHT = "akaza_eight";
    /** Akaza: five alternating kicks, three ticks apart (Explosive Flurry). */
    public static final String AKAZA_FLURRY = "akaza_flurry";
    /** Akaza: low gathering stance, then both arms flung forward (Final Form: Chaotic Afterglow). */
    public static final String AKAZA_FINAL = "akaza_final";
    /** Akaza: a flipping, spinning kick (Flying Planet Thousand Wheels). */
    public static final String AKAZA_WHEEL = "akaza_wheel";
    /** Akaza: a fist driven into the ground (Ten Thousand Leaves Willow). */
    public static final String AKAZA_WILLOW = "akaza_willow";
    /** Akaza: the stomp that unfolds the snowflake (Compass Needle). */
    public static final String AKAZA_COMPASS = "akaza_compass";
    /** Ends an Akaza animation early (cancelled charge / channel). */
    public static final String AKAZA_END = "akaza_end";
    /** Doma: one wide horizontal sweep of the right fan (Frozen Lotus). */
    public static final String DOMA_SWEEP = "doma_sweep";
    /** Doma: arms spread, fans open, a full turn while the garden lashes out (Barren Hanging Garden). */
    public static final String DOMA_SPIN = "doma_spin";
    /** Doma: both fans waved forward, over and over, driving the cloud (Freezing Clouds). */
    public static final String DOMA_WAVE = "doma_wave";
    /** Doma: fan raised, then pointed at the target (Lotus Vines, Wintry Icicles). */
    public static final String DOMA_POINT = "doma_point";
    /** Doma: a quick backhand flick of both fans (Scattering Lotuses). */
    public static final String DOMA_FLICK = "doma_flick";
    /** Doma: fans opened wide to either side, a graceful presentation (Cold White Princesses, Rime). */
    public static final String DOMA_SUMMON = "doma_summon";
    /** Doma: fans crossed before the face, gathering clones; held until replaced (Crystalline Divine Child). */
    public static final String DOMA_CHARGE = "doma_charge";
    /** Ends a Doma animation early. */
    public static final String DOMA_END = "doma_end";

    private FormAnimations() {
    }

    public static void play(LivingEntity entity, String animationId) {
        if (entity.level.isClientSide) {
            return;
        }

        ModMessages.sendToClients(new PlayPlayerAnimationS2CPacket(entity.getId(), animationId));
    }
}
