package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.demon.UpperMoonOneEntity;
import net.minecraft.client.model.HumanoidModel; // Import the standard HumanoidModel
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

// Change the model type in the class signature from DemonModel to HumanoidModel

/**
 * signature.
 */
public class UpperMoonOneRenderer extends HumanoidMobRenderer<UpperMoonOneEntity, HumanoidModel<UpperMoonOneEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_one.png");

    public UpperMoonOneRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(UpperMoonOneEntity pEntity) {
        return TEXTURE;
    }
}