package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WeepingSpearProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Aizetsu's (Sorrow's) jumonji spear, thrown in a volley for Weeping Spears: a long black-lacquered shaft with red
 * bindings, a brass collar and a cross-shaped steel head whose side blades curve forward. In flight it rolls and
 * trails a cold blue streak that sheds falling teardrops. (Replaces the GeckoLib model and the soul-fire
 * particles.)
 */
public class WeepingSpearRenderer extends EntityRenderer<WeepingSpearProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int SHAFT = 0xFF17131A;
    private static final int BINDING = 0xFF8E1B22;
    private static final int BRASS = 0xFFC9A04A;
    private static final int STEEL = 0xFFC9D1DB;
    private static final int STEEL_EDGE = 0xFFF4F7FF;

    public WeepingSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(WeepingSpearProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        boolean flying = !entity.isStuck();
        float age = entity.tickCount + partialTick;

        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(yRot));
        pose.mulPose(Vector3f.XP.rotationDegrees(-xRot));
        if (flying) pose.mulPose(Vector3f.ZP.rotationDegrees(age * 25.0f));
        pose.translate(0, 0, -0.55f); // balance point
        // Shaft with red bindings
        float r = 0.035f;
        ToonRenderHelper.solidBox(pose, buffers, SHAFT, light, -r, -r, -1.9f, r, r, 0.0f, 0.0f);
        for (int b = 0; b < 3; b++) {
            float z = -0.25f - b * 0.12f;
            ToonRenderHelper.solidBox(pose, buffers, BINDING, light, -r - 0.008f, -r - 0.008f, z - 0.05f, r + 0.008f, r + 0.008f, z, 0.0f);
        }
        ToonRenderHelper.solidBox(pose, buffers, BRASS, light, -r - 0.01f, -r - 0.01f, -1.95f, r + 0.01f, r + 0.01f, -1.88f, 0.0f);
        // Collar
        ToonRenderHelper.solidBox(pose, buffers, BRASS, light, -0.055f, -0.055f, 0.0f, 0.055f, 0.055f, 0.1f, 0.0f);
        // Central blade: long flat diamond
        blade(pose, buffers, light, 0.0f, 0.1f, 0.0f, 0.85f, 0.06f, 0.018f);
        // Cross blades: out to each side, then curving forward to a point
        for (int side = -1; side <= 1; side += 2) {
            float[][] arm = {
                    {0.0f, 0.18f}, {side * 0.18f, 0.2f}, {side * 0.3f, 0.27f}, {side * 0.34f, 0.42f}};
            for (int k = 0; k < arm.length - 1; k++) {
                float w0 = 0.045f * (1.0f - k * 0.3f);
                float w1 = 0.045f * (1.0f - (k + 1) * 0.3f);
                segment(pose, buffers, light, arm[k][0], arm[k][1], arm[k + 1][0], arm[k + 1][1], w0, Math.max(0.006f, w1), 0.016f,
                        k == arm.length - 2 ? STEEL_EDGE : STEEL);
            }
        }
        pose.popPose();

        // Cold blue streak and falling tears behind it while it flies.
        if (flying) {
            float yr = yRot * Mth.DEG_TO_RAD;
            float xr = xRot * Mth.DEG_TO_RAD;
            Vec3 dir = new Vec3(Mth.sin(yr) * Mth.cos(xr), Mth.sin(xr), Mth.cos(yr) * Mth.cos(xr));
            Vec3 origin = entity.getPosition(partialTick);
            Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
            VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
            Vec3 tail = dir.scale(-1.2);
            ThreadRenderHelper.ribbon(pose, vc, cam, tail, tail.subtract(dir.scale(3.0)), 0.35f, 0x305A8CFF);
            ThreadRenderHelper.ribbon(pose, vc, cam, tail, tail.subtract(dir.scale(2.0)), 0.12f, 0x80C8E4FF);
            for (int t = 0; t < 4; t++) {
                float k = ((age * 0.25f) + t * 0.25f) % 1.0f;
                Vec3 drop = tail.subtract(dir.scale(0.6 + k * 2.4)).add(0, -k * k * 0.9, 0);
                ThreadRenderHelper.ribbon(pose, vc, cam, drop, drop.add(0, -0.12, 0), 0.07f, ThreadRenderHelper.scaleAlpha(0xC0B8DCFF, 1.0f - k));
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Flat double-edged diamond blade along +Z from z0 to z1 (widest at 30%), centred on (x, y). */
    private static void blade(PoseStack pose, MultiBufferSource buffers, int light, float x, float z0, float y, float z1, float halfWidth, float halfThick) {
        float zw = z0 + (z1 - z0) * 0.3f;
        // Back taper (collar to widest) and front taper (widest to tip) as two hexas.
        ToonRenderHelper.solidHexa(pose, buffers, STEEL, light, hexa(x, y, z0, halfWidth * 0.5f, halfThick, zw, halfWidth, halfThick), false);
        ToonRenderHelper.solidHexa(pose, buffers, STEEL_EDGE, light, hexa(x, y, zw, halfWidth, halfThick, z1, 0.004f, 0.004f), false);
    }

    /** Blade segment in the XZ plane from (x0,z0) to (x1,z1), width w0 -> w1, thickness 2*t. */
    private static void segment(PoseStack pose, MultiBufferSource buffers, int light, float x0, float z0, float x1, float z1,
                                float w0, float w1, float t, int color) {
        float dx = x1 - x0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-5f) return;
        float px = dz / len, pz = -dx / len; // perpendicular in XZ, right-handed with (perp, up, along) so faces point out
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            boolean end = (i & 4) != 0;
            float w = end ? w1 : w0;
            float cx = end ? x1 : x0, cz = end ? z1 : z0;
            float side = (i & 1) != 0 ? 1 : -1;
            float y = (i & 2) != 0 ? t : -t;
            c[i] = new float[]{cx + px * w * side, y, cz + pz * w * side};
        }
        ToonRenderHelper.solidHexa(pose, buffers, color, light, c, false);
    }

    /** Corners of a prism along Z: rectangle (±w0, ±t0) at z0 to (±w1, ±t1) at z1, centred on (x, y). */
    private static float[][] hexa(float x, float y, float z0, float w0, float t0, float z1, float w1, float t1) {
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            boolean end = (i & 4) != 0;
            float w = end ? w1 : w0, t = end ? t1 : t0;
            c[i] = new float[]{x + ((i & 1) != 0 ? w : -w), y + ((i & 2) != 0 ? t : -t), end ? z1 : z0};
        }
        return c;
    }

    @Override
    public ResourceLocation getTextureLocation(WeepingSpearProjectileEntity entity) {
        return TEXTURE;
    }
}
