package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ObiSashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Daki's obi sashes as real cloth ribbons. Each sash leaves the bow at her back, sweeps wide to the side, then
 * whips forward so its pointed tip lands on her line of aim (the same line the slash damages) on its damage tick.
 * It then pulls back into its own place in a fan around the aim line and keeps writhing there, so all eight are
 * visible together (also from behind in third person) before they retract. The ribbon
 * twists along its length so both faces show, ripples while it travels, and is patterned like her obi:
 * pink silk with a black hem on both edges, fold shading and small gold flower diamonds down the middle.
 */
public class ObiSashRenderer extends EntityRenderer<ObiSashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int SILK = 0xFFF2A7C3;
    private static final int SILK_FOLD = 0xFFD9839F;
    private static final int SILK_UNDER = 0xFFC86A8B;
    private static final int HEM = 0xFF1E0F16;
    private static final int GOLD = 0xFFE8C15A;
    private static final int SEGMENTS = 26;
    private static final float WIDTH = 0.5f;

    public ObiSashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ObiSashEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity owner = entity.getOwner();
        if (!(owner instanceof LivingEntity living)) return;
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);

        float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 bodyForward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
        Vec3 bodySide = new Vec3(-bodyForward.z, 0, bodyForward.x); // owner's right
        float scale = living.getBbHeight() / 1.8f;
        Vec3 bow = living.getPosition(partialTick).add(0, 1.0 * scale, 0).subtract(bodyForward.scale(0.25 * scale)).subtract(origin);

        Vec3 eye = living.getEyePosition(partialTick).subtract(origin);
        Vec3 look = living.getViewVector(partialTick);
        // Basis around the aim line so each sash gets its own place in the fan.
        Vec3 aimRight = look.cross(new Vec3(0, 1, 0));
        aimRight = aimRight.lengthSqr() < 1.0E-4 ? bodySide : aimRight.normalize();
        Vec3 aimUp = aimRight.cross(look).normalize();
        float range = entity.getRange();

        for (int i = 0; i < ObiSashEntity.SASHES; i++) {
            float ext = ObiSashEntity.extension(i, age);
            if (ext <= 0.0f) continue;
            float settle = ObiSashEntity.settle(i, age);
            // Each sash owns a direction around the aim line (golden-angle spread so neighbours never overlap).
            double around = i * 2.39996 + 0.6;
            double ringR = Mth.lerp(settle, 0.35f, 2.6f + 0.25f * (i % 3)) * scale;
            double sway = Math.sin(age * 0.18 + i * 1.7) * 0.5 * settle;
            Vec3 ringDir = aimRight.scale(Math.cos(around + sway * 0.3)).add(aimUp.scale(Math.sin(around + sway * 0.3)));
            // On its strike the tip stabs the aim line at full range; afterwards it pulls back and hovers in the fan.
            double reach = range * Mth.lerp(settle, 1.0f, 0.62f + 0.06f * (i % 4));
            Vec3 tip = eye.add(look.scale(reach)).add(ringDir.scale(ringR)).add(0, sway * 0.4, 0);
            // Leave the bow on alternating sides, sweeping wide and high before curving in, so every sash has its
            // own silhouette in third person instead of collapsing onto the crosshair behind the body.
            float side = (i % 2 == 0 ? 1.0f : -1.0f) * (2.0f + 0.45f * (i / 2)) * scale;
            float lift = (0.6f + 0.55f * ((i * 3) % 4)) * scale;
            Vec3 root = bow.add(bodySide.scale((i % 2 == 0 ? 0.12 : -0.12) * scale)).add(0, (i / 2) * 0.06 * scale, 0);
            Vec3 control = root.add(bodySide.scale(side)).add(0, lift, 0).add(bodyForward.scale(2.2 * scale))
                    .add(ringDir.scale(1.2 * settle));
            drawSash(pose, buffers, root, control, tip, ext, age, i);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void drawSash(PoseStack pose, MultiBufferSource buffers, Vec3 p0, Vec3 p1, Vec3 p2, float ext,
                                 float age, int index) {
        int shown = Math.max(2, Math.round(SEGMENTS * ext));
        Vec3[] centre = new Vec3[shown + 1];
        Vec3[] across = new Vec3[shown + 1];
        float phase = index * 1.3f;
        for (int s = 0; s <= shown; s++) {
            float t = (float) s / SEGMENTS;
            Vec3 c = bezier(p0, p1, p2, t);
            Vec3 tangent = bezier(p0, p1, p2, Math.min(1.0f, t + 0.01f)).subtract(bezier(p0, p1, p2, Math.max(0.0f, t - 0.01f)));
            if (tangent.lengthSqr() < 1.0E-8) tangent = p2.subtract(p0);
            tangent = tangent.normalize();
            Vec3 n1 = tangent.cross(new Vec3(0, 1, 0));
            if (n1.lengthSqr() < 1.0E-4) n1 = tangent.cross(new Vec3(1, 0, 0));
            n1 = n1.normalize();
            Vec3 n2 = tangent.cross(n1).normalize();
            // Ripple travelling down the sash (damped at the root and tip), and a lengthwise twist.
            double ripple = Math.sin(t * Math.PI * 4.0 - age * 1.1 + phase) * 0.35 * Math.sin(Math.PI * t) * (1.0 - 0.5 * ext);
            c = c.add(n2.scale(ripple));
            double twist = t * Math.PI * 1.6 + phase + age * 0.08;
            Vec3 w = n1.scale(Math.cos(twist)).add(n2.scale(Math.sin(twist)));
            // Pointed tip at the leading end.
            float taper = s == shown ? 0.05f : (s >= shown - 2 ? 0.55f : 1.0f);
            centre[s] = c;
            across[s] = w.scale(WIDTH * 0.5 * taper);
        }

        for (int s = 0; s < shown; s++) {
            Vec3 a = centre[s];
            Vec3 b = centre[s + 1];
            Vec3 wa = across[s];
            Vec3 wb = across[s + 1];
            int silk = s % 3 == 1 ? SILK_FOLD : SILK;
            // Silk body (between the hems)
            strip(pose, buffers, a, b, wa.scale(-0.78), wa.scale(0.78), wb.scale(-0.78), wb.scale(0.78), silk);
            // Back face slightly darker so the twist reads
            strip(pose, buffers, a.add(normalOf(a, b, wa).scale(-0.004)), b.add(normalOf(a, b, wa).scale(-0.004)),
                    wa.scale(-0.78), wa.scale(0.78), wb.scale(-0.78), wb.scale(0.78), SILK_UNDER);
            // Black hems
            strip(pose, buffers, a, b, wa.scale(0.78), wa, wb.scale(0.78), wb, HEM);
            strip(pose, buffers, a, b, wa.scale(-1.0), wa.scale(-0.78), wb.scale(-1.0), wb.scale(-0.78), HEM);
            // Gold flower diamonds down the middle
            if (s % 4 == 2 && s < shown - 2) {
                Vec3 mid = a.add(b).scale(0.5);
                Vec3 along = b.subtract(a).scale(0.45);
                Vec3 off = normalOf(a, b, wa).scale(0.006);
                Vec3 sideHalf = wa.scale(0.3);
                ToonRenderHelper.quad3(pose, buffers, GOLD,
                        f(mid.add(along).add(off).x), f(mid.add(along).add(off).y), f(mid.add(along).add(off).z),
                        f(mid.add(sideHalf).add(off).x), f(mid.add(sideHalf).add(off).y), f(mid.add(sideHalf).add(off).z),
                        f(mid.subtract(along).add(off).x), f(mid.subtract(along).add(off).y), f(mid.subtract(along).add(off).z),
                        f(mid.subtract(sideHalf).add(off).x), f(mid.subtract(sideHalf).add(off).y), f(mid.subtract(sideHalf).add(off).z),
                        0, 1, 0);
            }
        }
    }

    /** Quad from segment start a to end b, spanning offsets [u0, u1] at a and [v0, v1] at b. */
    private static void strip(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 b, Vec3 u0, Vec3 u1, Vec3 v0, Vec3 v1, int color) {
        Vec3 p1 = a.add(u0), p2 = a.add(u1), p3 = b.add(v1), p4 = b.add(v0);
        ToonRenderHelper.quad3(pose, buffers, color,
                f(p1.x), f(p1.y), f(p1.z), f(p2.x), f(p2.y), f(p2.z), f(p3.x), f(p3.y), f(p3.z), f(p4.x), f(p4.y), f(p4.z),
                0, 1, 0);
    }

    private static Vec3 normalOf(Vec3 a, Vec3 b, Vec3 w) {
        Vec3 n = b.subtract(a).cross(w);
        return n.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : n.normalize();
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float u = 1.0f - t;
        return p0.scale(u * u).add(p1.scale(2 * u * t)).add(p2.scale(t * t));
    }

    private static float f(double d) {
        return (float) d;
    }

    @Override
    public ResourceLocation getTextureLocation(ObiSashEntity entity) {
        return TEXTURE;
    }
}
