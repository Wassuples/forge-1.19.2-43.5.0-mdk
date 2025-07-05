package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.entity.custom.PoisonNeedleProjectileEntity;
import com.wassuples.demonslayer.entity.models.PoisonNeedleModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

public class PoisonNeedleRenderer extends GeoProjectilesRenderer<PoisonNeedleProjectileEntity> {
    public PoisonNeedleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PoisonNeedleModel());
    }
}
