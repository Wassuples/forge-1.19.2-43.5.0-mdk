package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.SnakeHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Snake Hashira entity.
 */
public class SnakeHashiraRenderer extends HumanoidMobRenderer<SnakeHashiraEntity, HashiraModel<SnakeHashiraEntity>> {
    private static final ResourceLocation OBANAI_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/obanai.png");

    public SnakeHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SnakeHashiraEntity pEntity) {
        return OBANAI_TEXTURE;
    }
}
