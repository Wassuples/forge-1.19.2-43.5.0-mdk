package com.wassuples.demonslayer.client.animation;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.toon.ToonCameraAnimator;
import com.wassuples.demonslayer.client.toon.ToonFallCurve;
import com.wassuples.demonslayer.entity.renderers.DomaFansLayer;
import com.wassuples.demonslayer.util.AkazaKicks;
import com.wassuples.demonslayer.util.FormAnimations;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.AnimationFormat;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientFormAnimations {
    private static final ResourceLocation TOON_LAYER = new ResourceLocation(DemonSlayer.MOD_ID, "toon_force_player");

    private ClientFormAnimations() {
    }

    public static void registerLayer() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(TOON_LAYER, 800,
                player -> new ModifierLayer<>());
    }

    public static void play(int entityId, String animationId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        IAnimation registered = PlayerAnimationAccess.getPlayerAssociatedData(player).get(TOON_LAYER);
        if (!(registered instanceof ModifierLayer<?> layer)) {
            return;
        }

        KeyframeAnimation animation = create(animationId);
        if (animation == null) {
            return;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        ModifierLayer rawLayer = layer;
        rawLayer.setAnimation(new KeyframeAnimationPlayer(animation));

        // Doma fights with a pair of golden fans: show them while any of his animations plays.
        if (animationId.startsWith("doma_")) {
            DomaFansLayer.showFans(entityId, animationId.equals(FormAnimations.DOMA_END) ? 0 : animation.endTick + 10);
        }

        // First-person camera rides along with knock-down animations on the local player.
        if (player == minecraft.player) {
            ToonCameraAnimator.onAnimationStarted(animationId);
        }
    }

    /** Weapon swing players we started; only these may be interrupted by the next swing. */
    private static final java.util.Set<IAnimation> WEAPON_SWINGS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /**
     * Plays a weapon swing on the player's animation layer unless a form/BDA animation is running (those always win).
     * Returns true if it started.
     */
    public static boolean playWeaponSwing(AbstractClientPlayer player, KeyframeAnimation animation) {
        IAnimation registered = PlayerAnimationAccess.getPlayerAssociatedData(player).get(TOON_LAYER);
        if (!(registered instanceof ModifierLayer<?> layer)) return false;
        IAnimation current = layer.getAnimation();
        if (current != null && current.isActive() && !WEAPON_SWINGS.contains(current)) return false;
        KeyframeAnimationPlayer swing = new KeyframeAnimationPlayer(animation);
        WEAPON_SWINGS.add(swing);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ModifierLayer rawLayer = layer;
        rawLayer.setAnimation(swing);
        return true;
    }

    private static KeyframeAnimation create(String animationId) {
        // Seeded variants: "<id>#<seed>" (Explosive Flurry's kick arcs are shared with its visual entity).
        int hash = animationId.indexOf('#');
        if (hash > 0) {
            String base = animationId.substring(0, hash);
            int seed;
            try {
                seed = Integer.parseInt(animationId.substring(hash + 1));
            } catch (NumberFormatException e) {
                seed = 0;
            }
            if (FormAnimations.AKAZA_FLURRY.equals(base)) return akazaFlurry(seed);
            animationId = base;
        }
        return switch (animationId) {
            // Toon BDA Animations
            case FormAnimations.TOON_EYE_POP -> eyePop();
            case FormAnimations.TOON_ANVIL -> anvil();
            case FormAnimations.TOON_BANANA -> bananaThrow();
            case FormAnimations.TOON_SHADOW -> shadowToss();
            case FormAnimations.TOON_TUNNEL -> paintTunnel();
            case FormAnimations.TOON_POCKET -> pocketPull();
            case FormAnimations.TOON_BONKED -> bonked();
            case FormAnimations.TOON_SLIP -> slip();
            case FormAnimations.TOON_PIE_SPLAT -> pieSplat();
            case FormAnimations.TOON_TUNNEL_SLAM -> tunnelSlam();
            case FormAnimations.TOON_RUN -> empty("toon_run");
            case FormAnimations.TOON_FISH -> empty("toon_fish");
            case FormAnimations.TOON_COOKIE -> empty("toon_cookie");
            case FormAnimations.TOON_BOMB_PASS -> empty("toon_bomb_pass");
            case FormAnimations.TOON_BOMB_GET -> empty("toon_bomb_get");
            case FormAnimations.RUI_CRADLE -> ruiCradle();
            case FormAnimations.ENMU_WHISPER -> enmuWhisper();
            case FormAnimations.ENMU_EYES -> enmuEyes();
            case FormAnimations.ENMU_SLUMBER -> enmuSlumber();
            case FormAnimations.ENMU_WAKE -> empty("enmu_wake");
            case FormAnimations.GYUTARO_THROW -> gyutaroThrow();
            case FormAnimations.GYUTARO_GUARD -> gyutaroGuard();
            case FormAnimations.GYUTARO_SPIN -> gyutaroSpin();
            case FormAnimations.GYUTARO_BURST -> gyutaroBurst();
            case FormAnimations.DAKI_OBI -> dakiObi();
            case FormAnimations.GYOKKO_VASE -> gyokkoVase();
            case FormAnimations.HANTENGU_THRUST -> hantenguThrust();
            case FormAnimations.HANTENGU_DRUM -> hantenguDrum();
            case FormAnimations.HANTENGU_CRY -> hantenguCry();
            case FormAnimations.HANTENGU_SOUND -> hantenguSound();
            case FormAnimations.HANTENGU_END -> empty("hantengu_end");
            case FormAnimations.AKAZA_PUNCH -> akazaPunch();
            case FormAnimations.AKAZA_CHARGE -> akazaCharge();
            case FormAnimations.AKAZA_DASH -> akazaDash();
            case FormAnimations.AKAZA_UPPERKICK -> akazaUpperkick();
            case FormAnimations.AKAZA_BARRAGE -> akazaPunches("akaza_barrage", 100, 2, -1.45f, 0.15f);
            case FormAnimations.AKAZA_EIGHT -> akazaPunches("akaza_eight", 40, 5, -1.6f, 0.3f);
            case FormAnimations.AKAZA_FLURRY -> akazaFlurry(0);
            case FormAnimations.AKAZA_FINAL -> akazaFinal();
            case FormAnimations.AKAZA_WHEEL -> akazaWheel();
            case FormAnimations.AKAZA_WILLOW -> akazaWillow();
            case FormAnimations.AKAZA_COMPASS -> akazaCompass();
            case FormAnimations.AKAZA_END -> empty("akaza_end");
            case FormAnimations.DOMA_SWEEP -> domaSweep();
            case FormAnimations.DOMA_SPIN -> domaSpin();
            case FormAnimations.DOMA_WAVE -> domaWave();
            case FormAnimations.DOMA_POINT -> domaPoint();
            case FormAnimations.DOMA_FLICK -> domaFlick();
            case FormAnimations.DOMA_SUMMON -> domaSummon();
            case FormAnimations.DOMA_CHARGE -> domaCharge();
            case FormAnimations.DOMA_END -> empty("doma_end");
            default -> null;
        };
    }

    private static KeyframeAnimation empty(String name) {
        return builder(name, 10).build();
    }

    private static KeyframeAnimation eyePop() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_eye_pop", 12);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.head.z, 2, -0.32f, Ease.OUTBACK);
        key(builder.head.pitch, 2, -0.28f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 3, -2.3f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 3, -2.3f, Ease.OUTBACK);
        key(builder.rightArm.roll, 3, 0.45f, Ease.OUTBACK);
        key(builder.leftArm.roll, 3, -0.45f, Ease.OUTBACK);
        key(builder.head.z, 8, 0.0f, Ease.OUTBOUNCE);
        key(builder.head.pitch, 8, 0.0f, Ease.OUTBOUNCE);
        pose(builder, 12, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation anvil() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_anvil", 18);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.y, 4, 0.18f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 4, -2.55f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 4, -2.55f, Ease.OUTBACK);
        key(builder.rightArm.roll, 4, 0.2f, Ease.OUTSINE);
        key(builder.leftArm.roll, 4, -0.2f, Ease.OUTSINE);
        key(builder.body.y, 10, -0.12f, Ease.INBACK);
        key(builder.rightArm.pitch, 10, 1.1f, Ease.INBACK);
        key(builder.leftArm.pitch, 10, 1.1f, Ease.INBACK);
        key(builder.torso.pitch, 10, 0.32f, Ease.INBACK);
        pose(builder, 18, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation bananaThrow() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_banana", 14);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.torso.yaw, 3, -0.38f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 3, -1.65f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 3, -0.65f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 7, -0.3f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 7, 0.65f, Ease.OUTBACK);
        key(builder.torso.yaw, 7, 0.18f, Ease.OUTBACK);
        pose(builder, 14, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation shadowToss() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_shadow", 16);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.torso.pitch, 4, 0.55f, Ease.OUTSINE);
        key(builder.body.y, 4, -0.08f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 4, 1.25f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 4, 0.95f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 8, -1.45f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 8, -1.25f, Ease.OUTBACK);
        key(builder.torso.pitch, 8, -0.2f, Ease.OUTBACK);
        pose(builder, 16, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation paintTunnel() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_tunnel", 20);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 4, -1.9f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 4, -0.8f, Ease.OUTSINE);
        key(builder.torso.roll, 4, -0.2f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 9, 0.9f, Ease.INOUTSINE);
        key(builder.torso.roll, 9, 0.25f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 14, -0.65f, Ease.INOUTSINE);
        key(builder.torso.roll, 14, -0.15f, Ease.INOUTSINE);
        pose(builder, 20, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation pocketPull() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_pocket", 40);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.leftArm.pitch, 4, 0.9f, Ease.OUTSINE);
        key(builder.leftArm.yaw, 4, 0.75f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 4, 0.65f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 4, -0.65f, Ease.OUTSINE);
        key(builder.torso.yaw, 4, 0.32f, Ease.OUTSINE);
        key(builder.body.y, 8, -0.08f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 10, 0.95f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 10, -0.95f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 16, 0.55f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 16, -0.45f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 22, 0.95f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 22, -0.95f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 30, -1.8f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 30, 0.35f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 30, -0.3f, Ease.OUTBACK);
        key(builder.torso.yaw, 30, -0.2f, Ease.OUTBACK);
        key(builder.body.y, 30, 0.08f, Ease.OUTBACK);
        pose(builder, 40, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation bonked() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_bonked", 24);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.y, 3, -0.45f, Ease.OUTBOUNCE);
        key(builder.torso.pitch, 3, 1.05f, Ease.OUTBOUNCE);
        key(builder.head.pitch, 3, 0.45f, Ease.OUTBOUNCE);
        key(builder.rightArm.pitch, 3, 1.35f, Ease.OUTBOUNCE);
        key(builder.leftArm.pitch, 3, 1.35f, Ease.OUTBOUNCE);
        key(builder.rightLeg.pitch, 3, -0.65f, Ease.OUTBOUNCE);
        key(builder.leftLeg.pitch, 3, -0.65f, Ease.OUTBOUNCE);
        key(builder.body.y, 14, -0.18f, Ease.OUTSINE);
        key(builder.torso.pitch, 14, 0.35f, Ease.OUTSINE);
        pose(builder, 24, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Banana peel: feet fly out, land flat on the back, lie there, sit up. Body motion comes from ToonFallCurve. */
    private static KeyframeAnimation slip() {
        float[][] keys = ToonFallCurve.SLIP;
        KeyframeAnimation.AnimationBuilder builder = builder("toon_slip", ToonFallCurve.length(keys));
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        applyFallCurve(builder, keys);
        // Airborne flail: legs kick forward, arms windmill up.
        key(builder.rightLeg.pitch, 3, -1.55f, Ease.INOUTSINE);
        key(builder.leftLeg.pitch, 3, -1.05f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 3, -2.7f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 3, -2.4f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 3, 0.5f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 3, -0.5f, Ease.INOUTSINE);
        key(builder.head.pitch, 3, 0.35f, Ease.INOUTSINE);
        layFlatLimbs(builder, 9, 34);
        pose(builder, ToonFallCurve.length(keys), 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Fake Tunnel victim: face-plant into the wall, rebound, flat on the back. */
    private static KeyframeAnimation tunnelSlam() {
        float[][] keys = ToonFallCurve.TUNNEL_SLAM;
        KeyframeAnimation.AnimationBuilder builder = builder("toon_tunnel_slam", ToonFallCurve.length(keys));
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        applyFallCurve(builder, keys);
        key(builder.head.pitch, 2, -0.55f, Ease.INOUTSINE);   // head snaps back off the wall
        key(builder.rightArm.pitch, 2, -0.6f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 2, -0.6f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 5, -2.4f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 5, -2.6f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 5, 0.7f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 5, -0.7f, Ease.INOUTSINE);
        key(builder.rightLeg.pitch, 5, -0.6f, Ease.INOUTSINE);
        key(builder.leftLeg.pitch, 5, -0.3f, Ease.INOUTSINE);
        layFlatLimbs(builder, 10, 35);
        pose(builder, ToonFallCurve.length(keys), 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static void applyFallCurve(KeyframeAnimation.AnimationBuilder builder, float[][] keys) {
        // First and last keys are the neutral pose, which pose(...) already writes at those ticks.
        for (int i = 1; i < keys.length - 1; i++) {
            float[] k = keys[i];
            int tick = (int) k[0];
            key(builder.body.pitch, tick, k[1] * ToonFallCurve.BACK, Ease.INOUTSINE);
            key(builder.body.y, tick, k[2], Ease.INOUTSINE);
        }
    }

    /** Starfish pose on the ground with a couple of dazed twitches, between {@code from} and {@code until}. */
    private static void layFlatLimbs(KeyframeAnimation.AnimationBuilder builder, int from, int until) {
        key(builder.rightArm.pitch, from, -0.2f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, from, -0.2f, Ease.INOUTSINE);
        key(builder.rightArm.roll, from, 1.25f, Ease.INOUTSINE);
        key(builder.leftArm.roll, from, -1.25f, Ease.INOUTSINE);
        key(builder.rightLeg.pitch, from, 0.0f, Ease.INOUTSINE);
        key(builder.leftLeg.pitch, from, 0.0f, Ease.INOUTSINE);
        key(builder.head.pitch, from, -0.25f, Ease.INOUTSINE);
        int mid = (from + until) / 2;
        key(builder.rightLeg.pitch, mid - 3, -0.45f, Ease.INOUTSINE); // twitch
        key(builder.rightLeg.pitch, mid, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, mid + 2, -0.7f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, mid + 5, -0.2f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, until, -0.2f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, until, -0.2f, Ease.INOUTSINE);
        key(builder.rightArm.roll, until, 1.25f, Ease.INOUTSINE);
        key(builder.leftArm.roll, until, -1.25f, Ease.INOUTSINE);
        key(builder.head.pitch, until, -0.25f, Ease.INOUTSINE);
    }

    /** Six sickles in 18 ticks: right, left, right... overarm throws with a torso twist. */
    private static KeyframeAnimation gyutaroThrow() {
        int length = 22;
        KeyframeAnimation.AnimationBuilder builder = builder("gyutaro_throw", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        for (int i = 0; i < 6; i++) {
            int t = 3 + i * 3;
            boolean right = i % 2 == 0;
            key(right ? builder.rightArm.pitch : builder.leftArm.pitch, t - 2, -2.6f, Ease.INOUTSINE);
            key(right ? builder.rightArm.pitch : builder.leftArm.pitch, t, -0.9f, Ease.OUTBACK);
            key(builder.torso.yaw, t, right ? -0.25f : 0.25f, Ease.INOUTSINE);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Low guard, both arms sweeping across the body. Exactly as long as Rampant Arc's parry window (25 ticks) and
     * the whirling sickles, so the motion never outlives the visuals; a successful parry replaces it with
     * {@link #gyutaroBurst()}.
     */
    private static KeyframeAnimation gyutaroGuard() {
        int length = 25;
        KeyframeAnimation.AnimationBuilder builder = builder("gyutaro_guard", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.y, 3, -0.12f, Ease.INOUTSINE);
        key(builder.torso.pitch, 3, 0.25f, Ease.INOUTSINE);
        for (int t = 3; t <= 21; t += 3) {
            boolean a = (t / 3) % 2 == 0;
            key(builder.rightArm.pitch, t, -1.4f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, -1.4f, Ease.INOUTSINE);
            key(builder.rightArm.yaw, t, a ? 0.9f : -0.4f, Ease.INOUTSINE);
            key(builder.leftArm.yaw, t, a ? 0.4f : -0.9f, Ease.INOUTSINE);
        }
        key(builder.body.y, 21, -0.12f, Ease.INOUTSINE);
        key(builder.torso.pitch, 21, 0.25f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Successful Rampant Arc parry: arms thrown outward as the sickles explode, then recover. 10 ticks. */
    private static KeyframeAnimation gyutaroBurst() {
        int length = 10;
        KeyframeAnimation.AnimationBuilder builder = builder("gyutaro_burst", length);
        pose(builder, 0, -0.12f, 0.25f, 0.0f);
        key(builder.rightArm.roll, 2, 1.5f, Ease.OUTBACK);
        key(builder.leftArm.roll, 2, -1.5f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 2, -0.3f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 2, -0.3f, Ease.OUTBACK);
        key(builder.torso.pitch, 2, -0.2f, Ease.OUTBACK);
        key(builder.body.y, 2, 0.0f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Arms flung wide and one quick full-body spin as the ring of sickles bursts out. 10 ticks. */
    private static KeyframeAnimation gyutaroSpin() {
        int length = 10;
        KeyframeAnimation.AnimationBuilder builder = builder("gyutaro_spin", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.yaw, 0, 0.0f, Ease.LINEAR);
        key(builder.rightArm.roll, 2, 1.45f, Ease.OUTBACK);
        key(builder.leftArm.roll, 2, -1.45f, Ease.OUTBACK);
        key(builder.body.yaw, 6, (float) (Math.PI * 2.0), Ease.OUTSINE);
        key(builder.rightArm.roll, 6, 1.45f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 6, -1.45f, Ease.INOUTSINE);
        key(builder.body.yaw, length, (float) (Math.PI * 2.0), Ease.LINEAR);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Daki directs each of the eight sashes with an alternating flick of the arm, chin raised. 36 ticks. */
    private static KeyframeAnimation dakiObi() {
        int length = 36;
        KeyframeAnimation.AnimationBuilder builder = builder("daki_obi", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.head.pitch, 2, -0.15f, Ease.INOUTSINE);
        for (int i = 0; i < 8; i++) {
            int strike = (i + 1) * 4;
            boolean right = i % 2 == 0;
            var pitch = right ? builder.rightArm.pitch : builder.leftArm.pitch;
            var yaw = right ? builder.rightArm.yaw : builder.leftArm.yaw;
            key(pitch, strike - 2, -1.9f, Ease.INOUTSINE);
            key(yaw, strike - 2, right ? -0.6f : 0.6f, Ease.INOUTSINE);
            key(pitch, strike, -1.2f, Ease.OUTBACK);
            key(yaw, strike, right ? 0.5f : -0.5f, Ease.OUTBACK);
            key(builder.torso.yaw, strike, right ? 0.18f : -0.18f, Ease.INOUTSINE);
        }
        key(builder.head.pitch, 32, -0.15f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Gyokko presents his art: hands cupped low, then flung forward and up with a flourish. 18 ticks. */
    private static KeyframeAnimation gyokkoVase() {
        int length = 18;
        KeyframeAnimation.AnimationBuilder builder = builder("gyokko_vase", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 4, -0.5f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 4, -0.5f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 4, 0.4f, Ease.INOUTSINE);
        key(builder.leftArm.yaw, 4, -0.4f, Ease.INOUTSINE);
        key(builder.torso.pitch, 4, 0.15f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 9, -2.2f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 9, -2.2f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 9, -0.3f, Ease.OUTBACK);
        key(builder.leftArm.yaw, 9, 0.3f, Ease.OUTBACK);
        key(builder.rightArm.roll, 9, 0.35f, Ease.OUTBACK);
        key(builder.leftArm.roll, 9, -0.35f, Ease.OUTBACK);
        key(builder.torso.pitch, 9, -0.12f, Ease.OUTBACK);
        key(builder.head.pitch, 9, -0.2f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Aizetsu's throw: spear drawn back over the shoulder, body coiled, then hurled forward. 14 ticks. */
    private static KeyframeAnimation hantenguThrust() {
        int length = 14;
        KeyframeAnimation.AnimationBuilder builder = builder("hantengu_thrust", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 3, -2.6f, Ease.OUTSINE);
        key(builder.rightArm.roll, 3, 0.25f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 3, -0.9f, Ease.OUTSINE);
        key(builder.torso.yaw, 3, 0.35f, Ease.OUTSINE);
        key(builder.torso.pitch, 3, -0.1f, Ease.OUTSINE);
        key(builder.rightLeg.pitch, 3, 0.35f, Ease.OUTSINE);
        key(builder.leftLeg.pitch, 3, -0.3f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 6, -1.35f, Ease.OUTBACK);
        key(builder.rightArm.roll, 6, -0.1f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 6, 0.3f, Ease.OUTBACK);
        key(builder.torso.yaw, 6, -0.3f, Ease.OUTBACK);
        key(builder.torso.pitch, 6, 0.18f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 6, -0.25f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 6, 0.35f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Zohakuten beating the drums on his back: both arms raised over the shoulders, alternately swinging down
     * behind him on every strike (the form strikes every 8 ticks). 200 ticks, the length of the form.
     */
    private static KeyframeAnimation hantenguDrum() {
        int length = 200;
        KeyframeAnimation.AnimationBuilder builder = builder("hantengu_drum", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 4, -2.7f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 4, -2.7f, Ease.INOUTSINE);
        key(builder.head.pitch, 4, -0.1f, Ease.INOUTSINE);
        for (int t = 8; t <= length - 8; t += 8) {
            boolean right = (t / 8) % 2 == 1;
            var striking = right ? builder.rightArm.pitch : builder.leftArm.pitch;
            var resting = right ? builder.leftArm.pitch : builder.rightArm.pitch;
            key(striking, t - 3, -3.1f, Ease.INOUTSINE);  // wind back
            key(striking, t, -2.2f, Ease.OUTBACK);        // strike
            key(resting, t, -2.8f, Ease.INOUTSINE);
            key(builder.torso.pitch, t, -0.06f, Ease.OUTBACK);
            key(builder.torso.yaw, t, right ? 0.1f : -0.1f, Ease.OUTBACK);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Crazed Cry of Thunderdeath (240 ticks): drumming over the shoulders while the dragons charge, the head thrown
     * back with arms flung wide for the cry at tick 60, then both arms raised high, conducting the lightning.
     */
    private static KeyframeAnimation hantenguCry() {
        int length = 240;
        KeyframeAnimation.AnimationBuilder builder = builder("hantengu_cry", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        for (int t = 4; t <= 52; t += 6) {
            boolean even = (t / 6) % 2 == 0;
            key(builder.rightArm.pitch, t, even ? -3.0f : -2.3f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, even ? -2.3f : -3.0f, Ease.INOUTSINE);
        }
        key(builder.head.pitch, 52, -0.1f, Ease.INOUTSINE);
        key(builder.torso.pitch, 52, 0.0f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 52, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 52, 0.0f, Ease.INOUTSINE);
        // The cry
        key(builder.head.pitch, 60, -0.75f, Ease.OUTBACK);
        key(builder.torso.pitch, 60, -0.3f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 60, -1.2f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 60, -1.2f, Ease.OUTBACK);
        key(builder.rightArm.roll, 60, 1.2f, Ease.OUTBACK);
        key(builder.leftArm.roll, 60, -1.2f, Ease.OUTBACK);
        key(builder.head.pitch, 76, -0.7f, Ease.INOUTSINE);
        key(builder.torso.pitch, 76, -0.28f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 76, 1.15f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 76, -1.15f, Ease.INOUTSINE);
        // Conducting the storm: arms high, swaying
        for (int t = 84; t <= length - 8; t += 10) {
            boolean even = (t / 10) % 2 == 0;
            key(builder.head.pitch, t, -0.3f, Ease.INOUTSINE);
            key(builder.torso.pitch, t, -0.1f, Ease.INOUTSINE);
            key(builder.rightArm.pitch, t, even ? -2.9f : -2.5f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, even ? -2.5f : -2.9f, Ease.INOUTSINE);
            key(builder.rightArm.roll, t, 0.35f, Ease.INOUTSINE);
            key(builder.leftArm.roll, t, -0.35f, Ease.INOUTSINE);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Compressed Sound Waves: braced lunge forward, left arm pointing down the line of the blast. 16 ticks. */
    private static KeyframeAnimation hantenguSound() {
        int length = 16;
        KeyframeAnimation.AnimationBuilder builder = builder("hantengu_sound", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.leftArm.pitch, 3, -1.6f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 3, 0.4f, Ease.OUTBACK);
        key(builder.torso.pitch, 3, 0.2f, Ease.OUTBACK);
        key(builder.head.pitch, 3, -0.2f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 3, 0.4f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 3, -0.35f, Ease.OUTBACK);
        key(builder.body.y, 3, -0.06f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 10, -1.55f, Ease.INOUTSINE);
        key(builder.torso.pitch, 10, 0.16f, Ease.INOUTSINE);
        key(builder.head.pitch, 10, -0.2f, Ease.INOUTSINE);
        key(builder.rightLeg.pitch, 10, 0.4f, Ease.INOUTSINE);
        key(builder.leftLeg.pitch, 10, -0.35f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Akaza's fighting stance (fists up, guard forward) at {@code tick}. */
    private static void akazaStance(KeyframeAnimation.AnimationBuilder builder, int tick) {
        key(builder.rightArm.pitch, tick, -1.1f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, tick, -1.3f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, tick, -0.2f, Ease.INOUTSINE);
        key(builder.leftArm.yaw, tick, 0.3f, Ease.INOUTSINE);
        key(builder.torso.yaw, tick, 0.15f, Ease.INOUTSINE);
        key(builder.rightLeg.pitch, tick, 0.25f, Ease.INOUTSINE);
        key(builder.leftLeg.pitch, tick, -0.25f, Ease.INOUTSINE);
    }

    /** Straight right: guard, then the arm snaps out with the torso turning into it. 8 ticks. */
    private static KeyframeAnimation akazaPunch() {
        int length = 8;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_punch", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 1, -1.0f, Ease.OUTSINE);
        key(builder.torso.yaw, 1, 0.3f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 3, -1.6f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 3, 0.1f, Ease.OUTBACK);
        key(builder.torso.yaw, 3, -0.35f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 3, -0.6f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 3, 0.3f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 3, -0.3f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Annihilation Type charge: a deep crouch with both arms drawn back at the sides of the body, palms open and
     * poised to strike; held (400 ticks) until the release replaces it.
     */
    private static KeyframeAnimation akazaCharge() {
        int length = 400;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_charge", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        for (int t = 6; t <= length; t += 10) {
            float tremble = (t / 10) % 2 == 0 ? 0.02f : -0.02f;
            key(builder.body.y, t, -0.22f, Ease.INOUTSINE);
            key(builder.torso.pitch, t, 0.3f, Ease.INOUTSINE);
            key(builder.head.pitch, t, -0.2f, Ease.INOUTSINE);
            key(builder.rightArm.pitch, t, 0.55f + tremble, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, 0.55f - tremble, Ease.INOUTSINE);
            key(builder.rightArm.roll, t, 0.3f, Ease.INOUTSINE);
            key(builder.leftArm.roll, t, -0.3f, Ease.INOUTSINE);
            key(builder.rightLeg.pitch, t, 0.6f, Ease.INOUTSINE);
            key(builder.leftLeg.pitch, t, -0.6f, Ease.INOUTSINE);
        }
        return builder.build();
    }

    /** The lunge: both palms driven forward together, held through the dash. 15 ticks. */
    private static KeyframeAnimation akazaDash() {
        int length = 15;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_dash", length);
        key(builder.body.y, 0, -0.22f, Ease.LINEAR);
        key(builder.rightArm.pitch, 0, 0.55f, Ease.LINEAR);
        key(builder.leftArm.pitch, 0, 0.55f, Ease.LINEAR);
        key(builder.torso.pitch, 2, 0.3f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 2, -1.6f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 2, -1.6f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 2, 0.15f, Ease.OUTBACK);
        key(builder.leftArm.yaw, 2, -0.15f, Ease.OUTBACK);
        key(builder.body.y, 2, -0.08f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 2, 0.6f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 2, -0.5f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 12, -1.55f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 12, -1.55f, Ease.INOUTSINE);
        key(builder.torso.pitch, 12, 0.28f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Crown Splitter, a reverse rising axe kick: Akaza spins away, his right heel rising from low behind him up
     * over his head (ticks 0-3), then chops down onto the target's crown at tick 4 (when the arc lands).
     */
    private static KeyframeAnimation akazaUpperkick() {
        int length = 12;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_upperkick", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.yaw, 0, 0.0f, Ease.LINEAR);
        key(builder.body.yaw, 3, (float) (Math.PI * 2.0), Ease.OUTSINE);   // reverse spin
        key(builder.rightLeg.pitch, 1, 0.6f, Ease.OUTSINE);                  // heel swings back and low
        key(builder.rightLeg.pitch, 3, -3.0f, Ease.OUTSINE);                 // up past the head
        key(builder.rightLeg.roll, 3, 0.25f, Ease.OUTSINE);
        key(builder.torso.pitch, 3, -0.35f, Ease.OUTSINE);
        key(builder.rightArm.roll, 3, 0.7f, Ease.OUTSINE);
        key(builder.leftArm.roll, 3, -0.7f, Ease.OUTSINE);
        key(builder.rightLeg.pitch, 5, -0.9f, Ease.OUTBACK);                 // axe down onto the crown
        key(builder.rightLeg.roll, 5, 0.0f, Ease.OUTBACK);
        key(builder.torso.pitch, 5, 0.35f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 5, 0.5f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 5, 0.5f, Ease.OUTBACK);
        key(builder.body.yaw, length, (float) (Math.PI * 2.0), Ease.LINEAR); // a full turn reads as 0
        key(builder.rightLeg.pitch, length, 0.0f, Ease.INOUTSINE);
        key(builder.torso.pitch, length, 0.0f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, length, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, length, 0.0f, Ease.INOUTSINE);
        key(builder.rightArm.roll, length, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.roll, length, 0.0f, Ease.INOUTSINE);
        return builder.build();
    }

    /** Alternating punches every {@code every} ticks for {@code length} ticks, from a stance. */
    private static KeyframeAnimation akazaPunches(String name, int length, int every, float reach, float twist) {
        KeyframeAnimation.AnimationBuilder builder = builder(name, length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        akazaStance(builder, 2);
        int first = every * ((4 + every - 1) / every); // first punch after the stance has settled
        for (int t = first; t <= length - 2; t += every) {
            boolean right = (t / every) % 2 == 1;
            var out = right ? builder.rightArm.pitch : builder.leftArm.pitch;
            var back = right ? builder.leftArm.pitch : builder.rightArm.pitch;
            key(out, t, reach, Ease.OUTBACK);
            key(back, t, -0.9f, Ease.INOUTSINE);
            key(builder.torso.yaw, t, right ? -twist : twist, Ease.OUTBACK);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Explosive Flurry: five straight kicks, each swept along the restricted random arc that
     * {@link AkazaKicks#kick} builds from the shared seed (so the leg follows the arc the shockwave is drawn on):
     * chamber, extend onto the arc's start, sweep to its end on the strike tick, snap back.
     */
    private static KeyframeAnimation akazaFlurry(int seed) {
        int length = AkazaKicks.KICKS * AkazaKicks.INTERVAL + 3;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_flurry", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightLeg.yaw, 0, 0.0f, Ease.LINEAR);
        key(builder.leftLeg.yaw, 0, 0.0f, Ease.LINEAR);
        key(builder.rightArm.pitch, 1, -1.0f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 1, -1.0f, Ease.INOUTSINE);
        for (int k = 1; k <= AkazaKicks.KICKS; k++) {
            AkazaKicks.Kick kick = AkazaKicks.kick(seed, k);
            int strike = AkazaKicks.strikeTick(k);
            var pitch = kick.rightLeg() ? builder.rightLeg.pitch : builder.leftLeg.pitch;
            var yaw = kick.rightLeg() ? builder.rightLeg.yaw : builder.leftLeg.yaw;
            var otherPitch = kick.rightLeg() ? builder.leftLeg.pitch : builder.rightLeg.pitch;
            key(pitch, strike - 1, kick.startPitch(), Ease.OUTSINE);
            key(yaw, strike - 1, kick.startYaw(), Ease.OUTSINE);
            key(otherPitch, strike - 1, 0.15f, Ease.INOUTSINE);
            key(pitch, strike, kick.endPitch(), Ease.LINEAR);
            key(yaw, strike, kick.endYaw(), Ease.LINEAR);
            key(builder.torso.yaw, strike, kick.rightLeg() ? 0.3f : -0.3f, Ease.OUTBACK);
            key(builder.torso.pitch, strike, -0.15f, Ease.OUTBACK);
            // snap back (the next kick's chamber starts from here)
            key(pitch, strike + 1, -0.3f, Ease.INOUTSINE);
            key(yaw, strike + 1, 0.0f, Ease.INOUTSINE);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        key(builder.rightLeg.yaw, length, 0.0f, Ease.LINEAR);
        key(builder.leftLeg.yaw, length, 0.0f, Ease.LINEAR);
        return builder.build();
    }

    /**
     * Blue Silver Chaotic Afterglow (30 ticks): the Compass Needle stomp, then punching and kicking at nothing in
     * every direction, faster and faster, torso whipping side to side; at tick 25 (the release) arms and a leg
     * flung out in a final explosive burst.
     */
    private static KeyframeAnimation akazaFinal() {
        int length = 30;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_final", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightLeg.pitch, 2, -0.9f, Ease.OUTSINE);               // stomp
        key(builder.rightLeg.pitch, 4, 0.1f, Ease.OUTBACK);
        key(builder.body.y, 4, -0.12f, Ease.OUTBACK);
        int t = 6;
        int i = 0;
        while (t < 24) {
            boolean kick = i % 4 == 3;
            boolean right = i % 2 == 0;
            float yawSwing = (float) Math.sin(i * 2.1) * 0.7f;
            float aimPitch = -1.0f - (float) ((i * 0.37) % 0.9);
            if (kick) {
                key(right ? builder.rightLeg.pitch : builder.leftLeg.pitch, t, -1.7f, Ease.OUTBACK);
                key(right ? builder.leftLeg.pitch : builder.rightLeg.pitch, t, 0.2f, Ease.INOUTSINE);
            } else {
                key(right ? builder.rightArm.pitch : builder.leftArm.pitch, t, aimPitch - 0.5f, Ease.OUTBACK);
                key(right ? builder.leftArm.pitch : builder.rightArm.pitch, t, -0.8f, Ease.INOUTSINE);
                key(builder.rightLeg.pitch, t, 0.25f, Ease.INOUTSINE);
                key(builder.leftLeg.pitch, t, -0.25f, Ease.INOUTSINE);
            }
            key(builder.torso.yaw, t, yawSwing, Ease.OUTBACK);
            key(builder.head.pitch, t, (float) Math.cos(i * 1.3) * 0.3f, Ease.OUTBACK);
            t += i < 3 ? 3 : 2;
            i++;
        }
        key(builder.torso.yaw, 25, 0.0f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 25, -1.5f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 25, -1.5f, Ease.OUTBACK);
        key(builder.rightArm.roll, 25, 1.1f, Ease.OUTBACK);
        key(builder.leftArm.roll, 25, -1.1f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 25, -0.4f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 25, 0.2f, Ease.OUTBACK);
        key(builder.head.pitch, 25, -0.35f, Ease.OUTBACK);
        key(builder.body.y, 25, 0.0f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /**
     * Flying Planet Thousand Wheels: one flowing, upward straight kick: the right leg swings from behind, up
     * through straight ahead to straight up (tick 4), the body rising with it, arms flowing back. 12 ticks.
     */
    private static KeyframeAnimation akazaWheel() {
        int length = 12;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_wheel", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightLeg.pitch, 1, 0.5f, Ease.OUTSINE);
        key(builder.rightLeg.pitch, 4, -3.0f, Ease.OUTSINE);
        key(builder.leftLeg.pitch, 4, 0.35f, Ease.OUTSINE);
        key(builder.torso.pitch, 4, -0.3f, Ease.OUTSINE);
        key(builder.head.pitch, 4, -0.35f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 4, 0.9f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 4, 0.9f, Ease.OUTSINE);
        key(builder.rightArm.roll, 4, 0.35f, Ease.OUTSINE);
        key(builder.leftArm.roll, 4, -0.35f, Ease.OUTSINE);
        key(builder.body.y, 4, 0.12f, Ease.OUTSINE);
        key(builder.rightLeg.pitch, 7, -2.8f, Ease.INOUTSINE);
        key(builder.torso.pitch, 7, -0.25f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Ten Thousand Leaves Willow: drop into a crouch and drive the right fist into the ground. 20 ticks. */
    private static KeyframeAnimation akazaWillow() {
        int length = 20;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_willow", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 1, -2.8f, Ease.OUTSINE);
        key(builder.body.y, 1, 0.05f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 3, -0.5f, Ease.OUTBACK);
        key(builder.body.y, 3, -0.35f, Ease.OUTBACK);
        key(builder.torso.pitch, 3, 0.6f, Ease.OUTBACK);
        key(builder.head.pitch, 3, -0.3f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 3, -0.3f, Ease.OUTBACK);
        key(builder.leftArm.roll, 3, -0.5f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 3, -0.9f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 3, 0.6f, Ease.OUTBACK);
        key(builder.body.y, 16, -0.32f, Ease.INOUTSINE);
        key(builder.torso.pitch, 16, 0.55f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 16, -0.5f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Compass Needle: a sharp stomp from the fighting stance that unfolds the snowflake. 20 ticks. */
    private static KeyframeAnimation akazaCompass() {
        int length = 20;
        KeyframeAnimation.AnimationBuilder builder = builder("akaza_compass", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        akazaStance(builder, 3);
        key(builder.rightLeg.pitch, 5, -0.9f, Ease.OUTSINE);
        key(builder.body.y, 5, 0.04f, Ease.OUTSINE);
        key(builder.rightLeg.pitch, 7, 0.1f, Ease.OUTBACK);
        key(builder.body.y, 7, -0.12f, Ease.OUTBACK);
        akazaStance(builder, 16);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Frozen Lotus: right fan drawn across to the left, then swept wide across the front to the right. 12 ticks. */
    private static KeyframeAnimation domaSweep() {
        int length = 12;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_sweep", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 2, -1.5f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 2, 0.9f, Ease.OUTSINE);
        key(builder.torso.yaw, 2, 0.35f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 5, -1.45f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 5, -0.9f, Ease.OUTSINE);
        key(builder.torso.yaw, 5, -0.35f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 5, -0.4f, Ease.OUTSINE);
        key(builder.leftArm.roll, 5, -0.5f, Ease.OUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Barren Hanging Garden: arms spread with both fans open while he turns a full circle. 16 ticks. */
    private static KeyframeAnimation domaSpin() {
        int length = 16;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_spin", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.yaw, 0, 0.0f, Ease.LINEAR);
        key(builder.rightArm.roll, 2, 1.4f, Ease.OUTSINE);
        key(builder.leftArm.roll, 2, -1.4f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 2, -0.3f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 2, -0.3f, Ease.OUTSINE);
        key(builder.body.yaw, 15, (float) (Math.PI * 2.0), Ease.INOUTSINE);
        key(builder.rightArm.roll, 14, 1.35f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 14, -1.35f, Ease.INOUTSINE);
        key(builder.body.yaw, length, (float) (Math.PI * 2.0), Ease.LINEAR);
        key(builder.rightArm.roll, length, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.roll, length, 0.0f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, length, 0.0f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, length, 0.0f, Ease.INOUTSINE);
        return builder.build();
    }

    /** Freezing Clouds: both fans fanned forward again and again, driving the cloud. 40 ticks. */
    private static KeyframeAnimation domaWave() {
        int length = 40;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_wave", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        for (int t = 3; t <= length - 4; t += 6) {
            key(builder.rightArm.pitch, t, -2.0f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, -2.0f, Ease.INOUTSINE);
            key(builder.rightArm.pitch, t + 3, -1.1f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t + 3, -1.1f, Ease.INOUTSINE);
            key(builder.torso.pitch, t, -0.05f, Ease.INOUTSINE);
            key(builder.torso.pitch, t + 3, 0.12f, Ease.INOUTSINE);
        }
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Lotus Vines / Wintry Icicles: fan raised overhead, then pointed at the target and held. 16 ticks. */
    private static KeyframeAnimation domaPoint() {
        int length = 16;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_point", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 3, -2.8f, Ease.OUTSINE);
        key(builder.head.pitch, 3, -0.15f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 6, -1.5f, Ease.OUTBACK);
        key(builder.head.pitch, 6, 0.05f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 6, -0.3f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 13, -1.45f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Scattering Lotuses: both fans drawn in, then flicked outward and forward. 10 ticks. */
    private static KeyframeAnimation domaFlick() {
        int length = 10;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_flick", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 2, -1.2f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 2, -1.2f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 2, 0.6f, Ease.OUTSINE);
        key(builder.leftArm.yaw, 2, -0.6f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 4, -1.6f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 4, -1.6f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 4, -0.5f, Ease.OUTBACK);
        key(builder.leftArm.yaw, 4, 0.5f, Ease.OUTBACK);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** A graceful presentation: both fans opened wide to the sides, head tilted with a smile. 24 ticks. */
    private static KeyframeAnimation domaSummon() {
        int length = 24;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_summon", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 5, -0.9f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 5, -0.9f, Ease.OUTSINE);
        key(builder.rightArm.roll, 5, 0.9f, Ease.OUTSINE);
        key(builder.leftArm.roll, 5, -0.9f, Ease.OUTSINE);
        key(builder.torso.roll, 5, 0.06f, Ease.OUTSINE);
        key(builder.rightArm.roll, 18, 0.95f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 18, -0.95f, Ease.INOUTSINE);
        key(builder.torso.roll, 18, 0.06f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Crystalline Divine Child charge: fans crossed before the face, swaying gently; held (400 ticks). */
    private static KeyframeAnimation domaCharge() {
        int length = 400;
        KeyframeAnimation.AnimationBuilder builder = builder("doma_charge", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        for (int t = 5; t <= length; t += 20) {
            boolean even = (t / 20) % 2 == 0;
            key(builder.rightArm.pitch, t, -1.7f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, -1.7f, Ease.INOUTSINE);
            key(builder.rightArm.yaw, t, 0.55f, Ease.INOUTSINE);
            key(builder.leftArm.yaw, t, -0.55f, Ease.INOUTSINE);
            key(builder.torso.roll, t, even ? 0.05f : -0.05f, Ease.INOUTSINE);
        }
        return builder.build();
    }

    /** Enmu raises his right hand, palm (and the mouth on it) toward the target, then lowers it. */
    private static KeyframeAnimation enmuWhisper() {
        int length = 28;
        KeyframeAnimation.AnimationBuilder builder = builder("enmu_whisper", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 4, -1.65f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 4, 0.15f, Ease.INOUTSINE);
        key(builder.head.pitch, 4, 0.08f, Ease.INOUTSINE);
        key(builder.torso.yaw, 4, -0.12f, Ease.INOUTSINE);
        key(builder.rightArm.pitch, 20, -1.6f, Ease.INOUTSINE);
        key(builder.rightArm.yaw, 20, 0.15f, Ease.INOUTSINE);
        key(builder.head.pitch, 20, 0.08f, Ease.INOUTSINE);
        key(builder.torso.yaw, 20, -0.12f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Enmu spreads his arms wide, presenting the Mother Eye, then settles. */
    private static KeyframeAnimation enmuEyes() {
        int length = 30;
        KeyframeAnimation.AnimationBuilder builder = builder("enmu_eyes", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 6, -0.4f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 6, -0.4f, Ease.OUTBACK);
        key(builder.rightArm.roll, 6, 1.35f, Ease.OUTBACK);
        key(builder.leftArm.roll, 6, -1.35f, Ease.OUTBACK);
        key(builder.head.pitch, 6, -0.25f, Ease.OUTBACK);
        key(builder.torso.pitch, 6, -0.12f, Ease.OUTBACK);
        key(builder.rightArm.roll, 22, 1.3f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 22, -1.3f, Ease.INOUTSINE);
        key(builder.head.pitch, 22, -0.25f, Ease.INOUTSINE);
        key(builder.torso.pitch, 22, -0.12f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    /** Asleep on your feet: head drooping and bobbing, arms hanging forward, a slight sway. Looped. */
    private static KeyframeAnimation enmuSlumber() {
        int length = 60;
        KeyframeAnimation.AnimationBuilder builder = builder("enmu_slumber", length);
        builder.isLooped = true;
        builder.returnTick = 0;
        for (int t = 0; t <= length; t += 30) {
            boolean down = t == 30;
            key(builder.head.pitch, t, down ? 0.75f : 0.6f, Ease.INOUTSINE);
            key(builder.head.z, t, 0.0f, Ease.INOUTSINE);
            key(builder.torso.pitch, t, down ? 0.22f : 0.16f, Ease.INOUTSINE);
            key(builder.torso.roll, t, down ? 0.06f : -0.06f, Ease.INOUTSINE);
            key(builder.torso.yaw, t, 0.0f, Ease.INOUTSINE);
            key(builder.body.y, t, -0.08f, Ease.INOUTSINE);
            key(builder.body.pitch, t, 0.0f, Ease.INOUTSINE);
            key(builder.rightArm.pitch, t, down ? 0.35f : 0.25f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, down ? 0.25f : 0.35f, Ease.INOUTSINE);
            key(builder.rightArm.yaw, t, 0.0f, Ease.INOUTSINE);
            key(builder.leftArm.yaw, t, 0.0f, Ease.INOUTSINE);
            key(builder.rightArm.roll, t, 0.08f, Ease.INOUTSINE);
            key(builder.leftArm.roll, t, -0.08f, Ease.INOUTSINE);
            key(builder.rightLeg.pitch, t, 0.0f, Ease.INOUTSINE);
            key(builder.leftLeg.pitch, t, 0.0f, Ease.INOUTSINE);
        }
        return builder.build();
    }

    /** Rui weaving the Eye Basket: both arms raised forward and spread, fingers working the threads. */
    private static KeyframeAnimation ruiCradle() {
        int length = 56;
        KeyframeAnimation.AnimationBuilder builder = builder("rui_cradle", length);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 4, -1.45f, Ease.INOUTSINE);
        key(builder.leftArm.pitch, 4, -1.45f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 4, 0.22f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 4, -0.22f, Ease.INOUTSINE);
        key(builder.torso.pitch, 4, -0.06f, Ease.INOUTSINE);
        key(builder.head.pitch, 4, 0.12f, Ease.INOUTSINE);
        // Fingers "working" the cradle: small alternating dips of each arm.
        for (int t = 10; t <= 46; t += 6) {
            boolean even = (t / 6) % 2 == 0;
            key(builder.rightArm.pitch, t, even ? -1.38f : -1.52f, Ease.INOUTSINE);
            key(builder.leftArm.pitch, t, even ? -1.52f : -1.38f, Ease.INOUTSINE);
        }
        // Final pull: hands snap apart as the sphere crushes.
        key(builder.rightArm.roll, 46, 0.22f, Ease.INOUTSINE);
        key(builder.leftArm.roll, 46, -0.22f, Ease.INOUTSINE);
        key(builder.rightArm.roll, 50, 0.6f, Ease.OUTBACK);
        key(builder.leftArm.roll, 50, -0.6f, Ease.OUTBACK);
        key(builder.torso.pitch, 50, -0.06f, Ease.INOUTSINE);
        key(builder.head.pitch, 50, 0.12f, Ease.INOUTSINE);
        pose(builder, length, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    private static KeyframeAnimation pieSplat() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_pie_splat", 16);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.rightArm.pitch, 3, -1.55f, Ease.OUTSINE);
        key(builder.rightArm.yaw, 3, -0.7f, Ease.OUTSINE);
        key(builder.leftArm.pitch, 3, -0.65f, Ease.OUTSINE);
        key(builder.torso.yaw, 3, -0.25f, Ease.OUTSINE);
        key(builder.rightArm.pitch, 7, -0.15f, Ease.OUTBACK);
        key(builder.rightArm.yaw, 7, 0.8f, Ease.OUTBACK);
        key(builder.torso.yaw, 7, 0.2f, Ease.OUTBACK);
        key(builder.body.y, 8, 0.08f, Ease.OUTBACK);
        pose(builder, 16, 0.0f, 0.0f, 0.0f);
        return builder.build();
    }

    static KeyframeAnimation.AnimationBuilder builder(String name, int durationTicks) {
        KeyframeAnimation.AnimationBuilder builder = new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_MC_ANIM);
        builder.beginTick = 0;
        builder.endTick = durationTicks;
        builder.stopTick = durationTicks;
        builder.isLooped = false;
        builder.returnTick = 0;
        builder.setName(name);
        return builder;
    }

    static void pose(KeyframeAnimation.AnimationBuilder builder, int tick, float bodyY, float torsoPitch, float headPitch) {
        key(builder.body.y, tick, bodyY, Ease.LINEAR);
        key(builder.body.pitch, tick, 0.0f, Ease.LINEAR);
        key(builder.torso.pitch, tick, torsoPitch, Ease.LINEAR);
        key(builder.head.pitch, tick, headPitch, Ease.LINEAR);
        key(builder.head.z, tick, 0.0f, Ease.LINEAR);
        key(builder.rightArm.pitch, tick, 0.0f, Ease.LINEAR);
        key(builder.rightArm.yaw, tick, 0.0f, Ease.LINEAR);
        key(builder.rightArm.roll, tick, 0.0f, Ease.LINEAR);
        key(builder.leftArm.pitch, tick, 0.0f, Ease.LINEAR);
        key(builder.leftArm.yaw, tick, 0.0f, Ease.LINEAR);
        key(builder.leftArm.roll, tick, 0.0f, Ease.LINEAR);
        key(builder.rightLeg.pitch, tick, 0.0f, Ease.LINEAR);
        key(builder.leftLeg.pitch, tick, 0.0f, Ease.LINEAR);
        key(builder.torso.yaw, tick, 0.0f, Ease.LINEAR);
        key(builder.torso.roll, tick, 0.0f, Ease.LINEAR);
    }

    static void key(KeyframeAnimation.StateCollection.State state, int tick, float value, Ease ease) {
        state.setEnabled(true);
        state.addKeyFrame(tick, value, ease);
    }
}
