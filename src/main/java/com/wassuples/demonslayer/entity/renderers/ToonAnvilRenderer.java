package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A real 3D cartoon anvil: stepped foot, tapered waist, chunky face with a heel at the back and a long tapered horn,
 * a hardy hole on top, ink outlines (inverted hull), and an embossed ACME plaque on both sides. The letters are
 * raised geometry built from a pixel font, not text, so they catch light and read from any angle.
 * Squashes and stretches as it falls and lands.
 */
public class ToonAnvilRenderer extends EntityRenderer<ToonAnvilEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    private static final int IRON_DARK = 0xFF3E4852;
    private static final int IRON = 0xFF5F6E7C;
    private static final int IRON_LIGHT = 0xFF93A5B5;
    private static final int HOLE = 0xFF141218;
    private static final int PLAQUE = 0xFFFFD23A;
    private static final int PLAQUE_RIM = 0xFFC2281F;
    private static final int LETTER = 0xFFD8312A;
    private static final float INK = 0.022f;

    // 5-row pixel font, '#' = raised pixel. Columns can differ per letter.
    private static final String[][] GLYPHS = {
            {".#.", "#.#", "###", "#.#", "#.#"},          // A
            {"###", "#..", "#..", "#..", "###"},          // C
            {"#...#", "##.##", "#.#.#", "#...#", "#...#"}, // M
            {"###", "#..", "##.", "#..", "###"}           // E
    };

    public ToonAnvilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(ToonAnvilEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        pose.pushPose();
        // Each anvil drops at its own angle so the logo isn't always facing the same way.
        pose.mulPose(Vector3f.YP.rotationDegrees((entity.getId() * 67) % 360));

        // Stretch while falling, pancake on landing.
        float stretchY = entity.isOnGround() ? 0.72f : 1.0f + 0.12f * Mth.clamp(age / 6.0f, 0.0f, 1.0f);
        float widen = 1.0f / (float) Math.sqrt(stretchY);
        pose.scale(widen, stretchY, widen);
        if (!entity.isOnGround()) {
            pose.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(age * 0.9f) * 6.0f)); // whistling wobble
        }
        drawAnvil(pose, buffers, light);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Anvil in blocks, feet at y=0, horn pointing +X, about 1.2 x 0.8 x 0.64. */
    private static void drawAnvil(PoseStack pose, MultiBufferSource buffers, int light) {
        // Foot and step
        ToonRenderHelper.solidBox(pose, buffers, IRON_DARK, light, -0.45f, 0.0f, -0.32f, 0.45f, 0.14f, 0.32f, INK);
        ToonRenderHelper.solidBox(pose, buffers, IRON, light, -0.37f, 0.14f, -0.26f, 0.37f, 0.22f, 0.26f, INK);

        // Waist, tapering in toward the top
        float[][] waist = {
                {-0.27f, 0.22f, -0.2f}, {0.27f, 0.22f, -0.2f}, {-0.19f, 0.52f, -0.15f}, {0.19f, 0.52f, -0.15f},
                {-0.27f, 0.22f, 0.2f}, {0.27f, 0.22f, 0.2f}, {-0.19f, 0.52f, 0.15f}, {0.19f, 0.52f, 0.15f}};
        ToonRenderHelper.solidHexa(pose, buffers, IRON_DARK, light, waist, false);
        ToonRenderHelper.solidHexa(pose, buffers, 0xFF0D0C12, light, ToonRenderHelper.inflate(waist, INK), true);

        // Face (main body) and heel
        ToonRenderHelper.solidBox(pose, buffers, IRON, light, -0.42f, 0.52f, -0.22f, 0.36f, 0.8f, 0.22f, INK);
        ToonRenderHelper.solidBox(pose, buffers, IRON, light, -0.6f, 0.62f, -0.18f, -0.42f, 0.8f, 0.18f, INK);
        // polished working surface on top
        ToonRenderHelper.solidBox(pose, buffers, IRON_LIGHT, light, -0.6f, 0.8f, -0.2f, 0.36f, 0.83f, 0.2f, 0.0f);

        // Horn: thick at the face, tapering to a rounded point that rises slightly
        float[][] horn = {
                {0.36f, 0.56f, -0.2f}, {0.8f, 0.76f, -0.025f}, {0.36f, 0.83f, -0.2f}, {0.8f, 0.83f, -0.025f},
                {0.36f, 0.56f, 0.2f}, {0.8f, 0.76f, 0.025f}, {0.36f, 0.83f, 0.2f}, {0.8f, 0.83f, 0.025f}};
        ToonRenderHelper.solidHexa(pose, buffers, IRON, light, horn, false);
        ToonRenderHelper.solidHexa(pose, buffers, 0xFF0D0C12, light, ToonRenderHelper.inflate(horn, INK), true);

        // Hardy and pritchel holes in the top
        ToonRenderHelper.solidBox(pose, buffers, HOLE, light, -0.5f, 0.828f, -0.04f, -0.42f, 0.834f, 0.04f, 0.0f);
        ToonRenderHelper.solidBox(pose, buffers, HOLE, light, -0.36f, 0.828f, -0.03f, -0.3f, 0.834f, 0.03f, 0.0f);

        // ACME plaques on both long sides of the face
        drawPlaque(pose, buffers, light, 1);
        drawPlaque(pose, buffers, light, -1);
    }

    /** Raised plaque + raised pixel letters on the +Z (side = 1) or -Z (side = -1) face, reading left-to-right. */
    private static void drawPlaque(PoseStack pose, MultiBufferSource buffers, int light, int side) {
        float faceZ = 0.22f;
        float centreX = -0.03f;
        float centreY = 0.66f;
        float px = 0.03f;

        int columns = 0;
        for (String[] glyph : GLYPHS) columns += glyph[0].length();
        columns += GLYPHS.length - 1; // one-pixel gaps
        float width = columns * px;
        float height = 5 * px;

        // plaque: red rim, then yellow panel, each standing a little proud of the face
        plaqueBox(pose, buffers, light, side, centreX - width / 2 - 0.05f, centreY - height / 2 - 0.045f,
                centreX + width / 2 + 0.05f, centreY + height / 2 + 0.045f, faceZ, faceZ + 0.012f, PLAQUE_RIM);
        plaqueBox(pose, buffers, light, side, centreX - width / 2 - 0.035f, centreY - height / 2 - 0.03f,
                centreX + width / 2 + 0.035f, centreY + height / 2 + 0.03f, faceZ + 0.012f, faceZ + 0.02f, PLAQUE);

        // letters, merged into horizontal runs per row to keep the vertex count low
        int column = 0;
        for (String[] glyph : GLYPHS) {
            for (int row = 0; row < 5; row++) {
                String line = glyph[row];
                int start = -1;
                for (int c = 0; c <= line.length(); c++) {
                    boolean on = c < line.length() && line.charAt(c) == '#';
                    if (on && start < 0) start = c;
                    if (!on && start >= 0) {
                        float x0 = centreX - width / 2 + (column + start) * px;
                        float x1 = centreX - width / 2 + (column + c) * px;
                        float y1 = centreY + height / 2 - row * px;
                        float y0 = y1 - px;
                        plaqueBox(pose, buffers, light, side, x0, y0, x1, y1, faceZ + 0.02f, faceZ + 0.04f, LETTER);
                        start = -1;
                    }
                }
            }
            column += glyph[0].length() + 1;
        }
    }

    /**
     * Box on one side of the anvil. For the back side the X range is mirrored so the logo still reads correctly
     * when you walk around to it.
     */
    private static void plaqueBox(PoseStack pose, MultiBufferSource buffers, int light, int side,
                                  float x0, float y0, float x1, float y1, float z0, float z1, int color) {
        if (side > 0) {
            ToonRenderHelper.solidBox(pose, buffers, color, light, x0, y0, z0, x1, y1, z1, 0.0f);
        } else {
            float mirror = -0.06f; // mirror around the plaque centre (centreX * 2)
            ToonRenderHelper.solidBox(pose, buffers, color, light, mirror - x1, y0, -z1, mirror - x0, y1, -z0, 0.0f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonAnvilEntity entity) {
        return TEXTURE;
    }
}
