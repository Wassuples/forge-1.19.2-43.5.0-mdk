package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.ThunderHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Thunder Hashira entity.
 */
public class ThunderHashiraRenderer extends HumanoidMobRenderer<ThunderHashiraEntity, HashiraModel<ThunderHashiraEntity>> {
    private static final ResourceLocation JIGORO_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/jigoro.png");

    public ThunderHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderHashiraEntity pEntity) {
        return JIGORO_TEXTURE;
    }
}
