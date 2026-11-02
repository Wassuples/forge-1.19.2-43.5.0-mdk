package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Mt. Fujikasane's captive demons: humanoid, one of several skins, sized by variant (the horned demon is big). */
public class CaptiveDemonRenderer extends HumanoidMobRenderer<CaptiveDemonEntity, PlayerModel<CaptiveDemonEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[CaptiveDemonEntity.LAST_SKIN + 1];

    static {
        for (int i = 0; i <= CaptiveDemonEntity.LAST_SKIN; i++) {
            TEXTURES[i] = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/captive_demon_" + i + ".png");
        }
    }

    public CaptiveDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CaptiveDemonEntity entity) {
        int v = entity.variant();
        return TEXTURES[v >= 0 && v < TEXTURES.length ? v : 0];
    }

    @Override
    protected void scale(CaptiveDemonEntity entity, PoseStack pose, float partialTick) {
        float s = 0.9375F * CaptiveDemonEntity.scaleFor(entity.variant(), entity.build());
        pose.scale(s, s, s);
    }

    @Override
    protected float getFlipDegrees(CaptiveDemonEntity entity) {
        return 0.0F;   // they crumble to ash
    }
}
