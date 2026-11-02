package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.SoundBlastEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Compressed Sound Waves (Kyōatsu Meiha): Zohakuten draws air in, then opens his mouth and lets out a deafening
 * cry compressed into a thin beam: a white-hot core along the hitscan line, rings of compressed air racing down
 * it, a shock cone at the mouth and a burst of rings where the blast lands.
 */
public class SoundBlastRenderer extends EntityRenderer<SoundBlastEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int RINGS = 7;

    public SoundBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(SoundBlastEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 end = entity.getEndOffset();
        Vec3 beamDir = end.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : end.normalize();
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);

        // Zohakuten unleashes the sound from his own mouth (falls back to the line start if he isn't loaded).
        Vec3 mouth = Vec3.ZERO;
        Entity owner = entity.getOwner();
        if (owner instanceof LivingEntity living) {
            Vec3 look = living.getViewVector(partialTick);
            mouth = living.getEyePosition(partialTick).subtract(origin).add(0, -0.22 * living.getBbHeight() / 1.8, 0).add(look.scale(0.35));
        }
        // Wind-up: air being drawn into the mouth as converging rings.
        if (age < SoundBlastEntity.CHARGE + 1) {
            float k = Mth.clamp(age / (SoundBlastEntity.CHARGE + 1), 0.0f, 1.0f);
            for (int r = 0; r < 3; r++) {
                float kk = Mth.clamp(k + r * 0.25f, 0.0f, 1.0f);
                HantenguRenderHelper.soundRing(pose, buffers, cam, mouth.add(beamDir.scale((1.0 - kk) * 1.6)), beamDir,
                        0.15f + (1.0f - kk) * 1.1f, 0.08f, 0x70E8F0FF, kk);
            }
        }

        float blast = age - SoundBlastEntity.CHARGE;
        if (blast > 0.0f) {
            float fade = Mth.clamp(1.0f - (blast - 4.0f) / 9.0f, 0.0f, 1.0f);
            Vec3 path = end.subtract(mouth);
            double length = path.length();
            Vec3 dir = length < 1.0E-4 ? beamDir : path.scale(1.0 / length);
            // Core and glow, shooting out over the first two ticks.
            float reach = Mth.clamp(blast / 2.0f, 0.0f, 1.0f);
            Vec3 front = mouth.add(path.scale(reach));
            if (fade > 0.0f) {
                var vc = ThreadRenderHelper.buffer(buffers);
                ThreadRenderHelper.ribbon(pose, vc, cam, mouth, front, 0.9f * fade, ThreadRenderHelper.scaleAlpha(0x46B6C8FF, fade));
                ThreadRenderHelper.ribbon(pose, vc, cam, mouth, front, 0.38f * fade, ThreadRenderHelper.scaleAlpha(0x90E8F0FF, fade));
                ThreadRenderHelper.ribbon(pose, vc, cam, mouth, front, 0.16f, ThreadRenderHelper.scaleAlpha(0xF0FFFFFF, fade));
                // Pressure rings racing down the line.
                for (int r = 0; r < RINGS; r++) {
                    double k = ((blast * 0.45) + (double) r / RINGS) % 1.0;
                    if (k > reach) continue;
                    Vec3 at = mouth.add(path.scale(k));
                    float radius = (float) (0.25 + 0.3 * Math.sin(Math.PI * k) + 0.1 * k * Math.min(1.0, length / 10.0));
                    HantenguRenderHelper.soundRing(pose, buffers, cam, at, dir, radius, 0.16f, 0x90D8E8FF, fade);
                }
            }
            // Shock cone at the jaws.
            float cone = Mth.clamp(blast / 6.0f, 0.0f, 1.0f);
            if (cone < 1.0f) {
                for (int r = 0; r < 3; r++) {
                    float k = Mth.clamp(cone * 1.4f - r * 0.2f, 0.0f, 1.0f);
                    HantenguRenderHelper.soundRing(pose, buffers, cam, mouth.add(dir.scale(0.3 + k * 1.5)), dir,
                            0.3f + k * (0.9f + r * 0.4f), 0.2f, 0xA0FFF2D8, 1.0f - k);
                }
            }
            // Burst where it lands.
            float burst = Mth.clamp((blast - 1.5f) / 9.0f, 0.0f, 1.0f);
            if (burst > 0.0f && burst < 1.0f) {
                float big = entity.didHit() ? 1.0f : 0.6f;
                for (int r = 0; r < 3; r++) {
                    float k = Mth.clamp(burst * 1.3f - r * 0.15f, 0.0f, 1.0f);
                    HantenguRenderHelper.soundRing(pose, buffers, cam, end, dir, (0.3f + k * 3.2f) * big, 0.25f, 0xB0E8F4FF, 1.0f - k);
                }
                // Perpendicular ring so it also reads from the side.
                Vec3 side = dir.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() > 1.0E-4) {
                    HantenguRenderHelper.soundRing(pose, buffers, cam, end, side, (0.3f + burst * 2.4f) * big, 0.2f, 0x80E8F4FF, 1.0f - burst);
                }
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SoundBlastEntity entity) {
        return TEXTURE;
    }
}
