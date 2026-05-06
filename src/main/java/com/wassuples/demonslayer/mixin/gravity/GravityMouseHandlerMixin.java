package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class GravityMouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    // Quick storage for gravity-frame look angles: [gYaw, gPitch]
    // Key: entity ID. Reset when gravity direction changes.
    private static final java.util.Map<Integer, float[]> gravityAngles = new java.util.HashMap<>();
    private static final java.util.Map<Integer, GravityDirection> lastKnownDir = new java.util.HashMap<>();

    @Redirect(
        method = "turnPlayer()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void demonslayer$gravityTurn(net.minecraft.client.player.LocalPlayer player,
                                         double yawDelta, double pitchDelta) {
        GravityDirection dir = player.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity)
            .orElse(GravityDirection.DOWN);

        if (dir == GravityDirection.DOWN) {
            player.turn(yawDelta, pitchDelta);
            return;
        }

        int id = player.getId();

        // Re-initialise gravity-frame angles if gravity just changed.
        GravityDirection prev = lastKnownDir.get(id);
        if (prev != dir) {
            float[] init = GravityUtil.vanillaToGravityLook(player.getYRot(), player.getXRot(), dir);
            gravityAngles.put(id, new float[]{init[0], init[1]});
            lastKnownDir.put(id, dir);
        }

        float[] ga = gravityAngles.computeIfAbsent(id, k -> new float[]{0f, 0f});

        // Apply mouse delta directly in gravity space — no remapping needed.
        ga[0] += (float) yawDelta;            // gravity yaw
        ga[1]  = Mth.clamp(ga[1] + (float) pitchDelta, -90f, 90f); // gravity pitch

        // Convert back to vanilla angles that produce the correct world look.
        float[] va = GravityUtil.gravityLookToVanilla(ga[0], ga[1], dir);
        player.setYRot(va[0]);
        player.setXRot(va[1]);
        player.yRotO = va[0];
        player.xRotO = va[1];
    }
}
