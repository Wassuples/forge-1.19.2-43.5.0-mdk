
package com.wassuples.demonslayer.entity.renderers;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
public class ToonEyeModel extends AnimatedGeoModel<ToonEyeProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(ToonEyeProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/toon/toon_eye_projectile.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ToonEyeProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_eye_projectile.png");
    }
    @Override
    public ResourceLocation getAnimationResource(ToonEyeProjectileEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/toon/toon_eye_projectile.animation.json");
    }
}
