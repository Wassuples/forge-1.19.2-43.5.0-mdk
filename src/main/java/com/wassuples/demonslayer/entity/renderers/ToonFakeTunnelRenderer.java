package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;
public class ToonFakeTunnelRenderer extends GeoProjectilesRenderer<ToonFakeTunnelEntity> {
    public ToonFakeTunnelRenderer(EntityRendererProvider.Context context) {
        super(context, new ToonFakeTunnelModel());
        this.shadowRadius = 0.0f;
    }
    @Override
    public ResourceLocation getTextureLocation(ToonFakeTunnelEntity entity) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_fake_tunnel.png");
    }
}
