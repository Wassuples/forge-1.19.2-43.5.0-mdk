package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonSfxTextEntity;
import com.wassuples.demonslayer.util.ToonSfx;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Comic-book lettering: a burst/cloud/zigzag/speed-line backdrop, chunky outlined letters with a
 * fake 3D extrusion, per-letter tilt and size, a staggered pop-in, wobble while alive and a
 * squash-out at the end. All randomness is seeded from the entity, so every viewer sees the same.
 */
public class ToonSfxTextRenderer extends EntityRenderer<ToonSfxTextEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float PIXEL = 0.028f;

    public ToonSfxTextRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonSfxTextEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        String text = entity.getText();
        if (text.isEmpty()) return;

        float age = entity.tickCount + partialTick;
        float remaining = entity.getDuration() - age;
        if (remaining <= 0.0f) return;

        Random rnd = new Random(entity.getSeed());
        float popIn = age < 5.0f ? easeOutBack(Mth.clamp(age / 5.0f, 0.0f, 1.0f)) : 1.0f;
        float popOut = remaining < 5.0f ? Mth.clamp(remaining / 5.0f, 0.0f, 1.0f) : 1.0f;
        float alpha = Mth.clamp(remaining / 4.0f, 0.0f, 1.0f);
        float tilt = (rnd.nextFloat() - 0.5f) * 28.0f;

        pose.pushPose();
        pose.translate(0.0, 0.2, 0.0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float s = PIXEL * entity.getTextScale() * popIn;
        // Squash on the way out: wide and flat, like a cartoon "poof".
        float squashX = 1.0f + (1.0f - popOut) * 0.6f;
        float squashY = popOut;
        pose.scale(-s * squashX, -s * squashY, s);
        pose.mulPose(Vector3f.ZP.rotationDegrees(tilt + (float) Math.sin(age * 0.35f) * 2.5f));

        drawCartoonText(this.getFont(), pose, buffers, text, entity.getFill(), entity.getOutline(),
                entity.getBurst(), entity.getBurstOutline(), entity.getStyle(), entity.getSeed(), age, alpha, true);
        pose.popPose();
    }

    /**
     * Draws the full SFX treatment centred on the current pose origin, in "font pixel" units with Y down
     * (call after a billboard + {@code scale(-px, -px, px)}). Reused by the Pocket slot machine caption.
     */
    static void drawCartoonText(Font font, PoseStack pose, MultiBufferSource buffers, String text,
                                int fill, int outline, int burst, int burstOutline, int style, int seed,
                                float age, float alpha, boolean drawBackdrop) {
        Random rnd = new Random(seed * 31L + 7L);
        int count = text.length();
        float[] widths = new float[count];
        float[] scales = new float[count];
        float[] angles = new float[count];
        Component[] glyphs = new Component[count];
        float total = 0.0f;
        for (int i = 0; i < count; i++) {
            glyphs[i] = Component.literal(String.valueOf(text.charAt(i))).withStyle(ChatFormatting.BOLD);
            scales[i] = (i == 0 ? 1.25f : 0.95f) + rnd.nextFloat() * 0.3f;
            angles[i] = (rnd.nextFloat() - 0.5f) * 22.0f;
            widths[i] = font.width(glyphs[i]) * scales[i];
            total += widths[i] - 0.6f;
        }
        boolean alternate = rnd.nextFloat() < 0.4f;
        int depth = style == ToonSfx.STYLE_BLOCK_3D ? 5 : 3;

        if (drawBackdrop) {
            drawBackdrop(pose, buffers, style, rnd, total, age, ToonRenderHelper.withAlpha(burst, alpha),
                    ToonRenderHelper.withAlpha(burstOutline, alpha));
        }

        int extrudeColor = ToonRenderHelper.shade(fill, 0.5f);
        float cursor = -total / 2.0f;
        for (int i = 0; i < count; i++) {
            float letterAge = age - i * 0.6f;
            if (letterAge <= 0.0f) {
                cursor += widths[i] - 0.6f;
                continue;
            }
            float letterPop = letterAge < 4.0f ? easeOutBack(letterAge / 4.0f) : 1.0f;
            float wave = (float) Math.sin(age * 0.55f + i * 0.8f) * 1.3f;
            float wobble = (float) Math.sin(age * 0.4f + i * 1.3f) * 3.0f;
            float centre = cursor + widths[i] / 2.0f;

            pose.pushPose();
            pose.translate(centre, wave, 0.0);
            pose.mulPose(Vector3f.ZP.rotationDegrees(angles[i] + wobble));
            float ls = scales[i] * letterPop;
            pose.scale(ls, ls, 1.0f);

            float glyphX = -font.width(glyphs[i]) / 2.0f;
            float glyphY = -4.0f;
            int letterFill = alternate && (i % 2 == 1) ? ToonRenderHelper.shade(fill, 0.82f) : fill;

            // Extrusion (down-right), then a thick 8-way outline, then the fill on top.
            for (int k = depth; k >= 1; k--) {
                drawGlyph(font, pose, buffers, glyphs[i], glyphX + k * 0.55f, glyphY + k * 0.55f, 0.5f + k * 0.05f,
                        k == depth ? outline : extrudeColor, alpha);
            }
            for (int d = 0; d < 8; d++) {
                double a = Math.PI * 2.0 * d / 8.0;
                drawGlyph(font, pose, buffers, glyphs[i], glyphX + (float) Math.cos(a) * 1.15f,
                        glyphY + (float) Math.sin(a) * 1.15f, 0.3f, outline, alpha);
            }
            drawGlyph(font, pose, buffers, glyphs[i], glyphX, glyphY, 0.0f, letterFill, alpha);
            pose.popPose();

            cursor += widths[i] - 0.6f;
        }
    }

    private static void drawGlyph(Font font, PoseStack pose, MultiBufferSource buffers, Component glyph,
                                  float x, float y, float zBehind, int color, float alpha) {
        int a = (int) ((color >>> 24 & 255) * alpha);
        if (a < 8) return; // Font treats near-zero alpha as opaque; just skip.
        pose.pushPose();
        pose.translate(0.0, 0.0, zBehind);
        font.drawInBatch(glyph, x, y, (a << 24) | (color & 0xFFFFFF), false, pose.last().pose(), buffers, false, 0, FULL_BRIGHT);
        pose.popPose();
    }

    private static void drawBackdrop(PoseStack pose, MultiBufferSource buffers, int style, Random rnd,
                                     float textWidth, float age, int burst, int burstOutline) {
        float rx = textWidth / 2.0f + 11.0f;
        float ry = 14.0f + rnd.nextFloat() * 3.0f;
        switch (style) {
            case ToonSfx.STYLE_STARBURST -> {
                int spikes = 14 + rnd.nextInt(6);
                float[] xs = new float[spikes * 2];
                float[] ys = new float[spikes * 2];
                float[] ox = new float[spikes * 2];
                float[] oy = new float[spikes * 2];
                float pulse = 1.0f + (float) Math.sin(age * 0.8f) * 0.04f;
                for (int i = 0; i < spikes * 2; i++) {
                    double a = Math.PI * 2.0 * i / (spikes * 2) + age * 0.01f;
                    float r = (i % 2 == 0 ? 1.0f + rnd.nextFloat() * 0.35f : 0.68f) * pulse;
                    xs[i] = (float) Math.cos(a) * rx * r;
                    ys[i] = (float) Math.sin(a) * ry * r;
                    ox[i] = (float) Math.cos(a) * (rx * r + 2.6f);
                    oy[i] = (float) Math.sin(a) * (ry * r + 2.6f);
                }
                ToonRenderHelper.polygon(pose, buffers, burstOutline, ox, oy, 3.6f);
                ToonRenderHelper.polygon(pose, buffers, burst, xs, ys, 3.0f);
            }
            case ToonSfx.STYLE_CLOUD -> {
                int puffs = 8 + rnd.nextInt(3);
                float[] px = new float[puffs];
                float[] py = new float[puffs];
                float[] pr = new float[puffs];
                for (int i = 0; i < puffs; i++) {
                    double a = Math.PI * 2.0 * i / puffs;
                    px[i] = (float) Math.cos(a) * rx * 0.82f;
                    py[i] = (float) Math.sin(a) * ry * 0.72f;
                    pr[i] = 6.5f + rnd.nextFloat() * 3.5f + (float) Math.sin(age * 0.5f + i) * 0.5f;
                }
                for (int i = 0; i < puffs; i++) {
                    ToonRenderHelper.disc(pose, buffers, burstOutline, px[i], py[i], pr[i] + 2.2f, 14, 3.6f);
                }
                ToonRenderHelper.disc(pose, buffers, burstOutline, 0, 0, Math.max(rx, ry) * 0.2f + 2.2f, 14, 3.6f);
                for (int i = 0; i < puffs; i++) {
                    ToonRenderHelper.disc(pose, buffers, burst, px[i], py[i], pr[i], 14, 3.0f);
                }
                ToonRenderHelper.ellipse(pose, buffers, burst, rx * 0.86f, ry * 0.78f, 3.0f);
                // little thought bubbles trailing off
                ToonRenderHelper.disc(pose, buffers, burstOutline, rx * 0.9f, ry + 4.0f, 3.6f, 10, 3.6f);
                ToonRenderHelper.disc(pose, buffers, burst, rx * 0.9f, ry + 4.0f, 2.4f, 10, 3.0f);
            }
            case ToonSfx.STYLE_ZIGZAG -> {
                int perSide = 7;
                float[] xs = new float[perSide * 4];
                float[] ys = new float[perSide * 4];
                float[] ox = new float[perSide * 4];
                float[] oy = new float[perSide * 4];
                int n = 0;
                for (int side = 0; side < 4; side++) {
                    for (int i = 0; i < perSide; i++, n++) {
                        float t = (float) i / perSide;
                        float x;
                        float y;
                        switch (side) {
                            case 0 -> { x = -rx + 2 * rx * t; y = -ry; }
                            case 1 -> { x = rx; y = -ry + 2 * ry * t; }
                            case 2 -> { x = rx - 2 * rx * t; y = ry; }
                            default -> { x = -rx; y = ry - 2 * ry * t; }
                        }
                        float jag = (i % 2 == 0 ? 1.18f : 0.92f) + rnd.nextFloat() * 0.06f;
                        xs[n] = x * jag;
                        ys[n] = y * jag;
                        ox[n] = x * jag * 1.12f + Math.signum(x) * 1.5f;
                        oy[n] = y * jag * 1.15f + Math.signum(y) * 1.5f;
                    }
                }
                ToonRenderHelper.polygon(pose, buffers, burstOutline, ox, oy, 3.6f);
                ToonRenderHelper.polygon(pose, buffers, burst, xs, ys, 3.0f);
            }
            case ToonSfx.STYLE_SPEED_LINES -> {
                int lines = 18;
                for (int i = 0; i < lines; i++) {
                    double a = Math.PI * 2.0 * i / lines + rnd.nextFloat() * 0.15f;
                    float inner = 1.05f;
                    float outer = 1.55f + rnd.nextFloat() * 0.5f + (float) Math.sin(age * 0.9f + i) * 0.12f;
                    float c = (float) Math.cos(a);
                    float sn = (float) Math.sin(a);
                    float px = -sn * 1.6f;
                    float py = c * 1.6f;
                    ToonRenderHelper.tri(pose, buffers, burst,
                            c * rx * inner - px, sn * ry * inner - py,
                            c * rx * inner + px, sn * ry * inner + py,
                            c * rx * outer, sn * ry * outer, 3.0f);
                }
            }
            default -> {
                // STYLE_BLOCK_3D: no backdrop, only a soft drop shadow puddle so it pops off the scene.
                pose.pushPose();
                pose.translate(0.0, 9.0, 0.0);
                ToonRenderHelper.ellipse(pose, buffers, ToonRenderHelper.withAlpha(burstOutline, 0.35f),
                        rx * 0.8f, 4.0f, 3.6f);
                pose.popPose();
            }
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    @Override
    public ResourceLocation getTextureLocation(ToonSfxTextEntity entity) {
        return TEXTURE;
    }
}
