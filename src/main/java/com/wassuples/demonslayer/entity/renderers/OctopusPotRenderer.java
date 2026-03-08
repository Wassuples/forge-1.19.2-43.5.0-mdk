package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.BloodSickleProjectileEntity;
import com.wassuples.demonslayer.entity.custom.OctopusPotEntity;
import com.wassuples.demonslayer.entity.models.OctopusPotModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Octopus Pot entity.
 */
public class OctopusPotRenderer extends GeoEntityRenderer<OctopusPotEntity> {
    public OctopusPotRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OctopusPotModel());
        this.shadowRadius = 1.0f;
    }

    @Override
    public RenderType getRenderType(OctopusPotEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}