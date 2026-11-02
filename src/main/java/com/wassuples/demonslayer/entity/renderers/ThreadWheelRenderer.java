package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ThreadWheelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A wheel spun from glowing threads: a double rim, twelve spokes and an inner spiral. Its wide face points
 * along the direction of travel (toward the caster behind it and the target ahead), spinning like a saw blade
 * as it races along the ground, with motion ghosts trailing behind. Flashes on every cut.
 */
public class ThreadWheelRenderer extends EntityRenderer<ThreadWheelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ThreadWheelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(ThreadWheelEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float radius = ThreadWheelEntity.DIAMETER * 0.5f;
        float travelYaw = entity.getTravelYaw();
        float spin = age * 30.0f; // saw-blade spin, degrees per tick
        float appear = Mth.clamp(age / 4.0f, 0.0f, 1.0f);
        float flash = entity.cutFlash(age);

        // Build the wheel's geometry in its own frame (X = axle-perpendicular forward, Y = up), then map to world.
        float yawRad = travelYaw * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
        Vec3 side = new Vec3(-forward.z, 0, forward.x); // wheel plane = side x up, so its face looks along travel
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 centre = new Vec3(0, radius, 0);

        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);

        int core = flash > 0 ? ThreadRenderHelper.mix(ThreadRenderHelper.WHITE_CORE, 0xFFFFFFFF, flash) : ThreadRenderHelper.WHITE_CORE;
        int glow = ThreadRenderHelper.mix(ThreadRenderHelper.WHITE_GLOW, ThreadRenderHelper.BLOOD_GLOW, 0.35f + 0.65f * flash);

        // Motion ghosts (older spin angles, fainter, slightly behind).
        for (int ghost = 2; ghost >= 0; ghost--) {
            float b = ghost == 0 ? 1.0f : 0.25f / ghost;
            Vec3 offset = forward.scale(-0.18 * ghost);
            drawWheel(pose, vc, cam, centre.add(offset), side, up, radius * appear, spin - ghost * 14.0f, core, glow, b * (1.0f + 0.5f * flash));
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void drawWheel(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 c, Vec3 forward /* in-plane axis */, Vec3 up,
                                  float r, float spinDeg, int core, int glow, float brightness) {
        int rimSegments = 28;
        double spin = Math.toRadians(spinDeg);
        Vec3 prevOuter = null;
        Vec3 prevInner = null;
        for (int s = 0; s <= rimSegments; s++) {
            double a = Math.PI * 2.0 * s / rimSegments + spin;
            Vec3 outer = point(c, forward, up, r, a);
            Vec3 inner = point(c, forward, up, r * 0.86, a + 0.05);
            if (prevOuter != null) {
                ThreadRenderHelper.thread(pose, vc, cam, prevOuter, outer, core, glow, brightness);
                ThreadRenderHelper.thread(pose, vc, cam, prevInner, inner, core, glow, brightness * 0.7f);
            }
            prevOuter = outer;
            prevInner = inner;
        }
        // Spokes, slightly curved (threads pulled by the spin).
        int spokes = 12;
        for (int i = 0; i < spokes; i++) {
            double a = Math.PI * 2.0 * i / spokes + spin;
            Vec3 mid = point(c, forward, up, r * 0.5, a - 0.25);
            ThreadRenderHelper.thread(pose, vc, cam, c, mid, core, glow, brightness * 0.8f);
            ThreadRenderHelper.thread(pose, vc, cam, mid, point(c, forward, up, r, a), core, glow, brightness * 0.8f);
        }
        // Inner spiral.
        Vec3 prev = c;
        for (int s = 1; s <= 24; s++) {
            double t = s / 24.0;
            Vec3 p = point(c, forward, up, r * 0.8 * t, spin * 1.5 + t * Math.PI * 4.0);
            ThreadRenderHelper.thread(pose, vc, cam, prev, p, core, glow, brightness * 0.55f);
            prev = p;
        }
    }

    /** Point on the wheel's plane (spanned by forward and up) at angle a and distance d from centre c. */
    private static Vec3 point(Vec3 c, Vec3 forward, Vec3 up, double d, double a) {
        return c.add(forward.scale(Math.cos(a) * d)).add(up.scale(Math.sin(a) * d));
    }

    @Override
    public ResourceLocation getTextureLocation(ThreadWheelEntity entity) {
        return TEXTURE;
    }
}
