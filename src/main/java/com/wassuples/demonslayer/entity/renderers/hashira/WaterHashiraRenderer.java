package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.WaterHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Water Hashira entity.
 */
public class WaterHashiraRenderer extends HumanoidMobRenderer<WaterHashiraEntity, HashiraModel<WaterHashiraEntity>> {
    private static final ResourceLocation GIYU_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/giyu.png");

    public WaterHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterHashiraEntity pEntity) {
        return GIYU_TEXTURE;
    }
}
