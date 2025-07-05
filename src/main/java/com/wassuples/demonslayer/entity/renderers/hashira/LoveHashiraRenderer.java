package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.LoveHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LoveHashiraRenderer extends HumanoidMobRenderer<LoveHashiraEntity, HashiraModel<LoveHashiraEntity>> {
    private static final ResourceLocation MITSURI_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/mitsuri.png");

    public LoveHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(LoveHashiraEntity pEntity) {
        return MITSURI_TEXTURE;
    }
}
