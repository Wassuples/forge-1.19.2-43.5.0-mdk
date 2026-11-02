package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HantenguDrumsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Zohakuten's drums: five red taiko drums joined by a circular wooden frame that floats behind the owner's back,
 * each skin painted with 憎 (hatred) and facing backward. The drum that was just beaten shudders, glows with a
 * vibrating red light and throws off a ring of sound. Hidden in
 * the owner's own first-person view.
 */
public class HantenguDrumsRenderer extends EntityRenderer<HantenguDrumsEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    /** Angles (degrees, 90 = straight up) of the five drums, evenly spaced around the circular frame. */
    private static final float[] ANGLES = {90.0f, 18.0f, 162.0f, -54.0f, 234.0f};

    public HantenguDrumsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(HantenguDrumsEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity owner = entity.getOwner();
        if (!(owner instanceof LivingEntity living)) return;
        Minecraft mc = Minecraft.getInstance();
        if (owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return;

        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 5.0f, 0.0f, 1.0f) * Mth.clamp((entity.getDuration() - age) / 5.0f, 0.0f, 1.0f);
        if (appear <= 0.0f) return;
        float body = living.getBbHeight() / 1.8f;
        float scale = body * (0.4f + 0.6f * appear); // drums grow out of the back as they appear

        Vec3 origin = entity.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 centre = living.getPosition(partialTick).subtract(origin)
                .add(0, 1.25 * body, 0)
                .subtract(forward.scale(0.55 * body));
        float ringR = 0.78f * scale;

        // Wooden ring frame joining the drums.
        int frameSegs = 30;
        Vec3[] frame = new Vec3[frameSegs + 1];
        float[] frameR = new float[frameSegs + 1];
        for (int s = 0; s <= frameSegs; s++) {
            double a = Math.PI * 2.0 * s / frameSegs;
            frame[s] = centre.add(right.scale(Math.cos(a) * ringR)).add(up.scale(Math.sin(a) * ringR)).subtract(forward.scale(0.05 * scale));
            frameR[s] = 0.05f * scale;
        }
        HantenguRenderHelper.woodTube(pose, buffers, light, frame, frameR, 6, 2);
        // Strut from the back to the ring
        HantenguRenderHelper.woodTube(pose, buffers, light,
                new Vec3[]{centre.add(forward.scale(0.4 * scale)).add(0, -0.25 * scale, 0), centre.add(0, -0.1 * scale, 0), centre.add(up.scale(ringR * 0.2))},
                new float[]{0.07f * scale, 0.06f * scale, 0.05f * scale}, 6, 5);

        int lastBeat = entity.getLastBeat();
        int beatDrum = entity.getBeatDrum();
        float sinceBeat = age - lastBeat;
        for (int i = 0; i < HantenguDrumsEntity.DRUMS; i++) {
            double a = Math.toRadians(ANGLES[i]);
            Vec3 at = centre.add(right.scale(Math.cos(a) * ringR)).add(up.scale(Math.sin(a) * ringR));
            float shudder = (i == beatDrum && sinceBeat >= 0 && sinceBeat < 6) ? 1.0f - sinceBeat / 6.0f : 0.0f;
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            HantenguRenderHelper.faceAlong(pose, forward.scale(-1));
            float s = 1.0f + 0.08f * shudder * Mth.sin(sinceBeat * 3.0f);
            pose.scale(s * scale, s * scale, s * scale);
            // The struck drum gives off a vibrating red light.
            float glow = shudder * (0.75f + 0.25f * Mth.sin(sinceBeat * 4.0f));
            HantenguRenderHelper.taikoDrum(pose, buffers, light, 0.25f, 0.28f, shudder, glow);
            pose.popPose();
        }

        // Ring of sound off the drum that was just struck (drawn after all solids).
        if (sinceBeat >= 0 && sinceBeat < 8) {
            double a = Math.toRadians(ANGLES[Math.floorMod(beatDrum, HantenguDrumsEntity.DRUMS)]);
            Vec3 at = centre.add(right.scale(Math.cos(a) * ringR)).add(up.scale(Math.sin(a) * ringR)).subtract(forward.scale(0.2 * scale));
            Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
            float k = sinceBeat / 8.0f;
            for (int r = 0; r < 2; r++) {
                float kk = Mth.clamp(k - r * 0.2f, 0.0f, 1.0f);
                HantenguRenderHelper.soundRing(pose, buffers, cam, at.subtract(forward.scale(kk * 0.8)), forward,
                        (0.25f + 1.3f * kk) * scale, 0.14f, 0x80FF2A1E, 1.0f - kk);
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HantenguDrumsEntity entity) {
        return TEXTURE;
    }
}
