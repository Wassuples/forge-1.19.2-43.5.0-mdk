package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.PoisonNeedleProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Gyokko's poison needle: a thin dark needle with a sickly green-violet tip and a short venom streak. (The old
 * GeckoLib renderer pointed at textures/entity/poison_needle.png, which doesn't exist in the mod's assets, so it
 * rendered as the purple-black missing texture.)
 */
public class PoisonNeedleRenderer extends EntityRenderer<PoisonNeedleProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public PoisonNeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(PoisonNeedleProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 v = entity.getDeltaMovement();
        if (v.lengthSqr() < 1.0E-5) v = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        Vec3 dir = v.normalize();
        float flyYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float flyPitch = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);

        pose.pushPose();
        pose.translate(0, entity.getBbHeight() * 0.5, 0);
        pose.mulPose(Vector3f.YP.rotationDegrees(-flyYaw));
        pose.mulPose(Vector3f.XP.rotationDegrees(flyPitch));
        // Venom streak behind (additive).
        BloodRenderHelper.streak(pose, buffers, 0.06f, 0.9f, 0.6f);
        // Needle shaft (dark) and tip (venom), as tapered hexahedra along +Z.
        float[][] shaft = ToonRenderHelper.boxCorners(-0.025f, -0.025f, -0.45f, 0.025f, 0.025f, 0.25f);
        ToonRenderHelper.solidHexa(pose, buffers, 0xFF2B1F33, light, shaft, false);
        float[][] tip = {
                {-0.025f, -0.025f, 0.25f}, {0.025f, -0.025f, 0.25f}, {-0.025f, 0.025f, 0.25f}, {0.025f, 0.025f, 0.25f},
                {-0.002f, -0.002f, 0.55f}, {0.002f, -0.002f, 0.55f}, {-0.002f, 0.002f, 0.55f}, {0.002f, 0.002f, 0.55f}};
        ToonRenderHelper.solidHexa(pose, buffers, 0xFF8BE05A, 0xF000F0, tip, false);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PoisonNeedleProjectileEntity entity) {
        return TEXTURE;
    }
}
