package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.LotusShardProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Scattering Lotuses: each shard is a small crystal lotus spinning edge-on like a thrown blade, its petals sharp
 * enough to cut, trailing a glint of frost. (Replaces the GeckoLib model and the per-tick snowflake particle.)
 */
public class LotusShardRenderer extends EntityRenderer<LotusShardProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public LotusShardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(LotusShardProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 vel = entity.getDeltaMovement();
        Vec3 dir = vel.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : vel.normalize();
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick));
        // The flower lies in the plane of flight (its "up" is sideways), spinning around that axis.
        Vec3 axis = dir.cross(new Vec3(0, 1, 0));
        if (axis.lengthSqr() < 1.0E-4) axis = new Vec3(1, 0, 0);
        axis = axis.normalize();
        pose.pushPose();
        HantenguRenderHelper.faceAlong(pose, axis);
        pose.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(90.0f)); // lotus' +Y onto the spin axis
        pose.mulPose(com.mojang.math.Vector3f.YP.rotation(age * 0.9f));
        DomaRenderHelper.lotus(pose, buffers, Vec3.ZERO, 0.75f, 0.85f, 0.0f, 0xF000F0, null);
        pose.popPose();
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        ThreadRenderHelper.ribbon(pose, vc, cam, dir.scale(-0.2), dir.scale(-1.8), 0.3f, 0x50A8DCFF);
        ThreadRenderHelper.ribbon(pose, vc, cam, dir.scale(-0.2), dir.scale(-1.1), 0.08f, 0xC0E8F8FF);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LotusShardProjectileEntity entity) {
        return TEXTURE;
    }
}
