package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NemesisDemonRenderer extends MobRenderer<NemesisDemonEntity, PlayerModel<NemesisDemonEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/zombie/zombie.png");

    public NemesisDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NemesisDemonEntity entity) {
        return TEXTURE;
    }
}

