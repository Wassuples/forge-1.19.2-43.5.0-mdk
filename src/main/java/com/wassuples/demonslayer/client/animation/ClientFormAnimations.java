package com.wassuples.demonslayer.client.animation;

import com.wassuples.demonslayer.DemonSlayer;
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
    }

    private static KeyframeAnimation create(String animationId) {
        return switch (animationId) {
            case FormAnimations.TOON_EYE_POP -> eyePop();
            case FormAnimations.TOON_ANVIL -> anvil();
            case FormAnimations.TOON_BANANA -> bananaThrow();
            case FormAnimations.TOON_SHADOW -> shadowToss();
            case FormAnimations.TOON_TUNNEL -> paintTunnel();
            case FormAnimations.TOON_POCKET -> pocketPull();
            case FormAnimations.TOON_BONKED -> bonked();
            case FormAnimations.TOON_SLIP -> slip();
            case FormAnimations.TOON_PIE_SPLAT -> pieSplat();
            default -> null;
        };
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

    private static KeyframeAnimation slip() {
        KeyframeAnimation.AnimationBuilder builder = builder("toon_slip", 30);
        pose(builder, 0, 0.0f, 0.0f, 0.0f);
        key(builder.body.y, 3, 0.22f, Ease.OUTBACK);
        key(builder.torso.pitch, 3, -0.45f, Ease.OUTBACK);
        key(builder.rightArm.pitch, 3, -2.15f, Ease.OUTBACK);
        key(builder.leftArm.pitch, 3, -2.0f, Ease.OUTBACK);
        key(builder.rightLeg.pitch, 3, -1.05f, Ease.OUTBACK);
        key(builder.leftLeg.pitch, 3, 0.65f, Ease.OUTBACK);
        key(builder.body.y, 9, -0.38f, Ease.OUTBOUNCE);
        key(builder.torso.pitch, 9, 1.15f, Ease.OUTBOUNCE);
        key(builder.rightArm.pitch, 9, 1.15f, Ease.OUTBOUNCE);
        key(builder.leftArm.pitch, 9, 1.35f, Ease.OUTBOUNCE);
        key(builder.rightLeg.pitch, 9, 0.55f, Ease.OUTBOUNCE);
        key(builder.leftLeg.pitch, 9, -0.85f, Ease.OUTBOUNCE);
        key(builder.torso.roll, 14, -0.35f, Ease.INOUTSINE);
        key(builder.torso.roll, 20, 0.28f, Ease.INOUTSINE);
        pose(builder, 30, 0.0f, 0.0f, 0.0f);
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

    private static KeyframeAnimation.AnimationBuilder builder(String name, int durationTicks) {
        KeyframeAnimation.AnimationBuilder builder = new KeyframeAnimation.AnimationBuilder(AnimationFormat.JSON_MC_ANIM);
        builder.beginTick = 0;
        builder.endTick = durationTicks;
        builder.stopTick = durationTicks;
        builder.isLooped = false;
        builder.returnTick = 0;
        builder.setName(name);
        return builder;
    }

    private static void pose(KeyframeAnimation.AnimationBuilder builder, int tick, float bodyY, float torsoPitch, float headPitch) {
        key(builder.body.y, tick, bodyY, Ease.LINEAR);
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

    private static void key(KeyframeAnimation.StateCollection.State state, int tick, float value, Ease ease) {
        state.setEnabled(true);
        state.addKeyFrame(tick, value, ease);
    }
}
