package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ToonBananaPeelRenderer extends EntityRenderer<ToonBananaPeelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonBananaPeelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1f;
    }

    @Override
    public void render(ToonBananaPeelEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        stack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        stack.scale(0.85f, 0.85f, 0.85f);

        ToonRenderHelper.ellipse(stack, buffer, 0x77111111, 0.62f, 0.16f, -0.09f);
        ToonRenderHelper.quad(stack, buffer, 0xFF2B1C08, -0.08f, -0.12f, 0.08f, 0.12f, -0.04f);

        drawPeel(stack, buffer, -0.42f, -0.12f, -0.02f, 0.20f, 0xFFFFD83D);
        drawPeel(stack, buffer, -0.12f, -0.20f, 0.20f, 0.30f, 0xFFFFEC55);
        drawPeel(stack, buffer, 0.22f, -0.10f, 0.45f, 0.18f, 0xFFFFD83D);

        ToonRenderHelper.quad(stack, buffer, 0xFFE7A316, -0.12f, 0.02f, 0.12f, 0.36f, -0.01f);
        ToonRenderHelper.quad(stack, buffer, 0xFFFFFFFF, -0.08f, 0.22f, 0.02f, 0.31f, 0.0f);

        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    private static void drawPeel(PoseStack stack, MultiBufferSource buffer, float minX, float minY, float maxX, float maxY, int color) {
        ToonRenderHelper.quad(stack, buffer, 0xFF1A1204, minX - 0.04f, minY - 0.04f, maxX + 0.04f, maxY + 0.04f, -0.03f);
        ToonRenderHelper.quad(stack, buffer, color, minX, minY, maxX, maxY, -0.02f);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonBananaPeelEntity entity) {
        return TEXTURE;
    }
}
