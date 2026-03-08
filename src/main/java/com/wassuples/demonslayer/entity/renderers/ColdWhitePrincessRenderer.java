package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import com.wassuples.demonslayer.entity.models.ColdWhitePrincessModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * Client-side GeckoLib renderer for Cold White Princess entity.
 */
public class ColdWhitePrincessRenderer extends GeoEntityRenderer<ColdWhitePrincessEntity> {
    public ColdWhitePrincessRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ColdWhitePrincessModel());
        this.shadowRadius = 0.5F;
    }
}