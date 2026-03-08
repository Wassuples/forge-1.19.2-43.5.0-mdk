package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.FlameHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Flame Hashira entity.
 */
public class FlameHashiraRenderer extends HumanoidMobRenderer<FlameHashiraEntity, HashiraModel<FlameHashiraEntity>> {
    private static final ResourceLocation KYOJURO_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/kyojuro.png");

    public FlameHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(FlameHashiraEntity pEntity) {
        return KYOJURO_TEXTURE;
    }
}
