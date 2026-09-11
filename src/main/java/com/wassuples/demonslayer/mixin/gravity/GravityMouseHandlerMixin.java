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

    @Redirect(
        method = "turnPlayer()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void demonslayer$gravityTurn(net.minecraft.client.player.LocalPlayer player,
                                         double yawDelta, double pitchDelta) {
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();

            if (dir == GravityDirection.DOWN) {
                player.turn(yawDelta, pitchDelta);
                return;
            }

            // Sync gravity angles from vanilla if they were just initialized or if dir changed
            if (gravity.getTransitionProgress() < 0.01f) {
                float[] init = GravityUtil.vanillaToGravityLook(player.getYRot(), player.getXRot(), dir);
                gravity.setGravityYaw(init[0]);
                gravity.setGravityPitch(init[1]);
            }

            // Apply mouse delta directly in gravity space
            float gYaw = gravity.getGravityYaw() + (float) yawDelta;
            float gPitch = Mth.clamp(gravity.getGravityPitch() + (float) -pitchDelta, -89.0F, 89.0F);

            gravity.setGravityYaw(gYaw);
            gravity.setGravityPitch(gPitch);
            gravity.setGravityYawOld(gYaw);
            gravity.setGravityPitchOld(gPitch);

            float[] vanilla = GravityUtil.gravityLookToVanilla(gYaw, gPitch, dir);
            player.setYRot(vanilla[0]);
            player.setXRot(vanilla[1]);
            player.yRotO = vanilla[0];
            player.xRotO = vanilla[1];
        });
    }
}
