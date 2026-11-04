package com.wassuples.demonslayer.client.mission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.dialogue.ClientDialogueHandler;
import com.wassuples.demonslayer.networking.packet.QuestMarkersS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Side errands are found in the world, not in a list: a paper talisman (ofuda) floats over anyone who has one for you.
 * <ul>
 *   <li>a fresh talisman with a vermilion seal, swaying: they have something to ask;</li>
 *   <li>a faded grey one: the errand is under way;</li>
 *   <li>a talisman glowing gold: come back to them, it's done (or ready to hand in).</li>
 * </ul>
 * Hidden while the character is speaking, and beyond 48 blocks.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestMarkerRenderer {
    private static final ResourceLocation FILL = new ResourceLocation(DemonSlayer.MOD_ID, "textures/misc/speech_fill.png");
    private static final Map<Integer, Byte> MARKERS = new HashMap<>();

    private QuestMarkerRenderer() {
    }

    public static void set(Map<Integer, Byte> markers) {
        MARKERS.clear();
        MARKERS.putAll(markers);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKERS.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || MARKERS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        long now = mc.level.getGameTime();
        float pt = event.getPartialTick();
        float time = now + pt;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean drew = false;
        for (Map.Entry<Integer, Byte> m : MARKERS.entrySet()) {
            Entity e = mc.level.getEntity(m.getKey());
            if (e == null || e.isInvisible() || ClientDialogueHandler.speechFor(e.getId(), now) != null) continue;
            double x = Mth.lerp(pt, e.xOld, e.getX()), y = Mth.lerp(pt, e.yOld, e.getY()), z = Mth.lerp(pt, e.zOld, e.getZ());
            if (cam.distanceToSqr(x, y, z) > 48 * 48) continue;
            float bob = Mth.sin(time * 0.08F + e.getId()) * 0.07F;
            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y + e.getBbHeight() + 0.95 + bob, z - cam.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(time * 0.05F + e.getId()) * 6.0F));
            pose.scale(-0.025F, -0.025F, 0.025F);
            draw(pose.last().pose(), buffers, m.getValue(), time);
            pose.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch();
    }

    private static void draw(Matrix4f m, MultiBufferSource buffers, byte kind, float time) {
        int full = 0xF000F0;
        boolean active = kind == QuestMarkersS2CPacket.ACTIVE, ready = kind == QuestMarkersS2CPacket.READY;
        int paper = active ? 0x9A948A : 0xF2E9D8, ink = active ? 0x5A5450 : 0x2A1E33, seal = active ? 0x7A6A66 : 0xC8281E;
        float a = active ? 0.8F : 1.0F;
        if (ready) {
            float pulse = 0.35F + 0.25F * Mth.sin(time * 0.2F);
            rect(buffers, m, -10, -16, 10, 16, 0.05F, 0xF0C040, pulse, full);
            rect(buffers, m, -8, -14, 8, 14, 0.045F, 0xFFE080, pulse, full);
        }
        rect(buffers, m, -6.5F, -14.5F, 6.5F, 14.5F, 0.04F, ready ? 0xD8A830 : 0x3B2A4A, a, full);   // edge
        rect(buffers, m, -6, -14, 6, 14, 0.03F, paper, a, full);                                         // paper
        rect(buffers, m, -3, -11, 3, -5, 0.02F, seal, a, full);                                          // the seal
        rect(buffers, m, -2, -10, 2, -6, 0.015F, paper, a * 0.35F, full);
        rect(buffers, m, -0.6F, -3, 0.6F, 11, 0.02F, ink, a, full);                                     // brush strokes
        rect(buffers, m, -3.5F, -1, 3.5F, 0, 0.02F, ink, a, full);
        rect(buffers, m, -2.5F, 3, 2.5F, 4, 0.02F, ink, a, full);
        rect(buffers, m, -3, 7, 1, 8, 0.02F, ink, a, full);
    }

    private static void rect(MultiBufferSource buffers, Matrix4f m, float x0, float y0, float x1, float y1, float z, int rgb, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.text(FILL));
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, a = (int) (Math.max(0, Math.min(1, alpha)) * 255);
        vc.vertex(m, x0, y1, z).color(r, g, b, a).uv(0, 1).uv2(light).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).uv(1, 1).uv2(light).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).uv(1, 0).uv2(light).endVertex();
        vc.vertex(m, x0, y0, z).color(r, g, b, a).uv(0, 0).uv2(light).endVertex();
        vc.vertex(m, x0, y0, z).color(r, g, b, a).uv(0, 0).uv2(light).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).uv(1, 0).uv2(light).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).uv(1, 1).uv2(light).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, a).uv(0, 1).uv2(light).endVertex();
    }
}
