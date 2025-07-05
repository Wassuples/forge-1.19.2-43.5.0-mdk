package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.StoneHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StoneHashiraRenderer extends HumanoidMobRenderer<StoneHashiraEntity, HashiraModel<StoneHashiraEntity>> {
    private static final ResourceLocation GYOMEI_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/gyomei.png");

    public StoneHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(StoneHashiraEntity pEntity) {
        return GYOMEI_TEXTURE;
    }
}
