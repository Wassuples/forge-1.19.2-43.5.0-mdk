package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.FishSchoolEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shoals of goldfish weaving along each stream, then a slimy green splash ring at the impact point. */
public class FishSchoolRenderer extends EntityRenderer<FishSchoolEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FISH_PER_STREAM = 5;

    public FishSchoolRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(FishSchoolEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        int index = 0;
        for (Vec3[] stream : entity.getStreams()) {
            Vec3 start = stream[0].subtract(origin);
            Vec3 end = stream[1].subtract(origin);
            Vec3 path = end.subtract(start);
            Vec3 side = path.cross(new Vec3(0, 1, 0));
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            Vec3 up = side.cross(path).normalize();
            for (int f = 0; f < FISH_PER_STREAM; f++) {
                float t = (age - f * 1.2f) / FishSchoolEntity.TRAVEL_TICKS;
                if (t <= 0.0f || t >= 1.0f) continue;
                double weave = Math.sin(t * Math.PI * 3.0 + index + f) * 0.6 * Math.sin(Math.PI * t);
                double bob = Math.cos(t * Math.PI * 2.0 + f) * 0.35 * Math.sin(Math.PI * t);
                Vec3 p = start.add(path.scale(t)).add(side.scale(weave)).add(up.scale(bob));
                Vec3 ahead = start.add(path.scale(Math.min(1.0, t + 0.03)))
                        .add(side.scale(Math.sin((t + 0.03) * Math.PI * 3.0 + index + f) * 0.6 * Math.sin(Math.PI * (t + 0.03))));
                Vec3 dir = ahead.subtract(p);
                if (dir.lengthSqr() < 1.0E-6) dir = path;
                dir = dir.normalize();
                pose.pushPose();
                pose.translate(p.x, p.y, p.z);
                pose.mulPose(Vector3f.YP.rotationDegrees((float) (-Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG)));
                pose.mulPose(Vector3f.XP.rotationDegrees((float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG)));
                GyokkoRenderHelper.goldfish(pose, buffers, light, 0.38f, age * 2.0f + f, 0.6f);
                pose.popPose();
                // Slime trail. The goldfish above pulled other render types from the buffer source, which ends
                // the lightning batch, so the consumer must be fetched again here (holding it across caused the
                // "Not filled all elements of the vertex" crash).
                ThreadRenderHelper.thread(pose, ThreadRenderHelper.buffer(buffers), cam, p, p.subtract(dir.scale(0.8)), 0x90D8FFB0, 0x4060C040, 0.7f);
            }
            // Splash at the impact point once the shoal arrives.
            float splash = (age - FishSchoolEntity.TRAVEL_TICKS) / 8.0f;
            if (splash > 0.0f && splash < 1.0f) {
                VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
                float r = 0.4f + splash * 2.2f;
                Vec3 prev = null;
                for (int s = 0; s <= 24; s++) {
                    double a = Math.PI * 2.0 * s / 24;
                    Vec3 q = end.add(Math.cos(a) * r, Math.sin(a * 3.0) * 0.1, Math.sin(a) * r);
                    if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, q, 0xC0E0FFB8, 0x5060D040, 1.0f - splash);
                    prev = q;
                }
            }
            index++;
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FishSchoolEntity entity) {
        return TEXTURE;
    }
}
