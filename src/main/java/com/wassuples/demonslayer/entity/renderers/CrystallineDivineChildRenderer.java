package com.wassuples.demonslayer.entity.renderers;

import com.mojang.authlib.GameProfile;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Client-side GeckoLib renderer for Crystalline Divine Child entity.
 */
public class CrystallineDivineChildRenderer extends HumanoidMobRenderer<CrystallineDivineChildEntity, PlayerModel<CrystallineDivineChildEntity>> {
    private static final ResourceLocation DOMA_CLONE_SKIN =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/doma.png");

    public CrystallineDivineChildRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PlayerModel<>(pContext.getModelSet().bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new IcyTintLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CrystallineDivineChildEntity pEntity) {
        if (pEntity.isNpcClone()) {
            return DOMA_CLONE_SKIN;
        }
        Optional<GameProfile> profile = pEntity.getOwnerProfile();
        if(profile.isPresent()) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile.get());
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    protected boolean isBodyVisible(CrystallineDivineChildEntity pLivingEntity) {
        return false; // We will only render the body in our custom layer
    }
}