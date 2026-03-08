package com.wassuples.demonslayer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.networking.packet.UpdateCompassInfoPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side component: Compass Info Renderer.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CompassInfoRenderer {

    private static int targetEntityId = -1;
    private static final List<Component> infoLines = new ArrayList<>();

    public static void updateInfo(UpdateCompassInfoPacket packet) {
        targetEntityId = packet.getEntityId();
        infoLines.clear();

        if (targetEntityId != -1) {
            infoLines.add(packet.getEntityTypeName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE));
            infoLines.add(Component.literal(String.format("XYZ: %d, %d, %d", packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ())).withStyle(ChatFormatting.DARK_GRAY));
            infoLines.add(Component.literal("HP: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.format("%.1f / %.1f", packet.getHealth(), packet.getMaxHealth())).withStyle(ChatFormatting.RED)));
            infoLines.add(Component.literal("Armor: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(packet.getArmor())).withStyle(ChatFormatting.BLUE)));
            if (packet.getAir() < packet.getMaxAir()) {
                infoLines.add(Component.literal("Air: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getAir() + " / " + packet.getMaxAir()).withStyle(ChatFormatting.AQUA)));
            }
            if (packet.getHunger() != -1) {
                infoLines.add(Component.literal("Hunger: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(packet.getHunger())).withStyle(ChatFormatting.GOLD)));
            }
            Component race = Component.literal(packet.isDemon() ? "Demon" : "Human").withStyle(packet.isDemon() ? ChatFormatting.DARK_RED : ChatFormatting.GREEN);
            infoLines.add(Component.literal("Race: ").withStyle(ChatFormatting.GRAY).append(race));

            if (packet.isDemon()) {
                if (!packet.getDemonRank().isEmpty()) infoLines.add(Component.literal("Demon Rank: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getDemonRank()).withStyle(ChatFormatting.DARK_PURPLE)));
                if (packet.getBloodPoints() != -1) infoLines.add(Component.literal("Blood Points: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(packet.getBloodPoints())).withStyle(ChatFormatting.RED)));
                if (!packet.getBda().equals("None")) infoLines.add(Component.literal("BDA: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getBda()).withStyle(ChatFormatting.LIGHT_PURPLE)));
            } else {
                if (!packet.getSlayerRank().isEmpty()) infoLines.add(Component.literal("Slayer Rank: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getSlayerRank()).withStyle(ChatFormatting.AQUA)));
                if (packet.getBreath() != -1) infoLines.add(Component.literal("Breath: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getBreath() + " / " + packet.getMaxBreath()).withStyle(ChatFormatting.AQUA)));
                if (!packet.getStyle().isEmpty()) infoLines.add(Component.literal("Style: ").withStyle(ChatFormatting.GRAY).append(Component.literal(packet.getStyle()).withStyle(ChatFormatting.YELLOW)));
            }

            if (!packet.getMainHandItem().getString().equals("Air")) infoLines.add(Component.literal("Main Hand: ").withStyle(ChatFormatting.GRAY).append(packet.getMainHandItem()));
            if (!packet.getOffHandItem().getString().equals("Air")) infoLines.add(Component.literal("Off Hand: ").withStyle(ChatFormatting.GRAY).append(packet.getOffHandItem()));

            if (!packet.getEffects().isEmpty()) {
                infoLines.add(Component.literal("Effects:").withStyle(ChatFormatting.GRAY));
                for (String effect : packet.getEffects()) {
                    infoLines.add(Component.literal("  - " + effect).withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelLastEvent event) {
        if (targetEntityId == -1 || infoLines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity target = mc.level.getEntity(targetEntityId);

        if (target == null || !target.isAlive()) {
            targetEntityId = -1;
            return;
        }

        Font font = mc.font;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        double x = target.getX() - cameraPos.x();
        double y = target.getY() + target.getBbHeight() + 0.75 - cameraPos.y();
        double z = target.getZ() - cameraPos.z();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = poseStack.last().pose();

        int maxWidth = infoLines.stream().mapToInt(font::width).max().orElse(0);
        int boxHeight = infoLines.size() * 10;
        int padding = 4;

        float left = -maxWidth / 2.0f - padding;
        float right = maxWidth / 2.0f + padding;
        float top = -padding;
        float bottom = boxHeight - 10 + padding + 1;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        int color = 0x90000000;
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, left, bottom, 0.01f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, right, bottom, 0.01f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, right, top, 0.01f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, left, top, 0.01f).color(r, g, b, a).endVertex();
        tesselator.end();

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        int fullBright = 15728880;
        for (int i = 0; i < infoLines.size(); i++) {
            Component line = infoLines.get(i);
            float offset = (float) (-font.width(line) / 2);
            float yOffset = i * 10;
            font.drawInBatch(line, offset, yOffset, 0xFFFFFF, false, matrix, bufferSource, false, 0, fullBright);
        }

        poseStack.popPose();
    }
}