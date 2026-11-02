package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.KillerGoldfishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Thousand Needle Fish Kill turret: one of Gyokko's goldfish bobbing upright out of a small vase, turning to face
 * its target and puckering its lips to spit needles. (Was a plain white GeckoLib model.)
 */
public class KillerGoldfishRenderer extends EntityRenderer<KillerGoldfishEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public KillerGoldfishRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(KillerGoldfishEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float rise = Mth.clamp(age / 10.0f, 0.0f, 1.0f);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        pose.pushPose();
        // Small vase it sits in
        pose.pushPose();
        pose.scale(0.55f, 0.45f, 0.55f);
        GyokkoRenderHelper.vase(pose, buffers, light, 0.0f);
        pose.popPose();
        // Fish emerges from the vase mouth, bobbing
        pose.translate(0, 0.45f * GyokkoRenderHelper.vaseMouthHeight() + 0.25f * rise + Mth.sin(age * 0.2f) * 0.05f, 0);
        pose.mulPose(Vector3f.YP.rotationDegrees(-headYaw));
        pose.mulPose(Vector3f.XP.rotationDegrees(pitch * 0.6f - 25.0f)); // tilted up out of the vase
        float spit = Math.max(0.0f, Mth.sin(age * 0.5f)); // lips pucker rhythmically
        GyokkoRenderHelper.goldfish(pose, buffers, light, 0.75f * rise, age, spit);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(KillerGoldfishEntity entity) {
        return TEXTURE;
    }
}
