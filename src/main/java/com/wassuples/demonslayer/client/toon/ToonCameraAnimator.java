package com.wassuples.demonslayer.client.toon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Moves the first-person camera with the local player's knock-down animations (banana slip and fake
 * tunnel bonk): the eye drops and swings backwards with the body and the view tips up to the sky.
 * Position is applied from {@code ToonCameraMixin}; pitch/roll from {@link ToonClientEvents}.
 */
@OnlyIn(Dist.CLIENT)
public final class ToonCameraAnimator {
    private static final float MIN_EYE_HEIGHT = 0.28f;
    @Nullable
    private static float[][] keys;
    private static int startTick;

    private ToonCameraAnimator() {
    }

    public static void onAnimationStarted(String animationId) {
        LocalPlayer player = Minecraft.getInstance().player;
        float[][] found = ToonFallCurve.keysFor(animationId);
        if (player == null) return;
        if (found == null) {
            // Any other toon animation replaces the layer, so the camera must stop following too.
            keys = null;
            return;
        }
        keys = found;
        startTick = player.tickCount;
    }

    public static void reset() {
        keys = null;
    }

    private static float time(LocalPlayer player, float partialTick) {
        return player.tickCount - startTick + partialTick;
    }

    public static boolean isActive(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (keys == null || player == null) return false;
        float t = time(player, partialTick);
        if (t < 0 || t > ToonFallCurve.length(keys) || !player.isAlive()) {
            keys = null;
            return false;
        }
        return true;
    }

    /** 0..1, used to hide the first-person hand while lying down. */
    public static float flatness(float partialTick) {
        if (!isActive(partialTick)) return 0.0f;
        return ToonFallCurve.flatness(keys, time(Minecraft.getInstance().player, partialTick));
    }

    /** New first-person eye position, or null to leave the camera alone. Called at the end of Camera#setup. */
    @Nullable
    public static Vec3 adjustCameraPosition(Entity entity, boolean detached, float partialTick, float eyeHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (detached || entity != player || !isActive(partialTick)) return null;

        float t = time(player, partialTick);
        float height = Math.max(MIN_EYE_HEIGHT, ToonFallCurve.pointHeight(keys, t, eyeHeight));
        float back = ToonFallCurve.pointBackOffset(keys, t, eyeHeight) * ToonFallCurve.BACK;
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 backDir = new Vec3(Mth.sin(bodyYaw), 0.0, -Mth.cos(bodyYaw));

        Vec3 feet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ()));
        Vec3 normalEye = feet.add(0.0, eyeHeight, 0.0);
        Vec3 wanted = feet.add(backDir.scale(back)).add(0.0, height, 0.0);

        // Never swing the camera through a wall behind the player.
        BlockHitResult hit = player.level.clip(new ClipContext(normalEye, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 dir = wanted.subtract(normalEye);
            double len = dir.length();
            if (len > 1.0E-4) {
                wanted = hit.getLocation().subtract(dir.scale(Math.min(0.15, len) / len));
            }
        }
        return wanted;
    }

    /** Extra pitch (degrees, negative = look up) and roll for the current frame. Returns null if inactive. */
    @Nullable
    public static float[] angleOffsets(float partialTick) {
        if (!isActive(partialTick)) return null;
        LocalPlayer player = Minecraft.getInstance().player;
        float t = time(player, partialTick);
        float bodyPitchDeg = ToonFallCurve.pitch(keys, t) * ToonFallCurve.BACK * Mth.RAD_TO_DEG;
        float flat = ToonFallCurve.flatness(keys, t);
        float roll = Mth.sin(t * 0.35f) * 5.0f * flat;
        // Impact jolt right when the back hits the floor.
        float jolt = Mth.sin(t * 2.2f) * 3.0f * Math.max(0.0f, 1.0f - Math.abs(t - slamTick()) / 4.0f);
        return new float[]{-bodyPitchDeg + jolt, roll};
    }

    private static float slamTick() {
        return keys == ToonFallCurve.TUNNEL_SLAM ? 10.0f : 9.0f;
    }
}
