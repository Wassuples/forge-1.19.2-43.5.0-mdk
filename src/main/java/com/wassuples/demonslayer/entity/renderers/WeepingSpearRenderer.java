package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.entity.custom.WeepingSpearProjectileEntity;
import com.wassuples.demonslayer.entity.models.WeepingSpearModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

public class WeepingSpearRenderer extends GeoProjectilesRenderer<WeepingSpearProjectileEntity> {
    public WeepingSpearRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WeepingSpearModel());
    }
}