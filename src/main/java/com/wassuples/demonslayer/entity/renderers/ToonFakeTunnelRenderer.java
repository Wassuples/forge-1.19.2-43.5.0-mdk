package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Client-side renderer for Toon Fake Tunnel entity.
 *
 * <h2>Adding Custom Models/Animations</h2>
 * <p>To add a custom 3D model to the fake tunnel:</p>
 * <ol>
 *   <li>Create {@code assets/demonslayer/models/entity/toon_fake_tunnel.geo.json}</li>
 *   <li>Create {@code assets/demonslayer/textures/entity/toon_fake_tunnel.png}</li>
 *   <li>Create {@code assets/demoslayer/animations/entity/toon_fake_tunnel.animation.json}</li>
 *   <li>Make ToonFakeTunnelEntity extend LivingEntity and implement IAnimatable</li>
 *   <li>Change this class to extend {@code GeoEntityRenderer<ToonFakeTunnelEntity>}</li>
 * </ol>
 */
public class ToonFakeTunnelRenderer extends EntityRenderer<ToonFakeTunnelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonFakeTunnelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonFakeTunnelEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // === CUSTOM MODEL HOOK ===
        // Replace the below manual drawing with your GeckoLib model.

        float wobble = Mth.sin((entity.tickCount + partialTicks) * 0.25f) * 0.03f;
        stack.scale(1.9f + wobble, 2.35f - wobble, 1.0f);

        // Tunnel rings (outer to inner)
        ToonRenderHelper.ellipse(stack, buffer, 0xFF111111, 0.68f, 0.95f, -0.08f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFFFFFFFF, 0.61f, 0.86f, -0.07f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFF171717, 0.50f, 0.75f, -0.06f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFF020204, 0.35f, 0.58f, -0.05f);

        // Crack lines
        stack.pushPose();
        stack.mulPose(Vector3f.ZP.rotationDegrees(-14.0f));
        ToonRenderHelper.quad(stack, buffer, 0xDDFFFFFF, -0.48f, 0.62f, -0.10f, 0.70f, -0.03f);
        ToonRenderHelper.quad(stack, buffer, 0xDDFFFFFF, 0.12f, 0.52f, 0.48f, 0.60f, -0.03f);
        ToonRenderHelper.quad(stack, buffer, 0xDDFFFFFF, -0.38f, -0.62f, 0.00f, -0.54f, -0.03f);
        stack.popPose();

        // "THUD" text label
        stack.pushPose();
        stack.translate(0.0f, -1.08f, -0.02f);
        stack.scale(0.018f, -0.018f, 0.018f);
        ToonRenderHelper.text(this.getFont(), stack, buffer, Component.literal("THUD"), 0xFFFFF15C);
        stack.popPose();

        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonFakeTunnelEntity entity) {
        return TEXTURE;
    }
}
