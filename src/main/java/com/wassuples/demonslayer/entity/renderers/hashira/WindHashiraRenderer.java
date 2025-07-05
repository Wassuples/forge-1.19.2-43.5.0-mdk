package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.WindHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WindHashiraRenderer extends HumanoidMobRenderer<WindHashiraEntity, HashiraModel<WindHashiraEntity>> {
    private static final ResourceLocation SANEMI_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/sanemi.png");

    public WindHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WindHashiraEntity pEntity) {
        return SANEMI_TEXTURE;
    }
}
