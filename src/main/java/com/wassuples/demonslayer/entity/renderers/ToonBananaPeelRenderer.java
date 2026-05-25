package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;
public class ToonBananaPeelRenderer extends GeoProjectilesRenderer<ToonBananaPeelEntity> {
    public ToonBananaPeelRenderer(EntityRendererProvider.Context context) {
        super(context, new ToonBananaPeelModel());
        this.shadowRadius = 0.0f;
    }
    @Override
    public ResourceLocation getTextureLocation(ToonBananaPeelEntity entity) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_banana_peel.png");
    }
}
