package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BreathSlashEntity;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A breathing form's stroke ({@link BreathSlashEntity}): the cut travels along its path (a sideways crescent, a
 * rising arc, a full circle or a straight thrust) with a tapering trail, drawn as glowing camera-facing ribbons in
 * the element of the style:
 * <ul>
 *   <li>Water: a deep blue band with a second wave rolling along it, white foam at the core;</li>
 *   <li>Flame: a flickering orange band with a pale-gold core;</li>
 *   <li>Thunder: a jagged yellow zigzag with a white-hot core;</li>
 *   <li>Wind: three thin pale-green gusts side by side;</li>
 *   <li>Stone: a broad, heavy grey-brown sweep;</li>
 *   <li>Beast: two jagged parallel cuts (Inosuke's twin blades);</li>
 *   <li>Flower: a pink band shedding petals.</li>
 * </ul>
 */
public class BreathSlashRenderer extends EntityRenderer<BreathSlashEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int SEGMENTS = 28;
    private static final float SWEEP = 4.0F, TRAIL = 0.75F;

    public BreathSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BreathSlashEntity e, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partialTick;
        float progress = age / SWEEP;
        float head = Math.min(1.0F, progress), tail = Math.max(0.0F, progress - TRAIL);
        if (tail >= 1.0F || head <= 0.0F) return;
        float fade = Mth.clamp(1.0F - (age - 6.0F) / 8.0F, 0.0F, 1.0F);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(e.getPosition(partialTick));
        double yawR = Math.toRadians(e.strikeYaw());
        Vec3 fwd = new Vec3(-Math.sin(yawR), 0, Math.cos(yawR));
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        int shape = e.shape();
        BreathingStyles style = e.style();
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);

        switch (style) {
            case WATER -> {
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 0, 0.0F, false, 0.55F, 0x803A86FF, 0xE0E8F6FF, fade, 0);
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 1, 0.2F, false, 0.3F, 0x6050A8FF, 0xA0D8F0FF, fade, 0);
            }
            case FLAME -> {
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 2, 0.0F, false, 0.65F, 0x90FF5A10, 0xF0FFE6A0, fade, 0);
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 2, 0.12F, false, 0.35F, 0x80FF9A20, 0xC0FFF0C0, fade, 0);
            }
            case THUNDER -> {
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 3, 0.0F, false, 0.45F, 0x90FFD32A, 0xF0FFFFE8, fade, 0);
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 3, -0.25F, false, 0.2F, 0x70FFE060, 0xC0FFFFF0, fade * 0.8F, 0);
            }
            case WIND -> {
                for (int k = -1; k <= 1; k++) {
                    band(pose, vc, cam, shape, fwd, side, head, tail, age, 0, k * 0.3F, false, 0.28F, 0x6080E0B0, 0xB0E8FFF0, fade, k * 0.06F);
                }
            }
            case STONE -> band(pose, vc, cam, shape, fwd, side, head, tail, age, 0, 0.0F, false, 0.95F, 0x80786A5A, 0xC0D8CCB8, fade, 0);
            case BEAST -> {
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 4, 0.22F, true, 0.32F, 0x80707C90, 0xD0E0E4EC, fade, 0);
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 4, -0.22F, true, 0.32F, 0x80707C90, 0xD0E0E4EC, fade, 0.04F);
            }
            case FLOWER -> {
                band(pose, vc, cam, shape, fwd, side, head, tail, age, 0, 0.0F, false, 0.5F, 0x80FF80B4, 0xE0FFE4F2, fade, 0);
                petals(pose, vc, cam, shape, fwd, side, head, tail, age, fade, e.getId());
            }
            default -> band(pose, vc, cam, shape, fwd, side, head, tail, age, 0, 0.0F, false, 0.45F, 0x60FFFFFF, 0xE0FFFFFF, fade, 0);
        }
        super.render(e, entityYaw, partialTick, pose, buffers, light);
    }

    /** A point along the stroke (u from 0 to 1), relative to the owner's feet; {@code normal} receives the outward side. */
    private static Vec3 path(int shape, double u, Vec3 fwd, Vec3 side, Vec3[] normal) {
        Vec3 chest = new Vec3(0, 1.15, 0);
        switch (shape) {
            case BreathSlashEntity.THRUST -> {
                normal[0] = new Vec3(0, 1, 0);
                return chest.add(fwd.scale(0.4 + 4.6 * u));
            }
            case BreathSlashEntity.RISING -> {
                double th = Math.toRadians(-55 + 160 * u);
                Vec3 dir = fwd.scale(Math.cos(th)).add(0, Math.sin(th), 0);
                normal[0] = dir;
                return chest.add(dir.scale(1.9));
            }
            case BreathSlashEntity.SPIN -> {
                double th = Math.PI * 2 * u;
                Vec3 dir = fwd.scale(Math.cos(th)).add(side.scale(Math.sin(th)));
                normal[0] = dir;
                return chest.add(dir.scale(2.2)).add(0, 0.18 * Math.sin(th * 2), 0);
            }
            default -> {
                double th = Math.toRadians(-78 + 156 * u);
                Vec3 dir = fwd.scale(Math.cos(th)).add(side.scale(Math.sin(th)));
                normal[0] = dir;
                return chest.add(dir.scale(2.0)).add(0, 0.35 - 0.7 * u, 0);   // a diagonal cut
            }
        }
    }

    /**
     * One ribbon along the visible part of the stroke. {@code wiggle}: 0 none, 1 rolling wave, 2 flicker, 3 zigzag,
     * 4 small jags; {@code offset} pushes it outward (along the stroke's normal; across it when {@code stacked}), {@code lift} up.
     */
    private static void band(PoseStack pose, VertexConsumer vc, Vec3 cam, int shape, Vec3 fwd, Vec3 side, float head, float tail,
                             float age, int wiggle, float offset, boolean stacked, float width, int glow, int core, float fade, float lift) {
        if (fade <= 0.0F || head - tail < 1.0E-3F) return;
        Vec3[] n = new Vec3[1];
        Vec3 prev = null;
        float prevW = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float k = i / (float) SEGMENTS;
            double u = tail + (head - tail) * k;
            Vec3 p = path(shape, u, fwd, side, n);
            double off = offset;
            switch (wiggle) {
                case 1 -> off += 0.16 * Math.sin(u * 18 + age * 0.9);
                case 2 -> off += 0.07 * Math.sin(u * 47 + age * 3.1) + 0.04 * Math.sin(u * 91 - age * 5);
                case 3 -> off += ((int) (u * 16) % 2 == 0 ? 0.2 : -0.2) * Math.sin(Math.PI * k);
                case 4 -> off += ((int) (u * 30) % 2 == 0 ? 0.06 : -0.06);
                default -> {
                }
            }
            Vec3 normal = n[0];
            Vec3 up = shape == BreathSlashEntity.THRUST || shape == BreathSlashEntity.RISING ? side : new Vec3(0, 1, 0);
            if (stacked) {
                p = p.add(up.scale(off));   // twin cuts sit one above the other
            } else {
                p = p.add(normal.scale(off));
            }
            p = p.add(0, lift, 0);
            float w = width * (float) Math.sin(Math.PI * Math.min(1.0, Math.max(0.0, k))) * (0.35F + 0.65F * k);
            if (prev != null) {
                float ww = (w + prevW) * 0.5F;
                ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, ww, ThreadRenderHelper.scaleAlpha(glow, fade));
                ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, ww * 0.3F, ThreadRenderHelper.scaleAlpha(core, fade));
            }
            prev = p;
            prevW = w;
        }
    }

    /** Flower Breathing sheds petals along the cut that drift down as it fades. */
    private static void petals(PoseStack pose, VertexConsumer vc, Vec3 cam, int shape, Vec3 fwd, Vec3 side, float head, float tail,
                               float age, float fade, int seed) {
        Vec3[] n = new Vec3[1];
        for (int i = 0; i < 12; i++) {
            float h = Mth.frac(Mth.sin(seed * 12.9898F + i * 78.233F) * 43758.547F);
            double u = Math.min(head, 0.05 + 0.9 * h);
            if (u > head) continue;
            Vec3 p = path(shape, u, fwd, side, n).add(n[0].scale(0.25 * Math.sin(i * 2.1))).add(0, -0.05 * age, 0);
            Vec3 d = new Vec3(Math.cos(i + age * 0.4) * 0.12, 0.05, Math.sin(i + age * 0.4) * 0.12);
            ThreadRenderHelper.ribbon(pose, vc, cam, p.subtract(d), p.add(d), 0.12F, ThreadRenderHelper.scaleAlpha(0xE0FFB0D0, fade));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BreathSlashEntity entity) {
        return TEXTURE;
    }
}
