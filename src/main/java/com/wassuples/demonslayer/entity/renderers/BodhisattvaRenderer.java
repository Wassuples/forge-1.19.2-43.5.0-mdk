package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Rime: Water Lily Bodhisattva. A colossal statue of clear ice (about 9 blocks tall) seated cross-legged on a
 * great crystal lotus: hands joined in prayer, four more arms raised behind it holding lotuses, a serene face with
 * closed eyes and a frost halo. It crystallises up out of the ground when summoned, rocks as it walks, exhales a
 * torrent of freezing mist for its breath attack and sweeps a giant hand for its swipe (the swing lands on the
 * damage tick). Replaces the placeholder GeckoLib boxes.
 */
public class BodhisattvaRenderer extends EntityRenderer<BodhisattvaEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL = 0xF000F0;
    private static final int ICE = DomaRenderHelper.ICE;
    private static final int ICE_FACE = 0xA6E4F6FF;

    public BodhisattvaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 3.0f;
    }

    @Override
    public void render(BodhisattvaEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        // Track how long the current animation state has been running (client side).
        int state = entity.getAnimationState();
        if (state != entity.clientState) {
            entity.clientState = state;
            entity.clientStateStart = entity.tickCount;
        }
        float stateAge = age - entity.clientStateStart;
        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 origin = entity.getPosition(partialTick);

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float emerge = Mth.clamp(age / 20.0f, 0.0f, 1.0f);
        emerge = 1.0f - (1.0f - emerge) * (1.0f - emerge);
        float bob = state == 1 ? 0.25f * Math.abs(Mth.sin(age * 0.25f)) : 0.05f * Mth.sin(age * 0.05f);
        float roll = state == 1 ? 0.04f * Mth.sin(age * 0.25f) : 0.0f;

        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(-bodyYaw));
        pose.translate(0, -9.5 * (1.0 - emerge) + bob, 0);
        pose.mulPose(Vector3f.ZP.rotation(roll));
        // Camera in this rotated frame, for the additive edges and rings.
        Vec3 camWorld = camera.getPosition().subtract(origin).add(0, 9.5 * (1.0 - emerge) - bob, 0);
        Vec3 cam = camWorld.yRot(bodyYaw * Mth.DEG_TO_RAD); // inverse of the YP(-bodyYaw) above

        Vec3 R = new Vec3(1, 0, 0), U = new Vec3(0, 1, 0), F = new Vec3(0, 0, 1);

        // Pedestal: a great crystal lotus.
        DomaRenderHelper.lotus(pose, buffers, new Vec3(0, 0.1, 0), 7.5f, 1.0f, age * 0.003f, FULL, cam);
        // Seated, cross-legged lower body.
        DomaRenderHelper.iceHexa(pose, buffers, DomaRenderHelper.frameBox(new Vec3(0, 1.3, 0.25), R, U, F, 2.5, 0.7, 1.6, 0.9), ICE, FULL, cam, 0.6f);
        // Torso, robe falling to the lap.
        DomaRenderHelper.iceHexa(pose, buffers, DomaRenderHelper.frameBox(new Vec3(0, 3.8, 0), R, U, F, 1.7, 1.9, 1.0, 0.72), ICE, FULL, cam, 0.6f);
        DomaRenderHelper.iceLimb(pose, buffers, new Vec3(0, 5.6, 0), new Vec3(0, 6.3, 0), 0.5, 0.45, ICE, FULL, cam);

        // Head (tilting forward for the breath).
        float breath = state == 2 ? Mth.clamp(stateAge / 4.0f, 0.0f, 1.0f) * Mth.clamp((40.0f - stateAge) / 4.0f, 0.0f, 1.0f) : 0.0f;
        pose.pushPose();
        pose.translate(0, 6.3, 0);
        pose.mulPose(Vector3f.XP.rotation(0.25f * breath));
        Vec3 o = Vec3.ZERO;
        Vec3 headCam = cam.subtract(0, 6.3, 0).xRot(0.25f * breath);
        DomaRenderHelper.iceHexa(pose, buffers, DomaRenderHelper.frameBox(o.add(0, 0.95, 0.05), R, U, F, 0.9, 0.95, 0.85, 0.85), ICE_FACE, FULL, headCam, 0.5f);
        DomaRenderHelper.iceHexa(pose, buffers, DomaRenderHelper.frameBox(o.add(0, 2.15, 0), R, U, F, 0.45, 0.32, 0.45, 0.55), ICE, FULL, headCam, 0.5f);
        for (int side = -1; side <= 1; side += 2) {
            DomaRenderHelper.iceHexa(pose, buffers, DomaRenderHelper.frameBox(o.add(side * 0.98, 0.75, 0), R, U, F, 0.1, 0.6, 0.18, 0.8), ICE, FULL, headCam, 0.3f);
        }
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        // Serene closed eyes, mouth (opening for the breath) and the urna.
        for (int side = -1; side <= 1; side += 2) {
            Vec3 e0 = new Vec3(side * 0.18, 1.12, 0.92), e1 = new Vec3(side * 0.62, 1.16, 0.88);
            ThreadRenderHelper.ribbon(pose, vc, headCam, e0, e1, 0.05f, 0xC0E8F8FF);
        }
        ThreadRenderHelper.ribbon(pose, vc, headCam, new Vec3(-0.25, 0.48 - 0.12 * breath, 0.93), new Vec3(0.25, 0.48 - 0.12 * breath, 0.93), 0.05f + 0.25f * breath, 0xB0E8F8FF);
        ThreadRenderHelper.ribbon(pose, vc, headCam, new Vec3(0, 1.4, 0.93), new Vec3(0, 1.48, 0.93), 0.1f, 0xE0F4FBFF);
        pose.popPose();

        // Frost halo behind the head.
        float spin = age * 0.01f;
        Vec3 haloC = new Vec3(0, 7.3, -1.1);
        HantenguRenderHelper.soundRing(pose, buffers, cam, haloC, F, 2.6f, 0.25f, 0x80CFF4FF, emerge);
        HantenguRenderHelper.soundRing(pose, buffers, cam, haloC, F, 2.1f, 0.12f, 0x60E8F8FF, emerge);
        vc = ThreadRenderHelper.buffer(buffers);
        for (int k = 0; k < 12; k++) {
            double a = spin + Math.PI * 2.0 * k / 12;
            ThreadRenderHelper.ribbon(pose, vc, cam, haloC.add(Math.cos(a) * 2.15, Math.sin(a) * 2.15, 0), haloC.add(Math.cos(a) * 2.6, Math.sin(a) * 2.6, 0), 0.06f, 0x70E8F8FF);
        }

        // Main arms: joined in prayer; the right one (-X) sweeps for the swipe, both open for the breath.
        Vec3 shL = new Vec3(1.9, 5.5, 0), shR = new Vec3(-1.9, 5.5, 0);
        Vec3 handL = new Vec3(0.22, 4.7, 1.7), handR = new Vec3(-0.22, 4.7, 1.7);
        Vec3 elL = new Vec3(2.2, 4.0, 0.9), elR = new Vec3(-2.2, 4.0, 0.9);
        if (breath > 0.0f) {
            handL = handL.lerp(new Vec3(3.4, 4.4, 1.6), breath);
            handR = handR.lerp(new Vec3(-3.4, 4.4, 1.6), breath);
            elL = elL.lerp(new Vec3(2.9, 4.2, 0.6), breath);
            elR = elR.lerp(new Vec3(-2.9, 4.2, 0.6), breath);
        }
        float swingArc = Float.NaN;
        if (state == 3) {
            float s = stateAge;
            Vec3 back = new Vec3(-5.0, 6.8, -2.4);
            if (s < 12.0f) {
                float k = smooth(s / 12.0f);
                handR = handR.lerp(back, k);
                elR = elR.lerp(new Vec3(-3.4, 6.0, -1.0), k);
            } else if (s < 26.0f) {
                // Swing: around the front at chest height, from the right-back to the left-front (lands at s = 20).
                float k = Mth.clamp((s - 12.0f) / 8.0f, 0.0f, 1.0f);
                k = k * k * (3 - 2 * k);
                handR = swing(k, 6.0);
                elR = shR.lerp(handR, 0.5).add(0, 0.6, 0);
                swingArc = k;
            } else {
                float k = smooth(Mth.clamp((s - 26.0f) / 14.0f, 0.0f, 1.0f));
                Vec3 end = swing(1.0f, 6.0);
                handR = end.lerp(handR, k);
                elR = shR.lerp(end, 0.5).add(0, 0.6, 0).lerp(elR, k);
            }
        }
        arm(pose, buffers, shL, elL, handL, cam);
        arm(pose, buffers, shR, elR, handR, cam);

        // Four raised arms behind, holding lotuses, swaying gently.
        for (int side = -1; side <= 1; side += 2) {
            float sway = 0.25f * Mth.sin(age * 0.05f + side);
            Vec3 s1 = new Vec3(side * 1.5, 5.2, -0.5);
            Vec3 e1 = new Vec3(side * 3.2, 6.3 + sway, -0.7);
            Vec3 h1 = new Vec3(side * 3.6, 8.0 + sway, -0.5);
            arm(pose, buffers, s1, e1, h1, cam);
            DomaRenderHelper.lotus(pose, buffers, h1.add(0, 0.35, 0), 1.3f, 1.0f, age * 0.01f + side, FULL, cam);
            Vec3 s2 = new Vec3(side * 1.5, 4.8, -0.3);
            Vec3 e2 = new Vec3(side * 3.4, 4.5 - sway, -0.3);
            Vec3 h2 = new Vec3(side * 4.4, 3.9 - sway, 0.5);
            arm(pose, buffers, s2, e2, h2, cam);
        }

        // Swipe: a crescent of frost carved through the air by the hand (radius ~8, as the damage zone).
        if (!Float.isNaN(swingArc)) {
            vc = ThreadRenderHelper.buffer(buffers);
            Vec3 prev = null;
            int segs = 20;
            for (int i = 0; i <= segs; i++) {
                Vec3 p = swing(swingArc * i / segs, 8.0);
                if (prev != null) {
                    float w = (float) (0.4 + 1.6 * Math.sin(Math.PI * (double) i / segs));
                    ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, w, 0x70A8E4FF);
                    ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, w * 0.3f, 0xD0F0FBFF);
                }
                prev = p;
            }
        }
        // Emergence: frost ring racing out over the ground.
        if (age < 20.0f) {
            float t = age / 20.0f;
            HantenguRenderHelper.soundRing(pose, buffers, cam, new Vec3(0, 9.5 * (1.0 - emerge) + 0.1, 0), U, 2.0f + 8.0f * t, 0.6f, 0x90CFF4FF, 1.0f - t);
        }
        pose.popPose();

        // Breath: a torrent of freezing mist from the mouth along the damage cone (drawn in world-aligned space).
        if (breath > 0.0f) {
            Vec3 mouthLocal = new Vec3(0, 6.3 + 0.45 + bob - 9.5 * (1.0 - emerge), 1.2);
            Vec3 mouth = rotY(mouthLocal, bodyYaw);
            Vec3 look = entity.getViewVector(partialTick);
            Vec3 eye = entity.getEyePosition(partialTick).subtract(origin);
            for (int i = 0; i < 26; i++) {
                float q = ((stateAge * 0.08f) + i / 26.0f) % 1.0f;
                double d = 2.0 + q * 22.0;
                Vec3 along = eye.add(look.scale(d));
                Vec3 p = mouth.lerp(along, Math.min(1.0, q * 1.6));
                java.util.Random rnd = new java.util.Random(entity.getId() * 31L + i);
                double width = d * 0.6;
                Vec3 side = new Vec3(-look.z, 0, look.x);
                side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
                p = p.add(side.scale((rnd.nextDouble() - 0.5) * width)).add(0, (rnd.nextDouble() - 0.5) * width * 0.4, 0);
                DomaRenderHelper.puff(pose, buffers, camera, p, (float) (1.5 + d * 0.25), i + stateAge * 0.03f, 0.55f * breath * (1.0f - q * 0.6f));
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Rotate a statue-local offset into world-aligned space (the inverse of the render rotation). */
    private static Vec3 rotY(Vec3 local, float bodyYawDeg) {
        double a = Math.toRadians(-bodyYawDeg);
        double cos = Math.cos(a), sin = Math.sin(a);
        // Matches Vector3f.YP.rotationDegrees(-bodyYaw): x' = x cos + z sin, z' = -x sin + z cos
        return new Vec3(local.x * cos + local.z * sin, local.y, -local.x * sin + local.z * cos);
    }

    /**
     * The swiping hand's path at progress {@code k}: around the statue at chest height from behind its right
     * (-X) side, through straight ahead (+Z), to its front-left, {@code radius} out.
     */
    private static Vec3 swing(float k, double radius) {
        double phi = Math.toRadians(-150 + 210 * k); // angle from +Z toward +X
        return new Vec3(Math.sin(phi) * radius, 4.6, Math.cos(phi) * radius);
    }

    private static void arm(PoseStack pose, MultiBufferSource buffers, Vec3 shoulder, Vec3 elbow, Vec3 hand, Vec3 cam) {
        DomaRenderHelper.iceLimb(pose, buffers, shoulder, elbow, 0.42, 0.34, ICE, FULL, cam);
        DomaRenderHelper.iceLimb(pose, buffers, elbow, hand, 0.34, 0.26, ICE, FULL, cam);
        Vec3 dir = hand.subtract(elbow);
        Vec3 tip = hand.add(dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0.5, 0) : dir.normalize().scale(0.55));
        DomaRenderHelper.iceLimb(pose, buffers, hand, tip, 0.3, 0.18, ICE_FACE, FULL, cam);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }

    @Override
    public boolean shouldRender(BodhisattvaEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(9.0, 12.0, 9.0).move(0, 5.0, 0));
    }

    @Override
    public ResourceLocation getTextureLocation(BodhisattvaEntity entity) {
        return TEXTURE;
    }
}
