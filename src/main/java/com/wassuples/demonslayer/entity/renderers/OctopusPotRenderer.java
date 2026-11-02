package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.OctopusPotEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Octopus Vase Hell: a large glazed vase with six huge octopus tentacles writhing up out of its mouth (their roots
 * start inside the neck, so they visibly come from within the pot). On every damage pulse each tentacle rears up
 * toward the spot the server picked for it, whips down onto it on the damage tick, and recoils, leaving a splash
 * ring where it hit.
 */
public class OctopusPotRenderer extends EntityRenderer<OctopusPotEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int TOP = 0xFFA3344F;
    private static final int UNDER = 0xFFE7A3AE;
    private static final int SUCKER = 0xFFFBE2E6;
    private static final float VASE_SCALE = 2.0f;
    private static final float REACH = (float) OctopusPotEntity.REACH;
    private static final int SEGMENTS = 18;

    public OctopusPotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0f;
    }

    @Override
    public void render(OctopusPotEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float emerge = Mth.clamp((age - 2.0f) / 14.0f, 0.0f, 1.0f);
        // Strike cycle, starting when the server picks the spots (AIM_LEAD ticks before the damage tick):
        // rear up 0-4, whip down 4-6 (lands on the damage tick), hold 6-8, recoil 8-14, idle 14-20.
        float c = (age - (OctopusPotEntity.PULSE - OctopusPotEntity.AIM_LEAD)) % OctopusPotEntity.PULSE;
        if (c < 0) c += OctopusPotEntity.PULSE;
        float strike;  // 0 idle .. 1 committed to the strike
        float slam;    // 0 reared back .. 1 slammed down
        if (c < 4) { strike = smooth(c / 4.0f); slam = 0.0f; }
        else if (c < 6) { strike = 1.0f; slam = smooth((c - 4.0f) / 2.0f); }
        else if (c < 8) { strike = 1.0f; slam = 1.0f; }
        else if (c < 14) { strike = 1.0f - smooth((c - 8.0f) / 6.0f); slam = 1.0f; }
        else { strike = 0.0f; slam = 0.0f; }
        if (age < OctopusPotEntity.PULSE - OctopusPotEntity.AIM_LEAD) strike = 0.0f; // still emerging

        List<Vec3> strikes = entity.getStrikes();

        pose.pushPose();
        pose.scale(VASE_SCALE, VASE_SCALE, VASE_SCALE);
        GyokkoRenderHelper.vase(pose, buffers, light, strike * slam * 0.5f);
        pose.popPose();

        float lipY = GyokkoRenderHelper.vaseMouthHeight() * VASE_SCALE;
        Vec3 up = new Vec3(0, 1, 0);
        for (int i = 0; i < OctopusPotEntity.TENTACLES; i++) {
            double rootAngle = Math.PI * 2.0 * i / OctopusPotEntity.TENTACLES;
            Vec3 rootOff = new Vec3(Math.cos(rootAngle) * 0.22, 0, Math.sin(rootAngle) * 0.22);
            Vec3 deep = rootOff.add(0, lipY - 0.6, 0);          // inside the neck, hidden by the vase
            Vec3 exit = rootOff.scale(1.15).add(0, lipY + 0.35 * Math.max(0.3f, emerge), 0);

            double theta = rootAngle + 0.3 * Math.sin(age * 0.03 + i);
            Vec3 out = new Vec3(Math.cos(theta), 0, Math.sin(theta));
            Vec3 side = new Vec3(-out.z, 0, out.x);
            Vec3 target = i < strikes.size() ? strikes.get(i) : out.scale(REACH * 0.8);

            Vec3[] pts = new Vec3[SEGMENTS + 1];
            pts[0] = deep;
            pts[1] = rootOff.add(0, lipY - 0.1, 0);
            int free = SEGMENTS - 2;
            for (int s = 0; s <= free; s++) {
                double t = (double) s / free;
                // Idle: rising out of the mouth and curling over like a question mark, swaying.
                Vec3 idle = exit.add(up.scale(3.0 * t * emerge))
                        .add(out.scale((0.4 + 1.8 * t * t) * emerge))
                        .add(side.scale(Math.sin(t * 5.0 + age * 0.15 + i) * 0.35 * t))
                        .add(0, -Math.max(0, t - 0.7) * 2.5 * emerge, 0);
                // Strike: reared high toward the spot, then whipped down so the tip lands on it.
                Vec3 toTarget = target.subtract(exit);
                double reach = Mth.lerp(slam, 0.4f, 1.0f);
                double arc = Mth.lerp(slam, 3.4f, 0.9f) * Math.sin(Math.PI * Math.min(1.0, t * (slam < 1 ? 1.3 : 1.0)));
                double rear = Mth.lerp(slam, 2.2f, 0.0f) * t;
                Vec3 lash = exit.add(toTarget.scale(t * reach)).add(up.scale(arc + rear))
                        .add(side.scale(Math.sin(t * 7.0 + i + age * 0.4) * 0.18 * (1.0 - slam * 0.6)));
                pts[s + 2] = idle.lerp(lash, strike);
            }
            float thick = 0.24f * Math.max(0.25f, emerge);
            GyokkoRenderHelper.tentacle(pose, buffers, pts, thick, 0.04f, TOP, UNDER, SUCKER);
        }

        // Splash rings where the tentacles landed (drawn after all tentacles, with a freshly fetched buffer).
        if (strike > 0.3f && slam >= 1.0f && c < 11 && !strikes.isEmpty()) {
            VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
            Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick));
            float k = Mth.clamp((c - 6.0f) / 5.0f, 0.0f, 1.0f);
            float r = 0.6f + 1.8f * k;
            for (Vec3 hit : strikes) {
                Vec3 prev = null;
                for (int s = 0; s <= 20; s++) {
                    double a = Math.PI * 2.0 * s / 20;
                    Vec3 p = hit.add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                    if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, 0xC0D8F0FF, 0x405AA8FF, 1.0f - k);
                    prev = p;
                }
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }

    /** Tentacles reach 10 blocks out; keep drawing when only they are on screen. */
    @Override
    public boolean shouldRender(OctopusPotEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(REACH + 1.0, 4.0, REACH + 1.0));
    }

    @Override
    public ResourceLocation getTextureLocation(OctopusPotEntity entity) {
        return TEXTURE;
    }
}
