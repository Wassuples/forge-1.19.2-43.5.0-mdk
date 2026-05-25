package com.wassuples.demonslayer.entity.renderers;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
public class ToonAnvilModel extends AnimatedGeoModel<ToonAnvilEntity> {
    @Override
    public ResourceLocation getModelResource(ToonAnvilEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/toon/toon_anvil.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ToonAnvilEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_anvil.png");
    }
    @Override
    public ResourceLocation getAnimationResource(ToonAnvilEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/toon/toon_anvil.animation.json");
    }
}
