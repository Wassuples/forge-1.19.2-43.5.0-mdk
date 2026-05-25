package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;
public class ToonShadowAnchorRenderer extends GeoProjectilesRenderer<ToonShadowAnchorEntity> {
    public ToonShadowAnchorRenderer(EntityRendererProvider.Context context) {
        super(context, new ToonShadowAnchorModel());
        this.shadowRadius = 0.0f;
    }
    @Override
    public ResourceLocation getTextureLocation(ToonShadowAnchorEntity entity) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_shadow_anchor.png");
    }
}
