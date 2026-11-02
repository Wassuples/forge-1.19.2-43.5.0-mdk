package com.wassuples.demonslayer.client.animation;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.custom.HashiraSwordHelper;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.item.custom.SwordModel;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

import static com.wassuples.demonslayer.client.animation.ClientFormAnimations.builder;
import static com.wassuples.demonslayer.client.animation.ClientFormAnimations.key;
import static com.wassuples.demonslayer.client.animation.ClientFormAnimations.pose;

/**
 * Attack animations for Nichirin weapons. Every client watches every player's arm swing (vanilla already syncs it),
 * so no packets are needed: when a swing starts with a Nichirin weapon in the main hand, the matching animation plays.
 * Form/BDA animations always take priority; a new swing may interrupt the previous swing.
 *
 * <ul>
 *   <li>Giyu (Water): flowing horizontal arcs, alternating with a diagonal cut.</li>
 *   <li>Sanemi (Wind): a wild, fast, one-handed sweep with the body thrown into it.</li>
 *   <li>Rengoku (Flame): a big two-handed overhead cut with a stamping lunge.</li>
 *   <li>Muichiro (Mist): light, quick alternating cuts.</li>
 *   <li>Kanae (Flower): a spinning, graceful cut.</li>
 *   <li>Jigoro (Thunder): crouched iai draw from the left hip, then a flash across.</li>
 *   <li>Shinobu (Insect): a lunging stab.</li>
 *   <li>Mitsuri (Love): the arm circles back over the head, then flicks forward and snaps (the whip cracks).</li>
 *   <li>Obanai (Serpent): a low cut that weaves side to side.</li>
 *   <li>Gyomei (Stone): both arms heave the flail back over the head and hurl it forward.</li>
 *   <li>Tengen (Sound): both cleavers raised outward, then crossed through in an X.</li>
 *   <li>Plain Nichirin blade: alternating diagonal / rising cuts.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponSwingAnimations {
    private static final Map<Integer, Boolean> WAS_SWINGING = new HashMap<>();
    private static final Map<Integer, Integer> COMBO = new HashMap<>();

    private WeaponSwingAnimations() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (Player p : mc.level.players()) {
            if (!(p instanceof AbstractClientPlayer player)) continue;
            boolean swinging = player.swinging && player.swingingArm == InteractionHand.MAIN_HAND;
            boolean was = WAS_SWINGING.getOrDefault(player.getId(), false);
            WAS_SWINGING.put(player.getId(), swinging);
            if (!swinging || was) continue;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof NichirinSwordItem)) continue;
            int combo = COMBO.merge(player.getId(), 1, Integer::sum);
            KeyframeAnimation anim = forWeapon(HashiraSwordHelper.getModelFromStack(held), combo);
            ClientFormAnimations.playWeaponSwing(player, anim);
        }
        if (WAS_SWINGING.size() > 256) {
            WAS_SWINGING.clear();
            COMBO.clear();
        }
    }

    static KeyframeAnimation forWeapon(SwordModel model, int combo) {
        boolean alt = (combo & 1) == 0;
        if (model == null) return alt ? rising() : diagonal();
        return switch (model) {
            case WATER -> alt ? diagonal() : waterFlow();
            case WIND -> windSweep();
            case FLAME -> flameOverhead();
            case MIST -> alt ? mistQuick(true) : mistQuick(false);
            case FLOWER -> flowerTwirl();
            case THUNDER -> thunderIai();
            case INSECT -> insectThrust();
            case LOVE -> loveWhip();
            case SNAKE -> serpentWeave();
            case STONE -> stoneHurl();
            case SOUND -> soundCross();
            default -> alt ? rising() : diagonal();
        };
    }

    // ------------------------------------------------------------------ helpers

    /** Left hand joins the grip: follows the right arm, angled in toward it. */
    private static void bothHands(KeyframeAnimation.AnimationBuilder b, int t, float pitch, float yaw, Ease e) {
        key(b.rightArm.pitch, t, pitch, e);
        key(b.rightArm.yaw, t, yaw, e);
        key(b.leftArm.pitch, t, pitch + 0.15f, e);
        key(b.leftArm.yaw, t, yaw + 0.45f, e);
    }

    // ------------------------------------------------------------------ standard katana cuts

    private static KeyframeAnimation diagonal() {
        int len = 11;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_diagonal", len);
        pose(b, 0, 0f, 0f, 0f);
        bothHands(b, 2, -2.6f, 0.45f, Ease.OUTSINE);
        key(b.torso.yaw, 2, 0.35f, Ease.OUTSINE);
        bothHands(b, 5, -0.55f, -0.65f, Ease.OUTSINE);
        key(b.torso.yaw, 5, -0.4f, Ease.OUTSINE);
        key(b.torso.pitch, 5, 0.15f, Ease.OUTSINE);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation rising() {
        int len = 11;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_rising", len);
        pose(b, 0, 0f, 0f, 0f);
        bothHands(b, 2, -0.4f, -0.7f, Ease.OUTSINE);
        key(b.torso.yaw, 2, -0.35f, Ease.OUTSINE);
        bothHands(b, 5, -2.4f, 0.6f, Ease.OUTSINE);
        key(b.torso.yaw, 5, 0.4f, Ease.OUTSINE);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    // ------------------------------------------------------------------ per Hashira

    private static KeyframeAnimation waterFlow() {
        int len = 12;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_water", len);
        pose(b, 0, 0f, 0f, 0f);
        bothHands(b, 2, -1.45f, 1.05f, Ease.INOUTSINE);
        key(b.torso.yaw, 2, 0.55f, Ease.INOUTSINE);
        key(b.body.y, 3, -0.06f, Ease.INOUTSINE);
        bothHands(b, 7, -1.35f, -1.05f, Ease.INOUTSINE);
        key(b.torso.yaw, 7, -0.6f, Ease.INOUTSINE);
        key(b.rightLeg.pitch, 7, -0.25f, Ease.INOUTSINE);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation windSweep() {
        int len = 10;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_wind", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.rightArm.pitch, 1, -1.5f, Ease.OUTSINE);
        key(b.rightArm.yaw, 1, 1.35f, Ease.OUTSINE);
        key(b.torso.yaw, 1, 0.6f, Ease.OUTSINE);
        key(b.leftArm.roll, 1, -0.5f, Ease.OUTSINE);
        key(b.rightArm.pitch, 4, -1.45f, Ease.OUTBACK);
        key(b.rightArm.yaw, 4, -1.45f, Ease.OUTBACK);
        key(b.torso.yaw, 4, -0.7f, Ease.OUTBACK);
        key(b.torso.pitch, 4, 0.25f, Ease.OUTBACK);
        key(b.leftArm.pitch, 4, 0.5f, Ease.OUTBACK);
        key(b.leftArm.roll, 4, -0.9f, Ease.OUTBACK);
        key(b.leftLeg.pitch, 4, -0.35f, Ease.OUTBACK);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation flameOverhead() {
        int len = 14;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_flame", len);
        pose(b, 0, 0f, 0f, 0f);
        bothHands(b, 3, -3.0f, 0.05f, Ease.OUTSINE);
        key(b.torso.pitch, 3, -0.18f, Ease.OUTSINE);
        key(b.head.pitch, 3, -0.15f, Ease.OUTSINE);
        bothHands(b, 6, -0.45f, -0.1f, Ease.OUTBACK);
        key(b.torso.pitch, 6, 0.38f, Ease.OUTBACK);
        key(b.head.pitch, 6, 0.1f, Ease.OUTBACK);
        key(b.rightLeg.pitch, 6, -0.55f, Ease.OUTBACK);
        key(b.leftLeg.pitch, 6, 0.35f, Ease.OUTBACK);
        key(b.body.y, 6, -0.1f, Ease.OUTBACK);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation mistQuick(boolean left) {
        int len = 8;
        float s = left ? -1f : 1f;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_mist", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.rightArm.pitch, 1, -1.7f, Ease.OUTSINE);
        key(b.rightArm.yaw, 1, 0.7f * s, Ease.OUTSINE);
        key(b.rightArm.pitch, 3, -1.1f, Ease.OUTSINE);
        key(b.rightArm.yaw, 3, -0.8f * s, Ease.OUTSINE);
        key(b.torso.yaw, 3, -0.3f * s, Ease.OUTSINE);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation flowerTwirl() {
        int len = 14;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_flower", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.body.yaw, 0, 0f, Ease.LINEAR);
        key(b.rightArm.pitch, 2, -1.5f, Ease.OUTSINE);
        key(b.rightArm.roll, 2, 0.9f, Ease.OUTSINE);
        key(b.leftArm.roll, 2, -0.8f, Ease.OUTSINE);
        key(b.body.yaw, 10, (float) (Math.PI * 2), Ease.INOUTSINE);
        key(b.rightArm.pitch, 10, -1.4f, Ease.INOUTSINE);
        key(b.rightArm.roll, 10, 0.2f, Ease.INOUTSINE);
        key(b.body.yaw, len, (float) (Math.PI * 2), Ease.LINEAR);
        key(b.rightArm.pitch, len, 0f, Ease.INOUTSINE);
        key(b.rightArm.roll, len, 0f, Ease.INOUTSINE);
        key(b.leftArm.roll, len, 0f, Ease.INOUTSINE);
        return b.build();
    }

    private static KeyframeAnimation thunderIai() {
        int len = 13;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_thunder", len);
        pose(b, 0, 0f, 0f, 0f);
        // crouch, hand at the left hip
        key(b.body.y, 2, -0.22f, Ease.OUTSINE);
        key(b.torso.pitch, 2, 0.3f, Ease.OUTSINE);
        key(b.rightArm.pitch, 2, -0.35f, Ease.OUTSINE);
        key(b.rightArm.yaw, 2, 1.1f, Ease.OUTSINE);
        key(b.leftArm.pitch, 2, -0.4f, Ease.OUTSINE);
        key(b.leftArm.yaw, 2, 0.6f, Ease.OUTSINE);
        key(b.rightLeg.pitch, 2, -0.6f, Ease.OUTSINE);
        key(b.leftLeg.pitch, 2, 0.45f, Ease.OUTSINE);
        key(b.torso.yaw, 2, 0.45f, Ease.OUTSINE);
        // the flash
        key(b.rightArm.pitch, 4, -1.55f, Ease.OUTBACK);
        key(b.rightArm.yaw, 4, -1.35f, Ease.OUTBACK);
        key(b.torso.yaw, 4, -0.65f, Ease.OUTBACK);
        key(b.leftArm.yaw, 4, -0.2f, Ease.OUTBACK);
        key(b.rightArm.pitch, 7, -1.5f, Ease.LINEAR);
        key(b.body.y, 7, -0.22f, Ease.LINEAR);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation insectThrust() {
        int len = 10;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_insect", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.rightArm.pitch, 2, -1.2f, Ease.OUTSINE);
        key(b.rightArm.yaw, 2, 0.35f, Ease.OUTSINE);
        key(b.torso.yaw, 2, 0.35f, Ease.OUTSINE);
        key(b.rightArm.pitch, 4, -1.62f, Ease.OUTBACK);
        key(b.rightArm.yaw, 4, -0.05f, Ease.OUTBACK);
        key(b.torso.yaw, 4, -0.3f, Ease.OUTBACK);
        key(b.torso.pitch, 4, 0.28f, Ease.OUTBACK);
        key(b.rightLeg.pitch, 4, -0.7f, Ease.OUTBACK);
        key(b.leftLeg.pitch, 4, 0.4f, Ease.OUTBACK);
        key(b.leftArm.pitch, 4, 0.55f, Ease.OUTBACK);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation loveWhip() {
        int len = 13;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_love", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.rightArm.pitch, 3, -2.9f, Ease.INOUTSINE);
        key(b.rightArm.roll, 3, 0.7f, Ease.INOUTSINE);
        key(b.torso.yaw, 3, 0.4f, Ease.INOUTSINE);
        key(b.torso.pitch, 3, -0.12f, Ease.INOUTSINE);
        key(b.rightArm.pitch, 6, -1.2f, Ease.OUTSINE);
        key(b.rightArm.roll, 6, 0.1f, Ease.OUTSINE);
        key(b.rightArm.yaw, 6, -0.35f, Ease.OUTSINE);
        key(b.torso.yaw, 6, -0.35f, Ease.OUTSINE);
        key(b.torso.pitch, 6, 0.18f, Ease.OUTSINE);
        // the snap: a quick wrist-flick back up as the blade cracks
        key(b.rightArm.pitch, 7, -1.65f, Ease.OUTBACK);
        key(b.rightArm.pitch, 9, -1.35f, Ease.INOUTSINE);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation serpentWeave() {
        int len = 12;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_serpent", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.torso.pitch, 2, 0.28f, Ease.INOUTSINE);
        key(b.body.y, 2, -0.12f, Ease.INOUTSINE);
        float[] yaws = {0.7f, -0.55f, 0.45f, -0.7f};
        for (int i = 0; i < yaws.length; i++) {
            int t = 2 + i * 2;
            key(b.rightArm.pitch, t, -1.15f - 0.08f * i, Ease.INOUTSINE);
            key(b.rightArm.yaw, t, yaws[i], Ease.INOUTSINE);
            key(b.torso.yaw, t, yaws[i] * 0.4f, Ease.INOUTSINE);
            key(b.torso.roll, t, -yaws[i] * 0.15f, Ease.INOUTSINE);
        }
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation stoneHurl() {
        int len = 18;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_stone", len);
        pose(b, 0, 0f, 0f, 0f);
        bothHands(b, 5, -3.05f, 0.3f, Ease.INOUTSINE);
        key(b.torso.yaw, 5, 0.55f, Ease.INOUTSINE);
        key(b.torso.pitch, 5, -0.22f, Ease.INOUTSINE);
        key(b.leftLeg.pitch, 5, -0.2f, Ease.INOUTSINE);
        bothHands(b, 9, -1.25f, -0.35f, Ease.OUTBACK);
        key(b.torso.yaw, 9, -0.4f, Ease.OUTBACK);
        key(b.torso.pitch, 9, 0.35f, Ease.OUTBACK);
        key(b.rightLeg.pitch, 9, -0.5f, Ease.OUTBACK);
        key(b.leftLeg.pitch, 9, 0.3f, Ease.OUTBACK);
        key(b.body.y, 9, -0.12f, Ease.OUTBACK);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }

    private static KeyframeAnimation soundCross() {
        int len = 12;
        KeyframeAnimation.AnimationBuilder b = builder("weapon_sound", len);
        pose(b, 0, 0f, 0f, 0f);
        key(b.rightArm.pitch, 2, -2.3f, Ease.OUTSINE);
        key(b.rightArm.roll, 2, 1.0f, Ease.OUTSINE);
        key(b.leftArm.pitch, 2, -2.3f, Ease.OUTSINE);
        key(b.leftArm.roll, 2, -1.0f, Ease.OUTSINE);
        key(b.torso.pitch, 2, -0.15f, Ease.OUTSINE);
        key(b.rightArm.pitch, 5, -1.1f, Ease.OUTBACK);
        key(b.rightArm.roll, 5, 0f, Ease.OUTBACK);
        key(b.rightArm.yaw, 5, -1.0f, Ease.OUTBACK);
        key(b.leftArm.pitch, 5, -1.1f, Ease.OUTBACK);
        key(b.leftArm.roll, 5, 0f, Ease.OUTBACK);
        key(b.leftArm.yaw, 5, 1.0f, Ease.OUTBACK);
        key(b.torso.pitch, 5, 0.3f, Ease.OUTBACK);
        key(b.body.y, 5, -0.1f, Ease.OUTBACK);
        pose(b, len, 0f, 0f, 0f);
        return b.build();
    }
}
