package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;
public class ToonAnvilRenderer extends GeoProjectilesRenderer<ToonAnvilEntity> {
    public ToonAnvilRenderer(EntityRendererProvider.Context context) {
        super(context, new ToonAnvilModel());
        this.shadowRadius = 0.0f;
    }
    @Override
    public ResourceLocation getTextureLocation(ToonAnvilEntity entity) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_anvil.png");
    }
}
