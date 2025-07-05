package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.InsectHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class InsectHashiraRenderer extends HumanoidMobRenderer<InsectHashiraEntity, HashiraModel<InsectHashiraEntity>> {
    private static final ResourceLocation SHINOBU_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/shinobu.png");

    public InsectHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(InsectHashiraEntity pEntity) {
        return SHINOBU_TEXTURE;
    }
}
