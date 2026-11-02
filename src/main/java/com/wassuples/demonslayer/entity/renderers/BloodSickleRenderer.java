package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BloodSickleProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Flying Blood Sickles: a crescent blade of hardened blood spinning flat like a thrown kama, tilted into its line
 * of flight, with a fading crimson streak behind it.
 */
public class BloodSickleRenderer extends EntityRenderer<BloodSickleProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public BloodSickleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(BloodSickleProjectileEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 v = entity.getDeltaMovement();
        if (v.lengthSqr() < 1.0E-5) v = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        Vec3 dir = v.normalize();
        float flyYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float flyPitch = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        float appear = Mth.clamp(age / 3.0f, 0.0f, 1.0f);

        pose.pushPose();
        pose.translate(0, entity.getBbHeight() * 0.5, 0);
        pose.mulPose(Vector3f.YP.rotationDegrees(-flyYaw));
        pose.mulPose(Vector3f.XP.rotationDegrees(flyPitch)); // local +Z = direction of flight

        // Streak behind, in the flight plane.
        BloodRenderHelper.streak(pose, buffers, 0.32f * appear, 1.6f, 0.9f);

        // Blade: lie flat (XY → horizontal flight plane), tilt slightly, spin fast.
        pose.mulPose(Vector3f.XP.rotationDegrees(90.0f));
        pose.mulPose(Vector3f.YP.rotationDegrees(12.0f));
        pose.mulPose(Vector3f.ZP.rotationDegrees(age * 48.0f));
        BloodRenderHelper.crescent(pose, buffers, 0.55f * appear, 1.0f);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodSickleProjectileEntity entity) {
        return TEXTURE;
    }
}
