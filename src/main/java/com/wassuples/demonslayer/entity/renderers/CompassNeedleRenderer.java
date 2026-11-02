package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CompassNeedleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Compass Needle (Rashin): the red-to-light-blue snowflake compass with the numerals 一 to 十二, drawn as crisp
 * procedural lines (it used to be an upscaled low-resolution texture on a GeckoLib plane). It unfolds outward from
 * Akaza's feet, turns slowly while it reads the fighting spirit around him, its nodes pulse, and it fades out at
 * the end of the entity's 2-second life.
 */
public class CompassNeedleRenderer extends EntityRenderer<CompassNeedleEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final float RADIUS = 5.5f;
    private static final int LIFE = 40;

    public CompassNeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(CompassNeedleEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float unfold = Mth.clamp(age / 8.0f, 0.0f, 1.0f);
        unfold = 1.0f - (1.0f - unfold) * (1.0f - unfold);
        float fade = Mth.clamp((LIFE - age) / 8.0f, 0.0f, 1.0f);
        float spin = -entity.getYRot() * Mth.DEG_TO_RAD + age * 0.012f;
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.5f);
        AkazaRenderHelper.compass(pose, buffers, Vec3.ZERO, RADIUS, unfold, fade, spin, pulse * 0.6f);
        // Faint second ring of light racing out as it unfolds.
        if (age < 10.0f) {
            float t = age / 10.0f;
            HantenguRenderHelper.soundRing(pose, buffers, this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick)),
                    new Vec3(0, 0.05, 0), new Vec3(0, 1, 0), RADIUS * (0.2f + 0.9f * t), 0.12f, 0x80FF6A70, 1.0f - t);
        }
    }

    @Override
    public boolean shouldRender(CompassNeedleEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(RADIUS + 1.0, 1.0, RADIUS + 1.0));
    }

    @Override
    public ResourceLocation getTextureLocation(CompassNeedleEntity entity) {
        return TEXTURE;
    }
}
