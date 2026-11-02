package com.wassuples.demonslayer.client.dialogue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Paper speech bubbles over anyone who says something out loud ({@code NpcSpeechS2CPacket}), and the mod's own name
 * plate for story characters. Story NPCs draw theirs from their renderer; any other entity that speaks (the Hand
 * Demon...) gets its bubble from {@link #onRenderLevel}.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpeechBubbleRenderer {
    private static final ResourceLocation FILL = new ResourceLocation(DemonSlayer.MOD_ID, "textures/misc/speech_fill.png");
    private static final int BUBBLE_W = 150;
    private static final int PAPER = 0xF2E9D8, INK = 0x2A1E33, EDGE = 0x3B2A4A, DEMON_PAPER = 0x2A0E12, DEMON_INK = 0xF0D0D0;

    private SpeechBubbleRenderer() {
    }

    /** Bubbles over speakers that aren't story NPCs (those draw their own), whatever renderer they use. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        for (int id : ClientDialogueHandler.speakers()) {
            Entity e = mc.level.getEntity(id);
            if (!(e instanceof LivingEntity living) || e instanceof StoryNpcEntity) continue;
            if (ClientDialogueHandler.speechFor(id, now) == null) continue;
            float pt = event.getPartialTick();
            double x = Mth.lerp(pt, e.xOld, e.getX()) - cam.x, y = Mth.lerp(pt, e.yOld, e.getY()) - cam.y, z = Mth.lerp(pt, e.zOld, e.getZ()) - cam.z;
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(x, y, z);
            draw(living, null, 0xD8263A, pt, pose, buffers, mc.getEntityRenderDispatcher(), mc.font);
            pose.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch();
    }

    /**
     * Draws the name plate ({@code name} non-null, within 12 blocks) and the current speech bubble above {@code entity}.
     * Called with the pose stack at the entity's feet.
     */
    public static void draw(LivingEntity entity, @Nullable Component name, int color, float partialTick, PoseStack pose, MultiBufferSource buffers,
                            EntityRenderDispatcher dispatcher, Font font) {
        double distSq = dispatcher.distanceToSqr(entity);
        if (distSq > 32 * 32 || entity.isInvisible()) return;
        long now = entity.level.getGameTime();
        ClientDialogueHandler.Speech speech = ClientDialogueHandler.speechFor(entity.getId(), now);
        boolean showName = name != null && (distSq < 12 * 12 || speech != null);
        if (!showName && speech == null) return;
        boolean demon = name == null;
        int full = 0xF000F0;

        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D);
        pose.mulPose(dispatcher.cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f m = pose.last().pose();

        if (showName) {
            FormattedCharSequence label = name.getVisualOrderText();
            int nw = font.width(label);
            rect(buffers, m, -nw / 2f - 3, -1, nw / 2f + 3, 9, 0.02F, 0x000000, 0.45F, full);
            rect(buffers, m, -nw / 2f - 3, 9, nw / 2f + 3, 10, 0.02F, color & 0xFFFFFF, 0.9F, full);
            font.drawInBatch(label, -nw / 2f, 0, 0xFF000000 | color, false, m, buffers, false, 0, full);
        }

        if (speech != null) {
            float a = speech.alpha(now, partialTick);
            List<FormattedCharSequence> lines = font.split(speech.text(), BUBBLE_W);
            int w = 0;
            for (FormattedCharSequence l : lines) w = Math.max(w, font.width(l));
            float pad = 5;
            float bottom = showName ? -6 : 4, top = bottom - lines.size() * 10 - pad * 2 + 2;
            float left = -w / 2f - pad, right = w / 2f + pad;
            int paper = demon ? DEMON_PAPER : PAPER, ink = demon ? DEMON_INK : INK;
            rect(buffers, m, left - 1, top - 1, right + 1, bottom + 1, 0.03F, demon ? 0x8A1020 : EDGE, a, full);
            rect(buffers, m, left, top, right, bottom, 0.02F, paper, a * 0.95F, full);
            rect(buffers, m, left, top, left + 2, bottom, 0.015F, color & 0xFFFFFF, a, full);
            rect(buffers, m, -3, bottom, 3, bottom + 1, 0.02F, paper, a * 0.95F, full);
            rect(buffers, m, -2, bottom + 1, 2, bottom + 2, 0.02F, paper, a * 0.95F, full);
            rect(buffers, m, -1, bottom + 2, 1, bottom + 3, 0.02F, demon ? 0x8A1020 : EDGE, a, full);
            int alpha = Math.max(5, (int) (a * 255)) << 24;
            float y = top + pad;
            for (FormattedCharSequence l : lines) {
                font.drawInBatch(l, -font.width(l) / 2f, y, alpha | ink, false, m, buffers, false, 0, full);
                y += 10;
            }
        }
        pose.popPose();
    }

    /** A flat coloured rectangle in label space (emitted both ways round, so face culling never hides it). */
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
