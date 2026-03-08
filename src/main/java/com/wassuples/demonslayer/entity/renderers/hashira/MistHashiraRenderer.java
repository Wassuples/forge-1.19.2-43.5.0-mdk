package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.MistHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Mist Hashira entity.
 */
public class MistHashiraRenderer extends HumanoidMobRenderer<MistHashiraEntity, HashiraModel<MistHashiraEntity>> {
    private static final ResourceLocation MUICHIRO_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/muichiro.png");

    public MistHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(MistHashiraEntity pEntity) {
        return MUICHIRO_TEXTURE;
    }
}
