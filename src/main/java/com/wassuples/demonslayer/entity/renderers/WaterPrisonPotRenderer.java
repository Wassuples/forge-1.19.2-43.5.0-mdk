package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WaterPrisonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Water Prison Pot: a wobbling sphere of water engulfing the trapped target, with a current swirling around it,
 * bubbles escaping upward and a glint on the surface. It swells out of a splash when cast.
 */
public class WaterPrisonPotRenderer extends EntityRenderer<WaterPrisonEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int WATER = 0x5A3FA2E0;

    public WaterPrisonPotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(WaterPrisonEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        List<Entity> passengers = entity.getPassengers();
        float height = passengers.isEmpty() ? 1.8f : passengers.get(0).getBbHeight();
        float width = passengers.isEmpty() ? 0.6f : passengers.get(0).getBbWidth();
        float radius = Math.max(1.1f, Math.max(height, width) * 0.68f);
        float swell = Mth.clamp(age / 8.0f, 0.0f, 1.0f);
        swell = 1.0f + 0.15f * (1.0f - swell) * Mth.sin(age * 1.5f) - (1.0f - swell) * 0.6f;
        double centreY = entity.getPassengersRidingOffset() + height * 0.5;

        pose.pushPose();
        pose.translate(0, centreY, 0);
        GyokkoRenderHelper.waterBubble(pose, buffers, radius * swell, age, WATER);
        pose.popPose();

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick));
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        Vec3 centre = new Vec3(0, centreY, 0);
        // Swirling current: two tilted rings of light running around the inside.
        for (int ring = 0; ring < 2; ring++) {
            double tilt = ring == 0 ? 0.35 : -0.5;
            Vec3 prev = null;
            for (int s = 0; s <= 30; s++) {
                double a = Math.PI * 2.0 * s / 30 + age * (ring == 0 ? 0.12 : -0.09);
                double r = radius * 0.82 * swell;
                Vec3 p = centre.add(Math.cos(a) * r, Math.sin(a) * r * tilt, Math.sin(a) * r);
                if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, 0x90E6F6FF, 0x305AB6FF, 0.6f);
                prev = p;
            }
        }
        // Bubbles rising through the sphere and popping out of the top.
        for (int i = 0; i < 10; i++) {
            float cycle = ((age * 0.7f + i * 9.3f) % 40.0f) / 40.0f;
            double a = i * 2.4 + age * 0.02;
            double rr = radius * 0.6 * Math.sin(i * 1.7 + 1.0);
            Vec3 p = centre.add(Math.cos(a) * rr, (cycle * 2.0 - 1.0) * radius * 1.05, Math.sin(a) * rr);
            double size = 0.05 + 0.04 * Math.sin(i);
            ThreadRenderHelper.thread(pose, vc, cam, p.add(-size, 0, 0), p.add(size, 0, 0), 0xC0FFFFFF, 0x5090D8FF, 1.0f - cycle * 0.5f);
            ThreadRenderHelper.thread(pose, vc, cam, p.add(0, -size, 0), p.add(0, size, 0), 0xC0FFFFFF, 0x5090D8FF, 1.0f - cycle * 0.5f);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterPrisonEntity entity) {
        return TEXTURE;
    }
}
