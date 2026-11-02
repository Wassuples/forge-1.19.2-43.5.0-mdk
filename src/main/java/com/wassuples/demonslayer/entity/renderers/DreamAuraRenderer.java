package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.DreamAuraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Forced Sleep, seen from outside: a slowly turning dream halo above the sleeper's head with pastel motes
 * orbiting it, and big soft "Z z z" drifting up and away. Hidden for the sleeper's own first-person view
 * (they get the dream overlay instead).
 */
public class DreamAuraRenderer extends EntityRenderer<DreamAuraEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public DreamAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(DreamAuraEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity target = entity.getTarget();
        if (target == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (target == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return;

        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 10.0f, 0.0f, 1.0f);
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        Vec3 offset = target.getPosition(partialTick).subtract(origin);
        double top = target.getBbHeight() + 0.25;

        // Dream halo: a tilted, slowly turning double ring.
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        double tilt = 0.18 * Math.sin(age * 0.05);
        double radius = Math.max(0.35, target.getBbWidth() * 0.6);
        for (int ring = 0; ring < 2; ring++) {
            double r = radius * (ring == 0 ? 1.0 : 0.72);
            double h = top + (ring == 0 ? 0.0 : 0.08);
            Vec3 prev = null;
            for (int s = 0; s <= 28; s++) {
                double a = Math.PI * 2.0 * s / 28 + age * (ring == 0 ? 0.04 : -0.06);
                Vec3 p = offset.add(Math.cos(a) * r, h + Math.sin(a) * r * tilt, Math.sin(a) * r);
                if (prev != null) {
                    ThreadRenderHelper.thread(pose, vc, cam, prev, p, WhisperWaveRenderer.DREAM_CORE, WhisperWaveRenderer.DREAM_GLOW, 0.7f * appear);
                }
                prev = p;
            }
        }
        // Orbiting motes: tiny star crosses.
        for (int i = 0; i < 5; i++) {
            double a = age * 0.09 + i * (Math.PI * 2.0 / 5.0);
            Vec3 c = offset.add(Math.cos(a) * radius * 1.15, top + 0.12 + 0.08 * Math.sin(age * 0.2 + i), Math.sin(a) * radius * 1.15);
            float twinkle = 0.5f + 0.5f * Mth.sin(age * 0.6f + i * 1.7f);
            double size = 0.06 + 0.03 * twinkle;
            ThreadRenderHelper.thread(pose, vc, cam, c.add(-size, 0, 0), c.add(size, 0, 0), 0xF0FFFFFF, 0x60FFB6EC, appear * twinkle);
            ThreadRenderHelper.thread(pose, vc, cam, c.add(0, -size, 0), c.add(0, size, 0), 0xF0FFFFFF, 0x60FFB6EC, appear * twinkle);
        }

        // Z z z: three letters on a staggered 39-tick cycle, drifting up and to the side, growing then fading.
        for (int i = 0; i < 3; i++) {
            float cycle = ((age + i * 13.0f) % 39.0f) / 39.0f;
            float alpha = Mth.sin((float) Math.PI * cycle) * appear;
            if (alpha < 0.05f) continue;
            pose.pushPose();
            pose.translate(offset.x + 0.25 + cycle * 0.45, offset.y + top + 0.3 + cycle * 0.9, offset.z);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float s = 0.022f + 0.022f * cycle;
            pose.scale(-s, -s, s);
            String z = i == 0 ? "Z" : "z";
            float x = -this.getFont().width(z) / 2.0f;
            int a = (int) (alpha * 230);
            int outline = (a << 24) | 0x3A1658;
            for (int d = 0; d < 4; d++) {
                float ox = d == 0 ? 1 : d == 1 ? -1 : 0;
                float oy = d == 2 ? 1 : d == 3 ? -1 : 0;
                pose.pushPose();
                pose.translate(0, 0, 0.3);
                this.getFont().drawInBatch(z, x + ox, oy, outline, false, pose.last().pose(), buffers, false, 0, 0xF000F0);
                pose.popPose();
            }
            this.getFont().drawInBatch(z, x, 0, (a << 24) | 0xF4D4FF, false, pose.last().pose(), buffers, false, 0, 0xF000F0);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DreamAuraEntity entity) {
        return TEXTURE;
    }
}
