package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class ToonRenderHelper {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private ToonRenderHelper() {
    }

    static void ellipse(PoseStack stack, MultiBufferSource buffer, int color, float radiusX, float radiusY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        int segments = 28;

        for (int i = 0; i < segments; i++) {
            float x0 = -radiusX + (2.0f * radiusX * i / segments);
            float x1 = -radiusX + (2.0f * radiusX * (i + 1) / segments);
            float y0 = radiusY * (float) Math.sqrt(Math.max(0.0f, 1.0f - (x0 * x0) / (radiusX * radiusX)));
            float y1 = radiusY * (float) Math.sqrt(Math.max(0.0f, 1.0f - (x1 * x1) / (radiusX * radiusX)));

            quad(vc, pose, normal, x0, -y0, z, x1, -y1, z, x1, y1, z, x0, y0, z, color, FULL_BRIGHT, 0, 0, 1);
        }
    }

    static void quad(PoseStack stack, MultiBufferSource buffer, int color, float minX, float minY, float maxX, float maxY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        quad(vc, pose, normal, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z, color, FULL_BRIGHT, 0, 0, 1);
    }

    static void texturedQuad(PoseStack stack, MultiBufferSource buffer, ResourceLocation texture,
                             float minX, float minY, float maxX, float maxY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        quad(vc, pose, normal, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z,
                0xFFFFFFFF, FULL_BRIGHT, 0, 0, 1);
    }

    static void box(PoseStack stack, MultiBufferSource buffer, int color,
                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        quad(vc, pose, normal, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, FULL_BRIGHT, 0, 0, 1);
        quad(vc, pose, normal, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, color, FULL_BRIGHT, 0, 0, -1);
        quad(vc, pose, normal, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, color, FULL_BRIGHT, 0, 1, 0);
        quad(vc, pose, normal, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color, FULL_BRIGHT, 0, -1, 0);
        quad(vc, pose, normal, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, color, FULL_BRIGHT, 1, 0, 0);
        quad(vc, pose, normal, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color, FULL_BRIGHT, -1, 0, 0);
    }

    static void text(Font font, PoseStack stack, MultiBufferSource buffer, Component text, int color) {
        Matrix4f pose = stack.last().pose();
        float x = -font.width(text) / 2.0f;
        font.drawInBatch(text, x, 0.0f, color, false, pose, buffer, true, 0, FULL_BRIGHT);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int color, int light, float normalX, float normalY, float normalZ) {
        int a = color >>> 24 & 255;
        int r = color >>> 16 & 255;
        int g = color >>> 8 & 255;
        int b = color & 255;
        vertex(vc, pose, normal, x1, y1, z1, 0, 1, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x2, y2, z2, 1, 1, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x3, y3, z3, 1, 0, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x4, y4, z4, 0, 0, r, g, b, a, light, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light,
                               float normalX, float normalY, float normalZ) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
