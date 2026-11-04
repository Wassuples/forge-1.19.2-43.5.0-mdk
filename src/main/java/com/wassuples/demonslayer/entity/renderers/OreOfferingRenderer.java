package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.OreOfferingEntity;
import com.wassuples.demonslayer.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An ore laid out for the ceremony: a small black-lacquered tray with a vermilion cloth, the ore (its 3D item model)
 * resting on it and turning slowly. When the candidate looks at it, it lifts and turns faster and its name and a
 * one-line description float above it, on paper.
 */
public class OreOfferingRenderer extends EntityRenderer<OreOfferingEntity> {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final ResourceLocation FILL = new ResourceLocation(DemonSlayer.MOD_ID, "textures/misc/speech_fill.png");

    public OreOfferingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(OreOfferingEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean looked = mc.crosshairPickEntity == e;
        float t = e.tickCount + partialTick;

        // ---- the tray: black lacquer with a vermilion cloth
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        box(pose, vc, light, -0.3F, 0.0F, -0.3F, 0.3F, 0.08F, 0.3F, 0x16100E);
        box(pose, vc, light, -0.27F, 0.08F, -0.27F, 0.27F, 0.1F, 0.27F, looked ? 0xD8343A : 0xA82228);

        // ---- the ore, turning
        ItemStack stack = new ItemStack(ModItems.SELECTION_ORES.get(Mth.clamp(e.ore(), 0, ModItems.SELECTION_ORES.size() - 1)).get());
        pose.pushPose();
        float lift = looked ? 0.12F + Mth.sin(t * 0.15F) * 0.02F : 0.0F;
        pose.translate(0.0D, 0.28D + lift, 0.0D);
        pose.mulPose(Vector3f.YP.rotationDegrees(t * (looked ? 3.0F : 0.8F) + e.getId() * 37.0F));
        pose.scale(1.1F, 1.1F, 1.1F);
        mc.getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.GROUND, looked ? 0xF000F0 : light, OverlayTexture.NO_OVERLAY,
                pose, buffers, e.getId());
        pose.popPose();

        if (looked) label(e, stack, pose, buffers, mc.font);
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    /** Name and description on a paper slip above the tray. */
    private void label(OreOfferingEntity e, ItemStack stack, PoseStack pose, MultiBufferSource buffers, Font font) {
        Component name = stack.getHoverName();
        Component desc = Component.translatable("mission.demonslayer.final_selection.twins.ceremony.ore." + e.ore());
        List<FormattedCharSequence> lines = font.split(desc, 120);
        int w = font.width(name);
        for (FormattedCharSequence l : lines) w = Math.max(w, font.width(l));
        pose.pushPose();
        pose.translate(0.0D, 0.95D, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.02F, -0.02F, 0.02F);
        Matrix4f m = pose.last().pose();
        int full = 0xF000F0;
        float h = 12 + lines.size() * 10;
        rect(buffers, m, -w / 2f - 5, -h - 2, w / 2f + 5, 2, 0.02F, 0x3B2A4A, full);
        rect(buffers, m, -w / 2f - 4, -h - 1, w / 2f + 4, 1, 0.01F, 0xF2E9D8, full);
        font.drawInBatch(name, -font.width(name) / 2f, -h + 1, 0xFF8A1A22, false, m, buffers, false, 0, full);
        float y = -h + 12;
        for (FormattedCharSequence l : lines) {
            font.drawInBatch(l, -font.width(l) / 2f, y, 0xFF2A1E33, false, m, buffers, false, 0, full);
            y += 10;
        }
        pose.popPose();
    }

    private static void rect(MultiBufferSource buffers, Matrix4f m, float x0, float y0, float x1, float y1, float z, int rgb, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.text(FILL));
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        vc.vertex(m, x0, y1, z).color(r, g, b, 235).uv(0, 1).uv2(light).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, 235).uv(1, 1).uv2(light).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, 235).uv(1, 0).uv2(light).endVertex();
        vc.vertex(m, x0, y0, z).color(r, g, b, 235).uv(0, 0).uv2(light).endVertex();
    }

    /** A solid, flat-coloured box (all six faces). */
    private static void box(PoseStack pose, VertexConsumer vc, int light, float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1},
                {x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0}};
        for (float[] f : faces) {
            float shade = f[13] > 0 ? 1.0F : f[13] < 0 ? 0.5F : f[14] != 0 ? 0.8F : 0.6F;
            int rr = (int) (r * shade), gg = (int) (g * shade), bb = (int) (b * shade);
            for (int i = 0; i < 4; i++) {
                vc.vertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).color(rr, gg, bb, 255).uv(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, f[12], f[13], f[14]).endVertex();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(OreOfferingEntity entity) {
        return WHITE;
    }
}
