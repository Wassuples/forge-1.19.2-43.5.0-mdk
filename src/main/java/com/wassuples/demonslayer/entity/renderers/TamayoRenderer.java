package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.TamayoEntity;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Tamayo entity.
 */
public class TamayoRenderer extends HumanoidMobRenderer<TamayoEntity, HashiraModel<TamayoEntity>> {
    private static final ResourceLocation TAMAYO_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/tamayo.png");

    public TamayoRenderer(EntityRendererProvider.Context context) {
        super(context, new HashiraModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TamayoEntity entity) {
        return TAMAYO_TEXTURE;
    }
}
