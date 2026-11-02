package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WoodDragonStrikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Countless Striking Trees: a wooden dragon bursts out of the ground beside the target and its head slams down
 * onto the strike point (two ticks, matching the damage), grinds there, rears back roaring, then sinks into the
 * earth. Cracks splay out from the impact and from where the neck tore out of the ground.
 */
public class WoodDragonStrikeRenderer extends EntityRenderer<WoodDragonStrikeEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final float HEAD_SCALE = 1.35f;
    private static final int NECK_POINTS = 16;

    public WoodDragonStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(WoodDragonStrikeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 root = entity.getRootOffset();
        Vec3 strike = Vec3.ZERO;
        Vec3 flat = new Vec3(-root.x, 0, -root.z);
        Vec3 toward = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();

        // Final slam pose: head pointing steeply down onto the strike point, snout touching it.
        Vec3 slamFacing = toward.scale(0.45).add(0, -1, 0).normalize();
        float headLen = 1.75f * HEAD_SCALE;
        Vec3 slamHead = strike.subtract(slamFacing.scale(headLen)).add(0, 0.1, 0);
        // Reared pose: high above the root, looking at where it struck, jaws wide.
        Vec3 rearHead = root.add(up.scale(6.5)).add(toward.scale(0.6));
        Vec3 rearFacing = strike.add(0, 1.0, 0).subtract(rearHead).normalize().add(0, 0.5, 0).normalize();

        Vec3 head;
        Vec3 facing;
        float mouth;
        float sink = 0.0f;
        float quiver = 0.0f;
        if (age < WoodDragonStrikeEntity.SLAM_TICKS) {
            float p = age / WoodDragonStrikeEntity.SLAM_TICKS;
            float e = p * p; // accelerating into the slam
            Vec3 start = root.add(0, 0.5, 0);
            Vec3 apex = root.lerp(slamHead, 0.5).add(up.scale(4.0));
            head = HantenguRenderHelper.bezier(start, apex, slamHead, e);
            Vec3 ahead = HantenguRenderHelper.bezier(start, apex, slamHead, Math.min(1.0, e + 0.05));
            facing = ahead.subtract(head).lengthSqr() < 1.0E-6 ? slamFacing : ahead.subtract(head).normalize().lerp(slamFacing, e).normalize();
            mouth = 0.6f * (1.0f - e);
        } else if (age < WoodDragonStrikeEntity.HOLD_END) {
            head = slamHead;
            facing = slamFacing;
            mouth = 0.0f;
            quiver = 1.0f - (age - WoodDragonStrikeEntity.SLAM_TICKS) / (WoodDragonStrikeEntity.HOLD_END - WoodDragonStrikeEntity.SLAM_TICKS);
        } else {
            float r = Mth.clamp((age - WoodDragonStrikeEntity.HOLD_END) / 6.0f, 0.0f, 1.0f);
            r = r * r * (3 - 2 * r);
            head = slamHead.lerp(rearHead, r);
            facing = slamFacing.lerp(rearFacing, r).normalize();
            float roar = Mth.clamp((age - WoodDragonStrikeEntity.HOLD_END - 3.0f) / 4.0f, 0.0f, 1.0f)
                    * Mth.clamp((WoodDragonStrikeEntity.REAR_END + 2.0f - age) / 4.0f, 0.0f, 1.0f);
            mouth = roar * (0.85f + 0.15f * Mth.sin(age * 2.5f));
            sink = Mth.clamp((age - WoodDragonStrikeEntity.REAR_END) / (WoodDragonStrikeEntity.LIFE - WoodDragonStrikeEntity.REAR_END), 0.0f, 1.0f);
        }
        if (quiver > 0.0f) {
            head = head.add(Mth.sin(age * 9.0f) * 0.06 * quiver, Mth.sin(age * 11.0f) * 0.04 * quiver, Mth.cos(age * 8.0f) * 0.06 * quiver);
        }
        Vec3 down = new Vec3(0, -sink * sink * 11.0, 0);

        // Neck: out of the ground at the root, up and over into the back of the skull.
        Vec3 neckEnd = head.subtract(facing.scale(0.15 * HEAD_SCALE));
        Vec3 groundBelow = root.add(0, -1.2, 0);
        double rise = Math.max(2.5, neckEnd.y - root.y + 2.5);
        Vec3 control = root.add(up.scale(rise)).add(toward.scale(0.4)).subtract(facing.scale(1.0));
        Vec3[] pts = new Vec3[NECK_POINTS];
        for (int i = 0; i < NECK_POINTS; i++) {
            double t = (double) i / (NECK_POINTS - 1);
            Vec3 p = HantenguRenderHelper.bezier(groundBelow, control, neckEnd, t);
            // Writhing: a small travelling sideways wave along the neck.
            Vec3 side = toward.cross(up);
            p = p.add(side.scale(Math.sin(t * 6.0 - age * 0.6) * 0.18 * Math.sin(Math.PI * t)));
            pts[i] = p.add(down);
        }
        HantenguRenderHelper.woodTube(pose, buffers, light, pts, HantenguRenderHelper.neckRadii(NECK_POINTS, 0.75f, 0.48f), 10, entity.getId() % 5);

        pose.pushPose();
        Vec3 h = head.add(down);
        pose.translate(h.x, h.y, h.z);
        HantenguRenderHelper.faceAlong(pose, facing);
        pose.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
        HantenguRenderHelper.dragonHead(pose, buffers, light, mouth, mouth * 0.6f);
        pose.popPose();

        // Ground cracks at the impact and where the neck burst out (fade over the life).
        float crackAlpha = Mth.clamp((WoodDragonStrikeEntity.LIFE - age) / 10.0f, 0.0f, 1.0f);
        if (age >= WoodDragonStrikeEntity.SLAM_TICKS - 0.5f) {
            cracks(pose, buffers, strike.add(0, 0.03, 0), 2.2f, 9, entity.getId(), crackAlpha);
        }
        cracks(pose, buffers, root.add(0, 0.03, 0), 1.6f, 7, entity.getId() + 3, crackAlpha);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Dark jagged cracks splaying out across the ground from {@code at}. */
    private static void cracks(PoseStack pose, MultiBufferSource buffers, Vec3 at, float length, int count, int seed, float alpha) {
        if (alpha <= 0.0f) return;
        java.util.Random rnd = new java.util.Random(seed * 7919L);
        int color = ToonRenderHelper.withAlpha(0xE01A120C, alpha);
        for (int c = 0; c < count; c++) {
            double a = Math.PI * 2.0 * (c + rnd.nextDouble() * 0.6) / count;
            Vec3 prev = at;
            double width = 0.09;
            int steps = 4;
            for (int s = 1; s <= steps; s++) {
                double d = length * s / steps * (0.7 + rnd.nextDouble() * 0.5);
                double aa = a + (rnd.nextDouble() - 0.5) * 0.5;
                Vec3 p = at.add(Math.cos(aa) * d, 0, Math.sin(aa) * d);
                Vec3 dir = p.subtract(prev);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                if (side.lengthSqr() < 1.0E-8) continue;
                side = side.normalize().scale(width);
                double w2 = width * (1.0 - (double) s / (steps + 1));
                Vec3 side2 = side.normalize().scale(w2);
                ToonRenderHelper.quad3(pose, buffers, color,
                        (float) (prev.x - side.x), (float) prev.y, (float) (prev.z - side.z),
                        (float) (prev.x + side.x), (float) prev.y, (float) (prev.z + side.z),
                        (float) (p.x + side2.x), (float) p.y, (float) (p.z + side2.z),
                        (float) (p.x - side2.x), (float) p.y, (float) (p.z - side2.z),
                        0, 1, 0);
                prev = p;
                width = w2;
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(WoodDragonStrikeEntity entity) {
        return TEXTURE;
    }
}
