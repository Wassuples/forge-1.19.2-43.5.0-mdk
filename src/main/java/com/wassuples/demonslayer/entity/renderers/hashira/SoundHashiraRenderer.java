package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.SoundHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Sound Hashira entity.
 */
public class SoundHashiraRenderer extends HumanoidMobRenderer<SoundHashiraEntity, HashiraModel<SoundHashiraEntity>> {
    private static final ResourceLocation TENGEN_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/tengen.png");

    public SoundHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SoundHashiraEntity pEntity) {
        return TENGEN_TEXTURE;
    }
}
