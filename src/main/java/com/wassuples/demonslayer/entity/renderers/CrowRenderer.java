package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.CrowEntity;
import com.wassuples.demonslayer.entity.models.CrowModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Crow entity.
 */
public class CrowRenderer extends GeoEntityRenderer<CrowEntity> {
    public CrowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrowModel());
        this.shadowRadius = 0.3f;
    }

    @Override
    public RenderType getRenderType(CrowEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                                    int packedLight, ResourceLocation texture) {
        return RenderType.entityCutout(getTextureLocation(animatable));
    }
}

