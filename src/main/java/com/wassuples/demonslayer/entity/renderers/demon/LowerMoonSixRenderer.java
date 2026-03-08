package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.DemonModel;
import com.wassuples.demonslayer.entity.custom.demon.LowerMoonSixEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Lower Moon Six entity.
 */
public class LowerMoonSixRenderer extends HumanoidMobRenderer<LowerMoonSixEntity, DemonModel<LowerMoonSixEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lower_moon_six.png");

    public LowerMoonSixRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(LowerMoonSixEntity pEntity) {
        return TEXTURE;
    }
}