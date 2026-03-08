package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.DemonModel;
import com.wassuples.demonslayer.entity.custom.demon.CommonDemonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Common Demon entity.
 */
public class CommonDemonRenderer extends HumanoidMobRenderer<CommonDemonEntity, DemonModel<CommonDemonEntity>> {

    private static final ResourceLocation COMMON_DEMON_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/common_demon01.png");

    public CommonDemonRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CommonDemonEntity pEntity) {
        return COMMON_DEMON_TEXTURE;
    }
}