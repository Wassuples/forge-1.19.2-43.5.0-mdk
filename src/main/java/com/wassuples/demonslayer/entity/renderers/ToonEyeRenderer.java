package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

public class ToonEyeRenderer extends GeoProjectilesRenderer<ToonEyeProjectileEntity> {

    public ToonEyeRenderer(EntityRendererProvider.Context context) {
        super(context, new ToonEyeModel());
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(ToonEyeProjectileEntity entity) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_eye_projectile.png");
    }
}
