package com.wassuples.demonslayer.entity.renderers;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
public class ToonShadowAnchorModel extends AnimatedGeoModel<ToonShadowAnchorEntity> {
    @Override
    public ResourceLocation getModelResource(ToonShadowAnchorEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/toon/toon_shadow_anchor.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ToonShadowAnchorEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_shadow_anchor.png");
    }
    @Override
    public ResourceLocation getAnimationResource(ToonShadowAnchorEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/toon/toon_shadow_anchor.animation.json");
    }
}
