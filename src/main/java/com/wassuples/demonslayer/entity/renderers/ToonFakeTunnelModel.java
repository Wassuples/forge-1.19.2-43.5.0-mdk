package com.wassuples.demonslayer.entity.renderers;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
public class ToonFakeTunnelModel extends AnimatedGeoModel<ToonFakeTunnelEntity> {
    @Override
    public ResourceLocation getModelResource(ToonFakeTunnelEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/toon/toon_fake_tunnel.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ToonFakeTunnelEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_fake_tunnel.png");
    }
    @Override
    public ResourceLocation getAnimationResource(ToonFakeTunnelEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/toon/toon_fake_tunnel.animation.json");
    }
}
