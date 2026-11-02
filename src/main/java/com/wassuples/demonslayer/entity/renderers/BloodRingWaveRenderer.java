package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BloodRingWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Rotating Circular Slashes: three tiers of spinning blood sickles sweeping outward on an expanding ring, each tier
 * dragging crimson arcs behind it. Radius matches the damage ring exactly.
 */
public class BloodRingWaveRenderer extends EntityRenderer<BloodRingWaveEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public BloodRingWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(BloodRingWaveEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float radius = Math.max(0.6f, entity.radiusAt(age));
        float progress = Mth.clamp(age / entity.getDuration(), 0.0f, 1.0f);
        float fade = Mth.clamp((entity.getDuration() + 6 - age) / 8.0f, 0.0f, 1.0f);
        float height = entity.getWaveHeight();

        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);

        float[] tiers = {0.5f, height * 0.5f, height - 0.4f};
        // More blades as the ring grows so spacing stays dense.
        int blades = Mth.clamp((int) (radius * 2.2f), 6, 30);
        for (int t = 0; t < tiers.length; t++) {
            float tierSpin = age * (t % 2 == 0 ? 6.0f : -6.0f) * Mth.DEG_TO_RAD;
            float y = tiers[t];

            // Dragged arcs: three 80° arcs per tier, rotating with the tier.
            for (int arc = 0; arc < 3; arc++) {
                double start = tierSpin + arc * (Math.PI * 2.0 / 3.0);
                Vec3 prev = null;
                for (int s = 0; s <= 14; s++) {
                    double a = start + Math.toRadians(80) * s / 14;
                    float tail = (float) s / 14;
                    Vec3 p = new Vec3(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    if (prev != null) {
                        ThreadRenderHelper.thread(pose, vc, cam, prev, p, ThreadRenderHelper.BLOOD_CORE, ThreadRenderHelper.BLOOD_GLOW, fade * (0.3f + 0.7f * tail));
                    }
                    prev = p;
                }
            }

            for (int b = 0; b < blades; b++) {
                double a = tierSpin + Math.PI * 2.0 * b / blades + t * 0.3;
                pose.pushPose();
                pose.translate(Math.cos(a) * radius, y, Math.sin(a) * radius);
                // Face outward-ish and spin flat, like a thrown kama riding the wave.
                pose.mulPose(Vector3f.YP.rotation((float) (-a)));
                pose.mulPose(Vector3f.XP.rotationDegrees(90.0f - 15.0f));
                pose.mulPose(Vector3f.ZP.rotationDegrees(age * 40.0f + b * 30.0f));
                BloodRenderHelper.crescent(pose, buffers, 0.5f + 0.15f * progress, fade);
                pose.popPose();
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodRingWaveEntity entity) {
        return TEXTURE;
    }
}
