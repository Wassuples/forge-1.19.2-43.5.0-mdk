package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Icy Tint Layer.
 */
public class IcyTintLayer extends RenderLayer<CrystallineDivineChildEntity, PlayerModel<CrystallineDivineChildEntity>> {
    public IcyTintLayer(RenderLayerParent<CrystallineDivineChildEntity, PlayerModel<CrystallineDivineChildEntity>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CrystallineDivineChildEntity pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        PlayerModel<CrystallineDivineChildEntity> model = this.getParentModel();
        ResourceLocation texture = this.getTextureLocation(pLivingEntity);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entityTranslucent(texture));
        model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        model.renderToBuffer(pMatrixStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 0.7F, 0.9F, 1.0F, 0.6F);
    }
}