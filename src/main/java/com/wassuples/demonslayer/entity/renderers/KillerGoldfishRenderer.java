package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.KillerGoldfishEntity;
import com.wassuples.demonslayer.entity.models.KillerGoldfishModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

public class KillerGoldfishRenderer extends GeoEntityRenderer<KillerGoldfishEntity> {
    public KillerGoldfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KillerGoldfishModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(KillerGoldfishEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}