package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WhisperWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Whispers of Forced Unconscious Hypnosis: a lipped mouth opens on the back of Enmu's raised hand and murmurs;
 * its whisper travels to the target as soft, widening pink-violet ripples carrying the word "sleep...", and
 * blooms into a ring around the target's head when it lands.
 */
public class WhisperWaveRenderer extends EntityRenderer<WhisperWaveEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    static final int DREAM_CORE = 0xC8FFD2F2;
    static final int DREAM_GLOW = 0x50A050FF;

    public WhisperWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(WhisperWaveEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity caster = entity.getCaster();
        Entity target = entity.getTarget();
        if (caster == null || target == null) return;

        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        Vec3 hand = WhisperWaveEntity.handPosition(caster, partialTick).subtract(origin);
        Vec3 head = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.85, 0).subtract(origin);

        Vec3 path = head.subtract(hand);
        double length = path.length();
        if (length < 1.0E-3) return;
        Vec3 dir = path.scale(1.0 / length);
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-4) u = dir.cross(new Vec3(1, 0, 0));
        u = u.normalize();
        Vec3 v = dir.cross(u).normalize();

        int arrived = entity.getArrivedTick();
        float sinceArrival = arrived < 0 ? -1.0f : age - arrived;
        float fadeOut = arrived < 0 ? 1.0f : Mth.clamp(1.0f - sinceArrival / 12.0f, 0.0f, 1.0f);

        drawHandMouth(pose, buffers, hand, age, fadeOut);

        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        double front = Math.min(entity.travelled(age), length);
        // Ripples left behind the travelling whisper front; each widens and fades as it ages.
        for (double d = front; d > 0.0; d -= 0.7) {
            double behind = front - d;
            float fade = (float) Math.max(0.0, 1.0 - behind / 5.0) * fadeOut;
            if (fade <= 0.0f) break;
            double radius = 0.12 + 0.35 * (d / Math.max(1.0, length)) + behind * 0.08;
            ring(pose, vc, cam, hand.add(dir.scale(d)), u, v, radius, age * 0.2 + d, fade);
        }
        // Spiral thread of sound wound around the path.
        Vec3 prev = null;
        for (double d = 0.0; d <= front; d += 0.25) {
            double a = d * 2.4 - age * 0.6;
            double r = 0.08 + 0.18 * Math.sin(Math.PI * d / Math.max(1.0, length));
            Vec3 p = hand.add(dir.scale(d)).add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
            if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, DREAM_CORE, DREAM_GLOW, 0.6f * fadeOut);
            prev = p;
        }
        // The landing bloom: two rings swelling around the target's head.
        if (sinceArrival >= 0.0f) {
            float bloom = Mth.clamp(sinceArrival / 10.0f, 0.0f, 1.0f);
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 x = new Vec3(1, 0, 0);
            Vec3 z = new Vec3(0, 0, 1);
            ring(pose, vc, cam, head, x, z, 0.3 + bloom * 0.9, age * 0.3, (1.0f - bloom) * 1.2f);
            ring(pose, vc, cam, head.add(up.scale(0.15)), x, z, 0.2 + bloom * 0.6, -age * 0.3, (1.0f - bloom));
        }
        // The whisper itself, drifting along with the front.
        if (arrived < 0 && front > 0.4) {
            Vec3 at = hand.add(dir.scale(front)).add(0, 0.25, 0);
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float s = 0.018f;
            pose.scale(-s, -s, s);
            String word = "sleep...";
            float wobble = Mth.sin(age * 0.8f) * 1.5f;
            int alpha = (int) (200 * Mth.clamp((float) front / 1.5f, 0.0f, 1.0f));
            if (alpha > 8) {
                this.getFont().drawInBatch(word, -this.getFont().width(word) / 2.0f, wobble, (alpha << 24) | 0xF6C8FF,
                        false, pose.last().pose(), buffers, false, 0, 0xF000F0);
            }
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void ring(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 centre, Vec3 u, Vec3 v, double radius,
                             double phase, float brightness) {
        int segments = 22;
        Vec3 prev = null;
        for (int s = 0; s <= segments; s++) {
            double a = Math.PI * 2.0 * s / segments + phase;
            double wobble = 1.0 + 0.06 * Math.sin(a * 5.0 + phase * 3.0);
            Vec3 p = centre.add(u.scale(Math.cos(a) * radius * wobble)).add(v.scale(Math.sin(a) * radius * wobble));
            if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, DREAM_CORE, DREAM_GLOW, brightness);
            prev = p;
        }
    }

    /** A pair of pale lips on the back of the hand, opening and closing as it whispers. */
    private void drawHandMouth(PoseStack pose, MultiBufferSource buffers, Vec3 hand, float age, float fade) {
        float appear = Mth.clamp(age / 3.0f, 0.0f, 1.0f) * fade;
        if (appear <= 0.02f) return;
        float open = (0.35f + 0.65f * Math.abs(Mth.sin(age * 0.9f))) * appear;
        pose.pushPose();
        pose.translate(hand.x, hand.y, hand.z);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float s = 0.012f * appear;
        pose.scale(-s, -s, s);
        // skin patch, lips, open mouth, teeth (z: larger = further from camera)
        ToonRenderHelper.ellipse(pose, buffers, 0xFFF2D7C6, 11.0f, 8.0f, 0.6f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFFB8485E, 8.0f, 2.0f + 4.5f * open, 0.4f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF1A0610, 6.0f, 0.6f + 3.2f * open, 0.2f);
        if (open > 0.4f) {
            ToonRenderHelper.quad(pose, buffers, 0xFFF6F2EA, -4.0f, -0.6f - 3.2f * open, 4.0f, -0.6f - 2.2f * open, 0.1f);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WhisperWaveEntity entity) {
        return TEXTURE;
    }
}
