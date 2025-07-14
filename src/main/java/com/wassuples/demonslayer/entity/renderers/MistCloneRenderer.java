package com.wassuples.demonslayer.entity.renderers;

import com.mojang.authlib.GameProfile;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.MistCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class MistCloneRenderer extends HumanoidMobRenderer<MistCloneEntity, PlayerModel<MistCloneEntity>> {
    private static final ResourceLocation HASHIRA_CLONE_SKIN =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/muichiro.png");

    public MistCloneRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PlayerModel<>(pContext.getModelSet().bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(pContext.getModelSet().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(pContext.getModelSet().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));

        this.addLayer(new ItemInHandLayer<>(this, pContext.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MistCloneEntity pEntity) {
        if (pEntity.isNpcClone()) {
            return HASHIRA_CLONE_SKIN;
        }

        Optional<GameProfile> profile = pEntity.getOwnerProfile();
        if(profile.isPresent()) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile.get());
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }
}