package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonSlotMachineEntity;
import com.wassuples.demonslayer.util.ToonPocketOutcome;
import com.wassuples.demonslayer.util.ToonSfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The Pocket slot machine (formerly ToonPocketOverlay) drawn in the world: cabinet, blinking marquee lights,
 * three drum reels that spin and stop one after another on the rolled result, a pull lever and a cartoon
 * caption. Billboarded, so the caster and every onlooker see it face-on.
 */
public class ToonSlotMachineRenderer extends EntityRenderer<ToonSlotMachineEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float PX = 0.0085f;

    // Layer depths (positive = further from the camera). Items are centred on 0 and need room in front/behind.
    private static final float Z_FRAME = 14.0f;
    private static final float Z_PANEL = 13.5f;
    private static final float Z_WINDOW = 13.0f;
    private static final float Z_PAYLINE = -12.0f;

    public ToonSlotMachineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonSlotMachineEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float t = entity.tickCount + partialTick;
        int resolve = entity.getResolveTick();
        int result = Mth.clamp(entity.getResult(), 0, ToonPocketOutcome.count() - 1);
        float remaining = entity.getDuration() - t;
        float appear = t < 5.0f ? easeOutBack(t / 5.0f) : 1.0f;
        float vanish = remaining < 4.0f ? Mth.clamp(remaining / 4.0f, 0.0f, 1.0f) : 1.0f;
        boolean resolved = t >= resolve;

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float s = PX * appear * vanish;
        pose.scale(-s, -s, s);
        pose.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(t * 0.7f) * (resolved ? 1.0f : 3.0f)));

        drawCabinet(pose, buffers, t, resolved);
        for (int reel = 0; reel < 3; reel++) {
            drawReel(pose, buffers, reel, result, t, resolve, entity.getId());
        }
        // payline across the middle, in front of the items
        ToonRenderHelper.quad(pose, buffers, 0x99DC3131, -64.0f, -1.2f, 64.0f, 1.2f, Z_PAYLINE);
        drawLever(pose, buffers, t);

        // Caption above the cabinet, using the same lettering as every other Toon Force SFX.
        pose.pushPose();
        pose.translate(0.0, -46.0, 0.0);
        pose.scale(1.45f, 1.45f, 1.0f);
        String caption = resolved ? ToonPocketOutcome.byIndex(result).caption() : "POCKET ROULETTE!";
        float captionAge = resolved ? t - resolve : t;
        ToonSfxTextRenderer.drawCartoonText(this.getFont(), pose, buffers, caption,
                resolved ? 0xFFFFD84A : 0xFFFFFFFF, 0xFF1A0B33, 0xFFFF2E88, 0xFF1A0B33,
                resolved ? ToonSfx.STYLE_STARBURST : ToonSfx.STYLE_SPEED_LINES,
                entity.getId() + (resolved ? 17 : 0), captionAge, vanish, resolved);
        pose.popPose();

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void drawCabinet(PoseStack pose, MultiBufferSource buffers, float t, boolean resolved) {
        boolean flash = resolved && ((int) (t * 0.5f)) % 2 == 0;
        ToonRenderHelper.quad(pose, buffers, 0xF017121F, -74.0f, -32.0f, 74.0f, 32.0f, Z_FRAME);
        ToonRenderHelper.quad(pose, buffers, flash ? 0xFFFFF2A0 : 0xFFFFC928, -69.0f, -27.0f, 69.0f, 27.0f, Z_PANEL);
        // marquee bulbs, chasing
        for (int i = 0; i < 12; i++) {
            float x = -66.0f + i * 12.0f;
            boolean on = (((int) (t * 0.8f)) + i) % 3 == 0 || resolved;
            int bulb = on ? 0xFFFFFFFF : 0xFFB5462E;
            ToonRenderHelper.disc(pose, buffers, 0xFF17121F, x, -29.5f, 2.6f, 10, Z_PANEL - 0.2f);
            ToonRenderHelper.disc(pose, buffers, bulb, x, -29.5f, 1.8f, 10, Z_PANEL - 0.4f);
            ToonRenderHelper.disc(pose, buffers, 0xFF17121F, x, 29.5f, 2.6f, 10, Z_PANEL - 0.2f);
            ToonRenderHelper.disc(pose, buffers, bulb, x, 29.5f, 1.8f, 10, Z_PANEL - 0.4f);
        }
        for (int reel = 0; reel < 3; reel++) {
            float x0 = -61.0f + reel * 42.0f;
            ToonRenderHelper.quad(pose, buffers, 0xFF3A2A12, x0 - 1.5f, -21.5f, x0 + 37.5f, 21.5f, Z_WINDOW + 0.2f);
            ToonRenderHelper.quad(pose, buffers, 0xFFF8E8C1, x0, -20.0f, x0 + 36.0f, 20.0f, Z_WINDOW);
            // drum shading top and bottom
            ToonRenderHelper.quad(pose, buffers, 0x55000000, x0, -20.0f, x0 + 36.0f, -13.0f, Z_WINDOW - 0.2f);
            ToonRenderHelper.quad(pose, buffers, 0x55000000, x0, 13.0f, x0 + 36.0f, 20.0f, Z_WINDOW - 0.2f);
        }
    }

    /**
     * A reel is a drum of the four items. It spins with a cubic ease-out and lands exactly on the result,
     * reels stopping left to right, with a little overshoot wobble on stop.
     */
    private static void drawReel(PoseStack pose, MultiBufferSource buffers, int reel, int result, float t, int resolve, int seed) {
        float stopTick = Math.max(6.0f, resolve - 12.0f + reel * 6.0f);
        float travel = 2.6f * stopTick / 3.0f; // gives a ~2.6 items/tick initial speed
        int finalIndex = Mth.ceil(travel);
        while (Math.floorMod(finalIndex + reel * 5 + seed, ToonPocketOutcome.count()) != result) finalIndex++;

        float position;
        if (t < stopTick) {
            float u = t / stopTick;
            float eased = 1.0f - (1.0f - u) * (1.0f - u) * (1.0f - u);
            position = finalIndex * eased;
        } else {
            float since = t - stopTick;
            position = finalIndex + 0.22f * Mth.sin(since * 1.6f) * (float) Math.exp(-since * 0.45f);
        }

        float centreX = -61.0f + reel * 42.0f + 18.0f;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        int base = Mth.floor(position);
        for (int k = base - 1; k <= base + 2; k++) {
            float theta = (k - position) * ((float) Math.PI / 4.0f);
            float facing = Mth.cos(theta);
            if (facing < 0.25f) continue;
            float y = Mth.sin(theta) * 19.0f;
            ItemStack stack = ToonPocketOutcome.byIndex(k + reel * 5 + seed).icon();

            pose.pushPose();
            pose.translate(centreX, y, 0.0);
            // Our space is X right / Y down / Z away; items expect X right / Y up / Z toward the viewer.
            pose.scale(20.0f, -20.0f * facing, -20.0f);
            itemRenderer.renderStatic(stack, ItemTransforms.TransformType.GUI, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    pose, buffers, seed + k);
            pose.popPose();
        }
    }

    private static void drawLever(PoseStack pose, MultiBufferSource buffers, float t) {
        // Pulled down over the first few ticks, then springs back.
        float pull = t < 4.0f ? t / 4.0f : Math.max(0.0f, 1.0f - (t - 4.0f) / 5.0f);
        float knobY = Mth.lerp(pull, -40.0f, 6.0f);
        ToonRenderHelper.quad(pose, buffers, 0xFF17121F, 74.0f, -4.0f, 82.0f, 8.0f, Z_FRAME);      // hinge box
        float top = Math.min(knobY, 0.0f);
        float bottom = Math.max(knobY, 0.0f);
        ToonRenderHelper.quad(pose, buffers, 0xFF9AA3AE, 76.5f, top, 79.5f, bottom + 2.0f, Z_PANEL); // shaft
        ToonRenderHelper.disc(pose, buffers, 0xFF17121F, 78.0f, knobY, 6.0f, 14, Z_PANEL - 0.2f);
        ToonRenderHelper.disc(pose, buffers, 0xFFE0242F, 78.0f, knobY, 4.8f, 14, Z_PANEL - 0.4f);
        ToonRenderHelper.disc(pose, buffers, 0xFFFF9A9A, 76.6f, knobY - 1.6f, 1.4f, 8, Z_PANEL - 0.6f);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = Mth.clamp(t, 0.0f, 1.0f) - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    @Override
    public ResourceLocation getTextureLocation(ToonSlotMachineEntity entity) {
        return TEXTURE;
    }
}
