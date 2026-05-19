package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Client-side renderer for Toon Shadow Anchor entity.
 *
 * <h2>Adding Custom Models/Animations</h2>
 * <p>To add a custom 3D model to the shadow anchor:</p>
 * <ol>
 *   <li>Create {@code assets/demonslayer/models/entity/toon_shadow_anchor.geo.json}</li>
 *   <li>Create {@code assets/demonslayer/textures/entity/toon_shadow_anchor.png}</li>
 *   <li>Create {@code assets/demonslayer/animations/entity/toon_shadow_anchor.animation.json}</li>
 *   <li>Make ToonShadowAnchorEntity extend LivingEntity and implement IAnimatable</li>
 *   <li>Change this class to extend {@code GeoEntityRenderer<ToonShadowAnchorEntity>}</li>
 * </ol>
 */
public class ToonShadowAnchorRenderer extends EntityRenderer<ToonShadowAnchorEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonShadowAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonShadowAnchorEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // === CUSTOM MODEL HOOK ===
        // Replace the below manual drawing with your GeckoLib model.

        float pulse = 1.0f + Mth.sin((entity.tickCount + partialTicks) * 0.35f) * 0.08f;
        stack.scale(pulse, pulse, pulse);

        // Shadow disc layers
        ToonRenderHelper.ellipse(stack, buffer, 0xEE050508, 0.82f, 0.30f, -0.04f);
        ToonRenderHelper.ellipse(stack, buffer, 0xAA4C1D88, 0.92f, 0.38f, -0.05f);
        ToonRenderHelper.ellipse(stack, buffer, 0xF7070709, 0.70f, 0.22f, -0.03f);

        // Shadow tendrils
        stack.pushPose();
        stack.mulPose(Vector3f.ZP.rotationDegrees(-18.0f));
        ToonRenderHelper.quad(stack, buffer, 0xE509090B, -0.12f, -0.02f, 0.78f, 0.12f, -0.01f);
        ToonRenderHelper.quad(stack, buffer, 0xB54C1D88, 0.54f, 0.08f, 0.94f, 0.22f, -0.01f);
        stack.popPose();

        stack.pushPose();
        stack.mulPose(Vector3f.ZP.rotationDegrees(21.0f));
        ToonRenderHelper.quad(stack, buffer, 0xE509090B, -0.14f, -0.10f, 0.54f, 0.04f, -0.01f);
        stack.popPose();

        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonShadowAnchorEntity entity) {
        return TEXTURE;
    }
}
