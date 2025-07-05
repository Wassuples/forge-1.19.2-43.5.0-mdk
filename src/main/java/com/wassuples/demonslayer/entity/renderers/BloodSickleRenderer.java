package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.BloodSickleProjectileEntity;
import com.wassuples.demonslayer.entity.models.BloodSickleModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

import javax.annotation.Nullable;

public class BloodSickleRenderer extends GeoProjectilesRenderer<BloodSickleProjectileEntity> {
    public BloodSickleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BloodSickleModel());
    }

    @Override
    public RenderType getRenderType(BloodSickleProjectileEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}