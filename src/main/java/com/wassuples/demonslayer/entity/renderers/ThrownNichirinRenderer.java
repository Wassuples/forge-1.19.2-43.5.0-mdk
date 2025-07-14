package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.entity.custom.ThrownNichirinEntity;
import com.wassuples.demonslayer.entity.models.ThrownNichirinModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

public class ThrownNichirinRenderer extends GeoProjectilesRenderer<ThrownNichirinEntity> {
    public ThrownNichirinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ThrownNichirinModel());
    }
}