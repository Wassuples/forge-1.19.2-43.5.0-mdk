package com.wassuples.demonslayer.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.renderer.ModRenderTypes;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GlowRenderHandler {

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || entity.is(mc.player)) {
            return;
        }

        entity.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
            boolean shouldGlow = cap.isGlowing()
                    && cap.hasExclusiveViewer()
                    && cap.getExclusiveViewer().equals(mc.player.getUUID());

            if (shouldGlow) {
                renderHologramEffect(event);
            }
        });
    }

    private static void renderHologramEffect(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer = event.getRenderer();
        EntityModel<LivingEntity> model = renderer.getModel();
        int packedLight = event.getPackedLight();
        float partialTicks = event.getPartialTick();

        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0, -1.501, 0.0);

        float customScale = 1.01f;
        poseStack.scale(customScale, customScale, customScale);
        ResourceLocation texture = renderer.getTextureLocation(entity);
        RenderType renderType = ModRenderTypes.hologram(texture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        float alpha = 0.4f;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);

        model.renderToBuffer(poseStack, vertexConsumer, packedLight, overlay, 1.0f, 1.0f, 1.0f, alpha);

        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}