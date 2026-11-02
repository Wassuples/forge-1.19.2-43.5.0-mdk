package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BloodArcDomeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Rampant Arc Rampage: eight blood sickles whirling around Gyutaro on criss-crossing orbits, traced by faint
 * crimson rings, forming a cage of arcs. On a successful parry they fly outward with an expanding shockwave.
 */
public class BloodArcDomeRenderer extends EntityRenderer<BloodArcDomeEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int BLADES = 8;

    public BloodArcDomeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(BloodArcDomeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity owner = entity.getOwner();
        if (owner == null) return;
        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 4.0f, 0.0f, 1.0f);
        int burstTick = entity.getBurstTick();
        float burst = burstTick < 0 ? 0.0f : Mth.clamp((age - burstTick) / BloodArcDomeEntity.BURST_TICKS, 0.0f, 1.0f);
        float burstEase = 1.0f - (1.0f - burst) * (1.0f - burst);
        float fadeGuard = burstTick < 0 ? Mth.clamp((entity.getGuardTicks() - age) / 3.0f, 0.0f, 1.0f) : 1.0f;
        float brightness = appear * fadeGuard * (1.0f - burst);

        float centreY = owner.getBbHeight() * 0.55f;
        float orbit = Mth.lerp(burstEase, 1.5f, entity.getBurstRadius());
        float bladeSize = 0.55f * (1.0f + burst * 0.8f);

        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);

        // Orbit traces.
        for (int i = 0; i < 3; i++) {
            double tilt = Math.toRadians(35 + i * 50);
            double spin = age * 0.15 + i * 2.1;
            Vec3 prev = null;
            for (int s = 0; s <= 32; s++) {
                double a = Math.PI * 2.0 * s / 32;
                Vec3 p = new Vec3(Math.cos(a) * orbit, Math.sin(a) * orbit * Math.cos(tilt), Math.sin(a) * orbit * Math.sin(tilt))
                        .yRot((float) spin).add(0, centreY, 0);
                if (prev != null) {
                    ThreadRenderHelper.thread(pose, vc, cam, prev, p, ThreadRenderHelper.BLOOD_CORE, ThreadRenderHelper.BLOOD_GLOW, brightness * 0.45f);
                }
                prev = p;
            }
        }

        // Blades.
        for (int i = 0; i < BLADES; i++) {
            float orbitYaw = i * (360.0f / BLADES) + i * 17.0f;
            float orbitTilt = -60.0f + (i * 37 % 120);
            float angle = age * 34.0f + i * 45.0f;
            pose.pushPose();
            pose.translate(0, centreY, 0);
            pose.mulPose(Vector3f.YP.rotationDegrees(orbitYaw));
            pose.mulPose(Vector3f.XP.rotationDegrees(orbitTilt));
            pose.mulPose(Vector3f.YP.rotationDegrees(angle));
            pose.translate(orbit, 0, 0);
            pose.mulPose(Vector3f.YP.rotationDegrees(-90.0f)); // cutting edge leads along the orbit
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0f));  // blade lies in the orbit plane
            BloodRenderHelper.crescent(pose, buffers, bladeSize, Math.max(brightness, burst > 0 ? 1.0f - burst : 0.0f));
            pose.popPose();
        }

        // Shockwave on a successful parry.
        if (burst > 0.0f) {
            vc = ThreadRenderHelper.buffer(buffers); // the blades switched render types; re-fetch
            float r = Mth.lerp(burstEase, 1.0f, entity.getBurstRadius() + 1.0f);
            Vec3 prev = null;
            for (int s = 0; s <= 48; s++) {
                double a = Math.PI * 2.0 * s / 48;
                Vec3 p = new Vec3(Math.cos(a) * r, centreY * 0.6, Math.sin(a) * r);
                if (prev != null) {
                    ThreadRenderHelper.thread(pose, vc, cam, prev, p, 0xF0FFD0D6, ThreadRenderHelper.BLOOD_GLOW, (1.0f - burst) * 1.4f);
                }
                prev = p;
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodArcDomeEntity entity) {
        return TEXTURE;
    }
}
