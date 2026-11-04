package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CrowEntity;
import com.wassuples.demonslayer.entity.models.KasugaiCrowModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws the Kasugai crow ({@link KasugaiCrowModel}). */
public class CrowRenderer extends MobRenderer<CrowEntity, KasugaiCrowModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/kasugai_crow_v2.png");

    public CrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KasugaiCrowModel(ctx.bakeLayer(KasugaiCrowModel.LAYER)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(CrowEntity crow) {
        return TEXTURE;
    }
}
