package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Demon feedback banners (nothing permanent on screen): a large "BLOOD DEMON ART" / "ART SEALED" title when the
 * BDA keybind toggles the mode, with the art's name under it, and a short flash of the form's name when the
 * selected form changes.
 */
public final class DemonBannerOverlay {
    private static Boolean lastBdaMode = null;
    private static long bdaBannerUntil = 0L;
    private static DemonForms lastForm = null;
    private static long formBannerUntil = 0L;

    private DemonBannerOverlay() {
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;
        PlayerDemon demon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).resolve().orElse(null);
        if (demon == null || !demon.isDemon()) {
            lastBdaMode = null;
            lastForm = null;
            return;
        }
        long now = mc.level.getGameTime();
        boolean bda = demon.isBdaModeActive();
        DemonForms form = demon.getCurrentDemonForm();
        if (lastBdaMode != null && lastBdaMode != bda) bdaBannerUntil = now + 40;
        lastBdaMode = bda;
        if (lastForm != null && form != lastForm && bda) formBannerUntil = now + 25;
        lastForm = form;
        Font font = mc.font;

        float bdaLeft = (bdaBannerUntil - now - partialTick) / 40.0f;
        int a8 = (int) (255 * Math.min(1.0f, bdaLeft * 3.0f));
        if (bdaLeft > 0.0f && a8 >= 8) { // Font treats near-zero alpha as opaque
            int alpha = a8 << 24;
            pose.pushPose();
            pose.translate(width / 2.0, height * 0.26, 0);
            float s = 1.8f + 0.3f * (1.0f - Math.min(1.0f, (1.0f - bdaLeft) * 4.0f));
            pose.scale(s, s, 1.0f);
            String line = bda ? "BLOOD DEMON ART" : "ART SEALED";
            font.drawShadow(pose, line, -font.width(line) / 2.0f, -4, (bda ? 0xD8263A : 0xA09090) | alpha);
            pose.popPose();
            BloodDemonArts art = demon.getEquippedBda();
            if (bda && art != BloodDemonArts.NONE) {
                String sub = art.getDisplayName();
                font.drawShadow(pose, sub, width / 2.0f - font.width(sub) / 2.0f, (float) (height * 0.26 + 14), 0xFFE6E6 | alpha);
            }
        }
        float formLeft = (formBannerUntil - now - partialTick) / 25.0f;
        int f8 = (int) (255 * Math.min(1.0f, formLeft * 3.0f));
        if (formLeft > 0.0f && f8 >= 8 && form != null && bda) {
            String name = form.getDisplayName();
            font.drawShadow(pose, name, width / 2.0f - font.width(name) / 2.0f, height / 2.0f + 18, 0xFFFFFF | (f8 << 24));
        }
    };
}
