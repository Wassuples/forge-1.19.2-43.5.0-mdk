package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doma's pair of golden iron fans, held open in both hands while he uses Cryokinesis. Shown for as long as one of
 * his {@code doma_*} player animations plays (see ClientFormAnimations), attached to the animated arms so they
 * follow every sweep and flick.
 */
public class DomaFansLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Entity id -> client game time until which the fans are shown. */
    private static final Map<Integer, Long> SHOWN_UNTIL = new ConcurrentHashMap<>();

    public DomaFansLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Show the fans on entity {@code entityId} for {@code ticks} ticks (0 hides them). */
    public static void showFans(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ticks <= 0) {
            SHOWN_UNTIL.remove(entityId);
        } else {
            SHOWN_UNTIL.put(entityId, mc.level.getGameTime() + ticks);
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Long until = SHOWN_UNTIL.get(player.getId());
        if (until == null) return;
        long now = player.level.getGameTime();
        if (now > until) {
            SHOWN_UNTIL.remove(player.getId());
            return;
        }
        if (player.isInvisible()) return;
        float open = Math.min(1.0f, (until - now) / 6.0f); // folds shut as it ends
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            (side == 0 ? this.getParentModel().rightArm : this.getParentModel().leftArm).translateAndRotate(pose);
            // At the hand, the fan spreading on past the fingers along the arm (model +Y runs down the arm).
            pose.translate(side == 0 ? -0.06f : 0.06f, 0.62f, 0.0f);
            pose.mulPose(Vector3f.YP.rotationDegrees(side == 0 ? 20.0f : -20.0f));
            DomaRenderHelper.fan(pose, buffers, 0.55f, open, light);
            pose.popPose();
        }
    }
}
