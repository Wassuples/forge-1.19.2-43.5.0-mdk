package com.wassuples.demonslayer.entity.renderers;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
public class ToonBananaPeelModel extends AnimatedGeoModel<ToonBananaPeelEntity> {
    @Override
    public ResourceLocation getModelResource(ToonBananaPeelEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/toon/toon_banana_peel.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ToonBananaPeelEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_banana_peel.png");
    }
    @Override
    public ResourceLocation getAnimationResource(ToonBananaPeelEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/toon/toon_banana_peel.animation.json");
    }
}
