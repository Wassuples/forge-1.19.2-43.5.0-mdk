package com.wassuples.demonslayer.events; // Or your package

// Imports...
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.client.renderer.ModRenderTypes;
import com.wassuples.demonslayer.entity.custom.CompassNeedleEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CompassNeedleHandler {

    private static final ResourceLocation COMPASS_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/effect/compass_needle.png");
    private static Entity targetedEntity = null;
    private static final int COMPASS_EFFECT_DURATION_TICKS = 40; // 2 seconds (20 ticks/sec)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (isCompassFormActive(mc.player)) {
                Entity bestTarget = null;
                double highestDot = -1.0;
                final double AIM_THRESHOLD = 0.99;

                Vec3 playerLookVec = mc.player.getViewVector(1.0F);
                double detectionRadius = 128.0;

                List<LivingEntity> potentialTargets = mc.player.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(detectionRadius));

                for (LivingEntity entity : potentialTargets) {
                    if (entity.is(mc.player) || !entity.isAlive()) {
                        continue;
                    }

                    boolean isGlowTarget = entity.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).map(cap ->
                            cap.isGlowing() && cap.getExclusiveViewer() != null && cap.getExclusiveViewer().equals(mc.player.getUUID())
                    ).orElse(false);

                    if (isGlowTarget) {
                        Vec3 vecToEntity = entity.getEyePosition().subtract(mc.player.getEyePosition()).normalize();
                        double dotProduct = playerLookVec.dot(vecToEntity);

                        if (dotProduct > AIM_THRESHOLD && dotProduct > highestDot) {
                            highestDot = dotProduct;
                            bestTarget = entity;
                        }
                    }
                }
                targetedEntity = bestTarget;
            } else {
                targetedEntity = null;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean compassActive = isCompassFormActive(player);

        // === Render the Info Display above targets ===
        if (compassActive && targetedEntity != null && entity.is(targetedEntity)) {
            entity.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                if (cap.isGlowing() && cap.getExclusiveViewer() != null && cap.getExclusiveViewer().equals(player.getUUID())) {
                    renderInfoDisplay(event);
                }
            });
        }
    }

    private static void renderInfoDisplay(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity target = event.getEntity();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        Minecraft mc = Minecraft.getInstance();

        Player player = mc.player;
        if (player == null) {
            return;
        }

        List<Component> displayLines = new ArrayList<>();

        float currentHealth = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float absorption = target.getAbsorptionAmount();
        if (absorption > 0) {
            displayLines.add(Component.literal("❤ ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(String.format("%.0f (+%.0f) / %.0f", currentHealth, absorption, maxHealth))
                            .withStyle(ChatFormatting.WHITE)));
        } else {
            displayLines.add(Component.literal("❤ ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(String.format("%.0f / %.0f", currentHealth, maxHealth))
                            .withStyle(ChatFormatting.WHITE)));
        }

        // Armor Line
        displayLines.add(Component.literal("⛨ ")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.format("%d", target.getArmorValue()))
                        .withStyle(ChatFormatting.WHITE)));

        // Attack Damage Line
        double attackValue = 0.0;
        AttributeInstance attackAttribute = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackValue = attackAttribute.getValue();
        }
        displayLines.add(Component.literal("⚔ ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.format("%.1f", attackValue))
                        .withStyle(ChatFormatting.WHITE)));


        // Player-specific stats
        if (target instanceof Player playerTarget) {
            playerTarget.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breathCap ->
                    displayLines.add(Component.literal("~ ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(String.format("%d / %d", breathCap.getBreath(), breathCap.getMaxBreath()))
                                    .withStyle(ChatFormatting.WHITE)))
            );
            playerTarget.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                    displayLines.add(Component.literal("☆ ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(progression.getRank().name())
                                    .withStyle(ChatFormatting.WHITE)))
            );
        }

        // --- Calculate dimensions for the background ---
        int maxWidth = 0;
        for (Component line : displayLines) {
            int lineWidth = mc.font.width(line);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        int boxWidth = maxWidth + 4; // Add 2 pixels of padding on each side
        int boxHeight = (displayLines.size() * 10) - 2; // 10 pixels per line, -2 for tighter fit

        // --- Positioning ---
        poseStack.pushPose();

        float distance = player.distanceTo(target);
        float clampedDistance = Mth.clamp(distance, 4.0F, 128.0F);
        float dynamicScale = clampedDistance * 0.0035f;

        float baseHeight = target.getBbHeight() + 0.8F;
        if (target.isCustomNameVisible() || target instanceof Player) {
            baseHeight += 0.5F;
        }
        poseStack.translate(0, baseHeight, 0);
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
        poseStack.scale(-dynamicScale, -dynamicScale, dynamicScale);

        // --- Render the Background ---
        int backgroundColor = 0x90000000; // Semi-transparent black
        VertexConsumer builder = buffer.getBuffer(ModRenderTypes.infoBoxBackground());
        Matrix4f matrix = poseStack.last().pose();

        float left = -boxWidth / 2f;
        float right = boxWidth / 2f;
        float top = -1; // Start 1 pixel above the first line of text
        float bottom = boxHeight;

        builder.vertex(matrix, left, top, 0).color(backgroundColor).endVertex();
        builder.vertex(matrix, left, bottom, 0).color(backgroundColor).endVertex();
        builder.vertex(matrix, right, bottom, 0).color(backgroundColor).endVertex();
        builder.vertex(matrix, right, top, 0).color(backgroundColor).endVertex();

        // --- Render the Text ---
        for (int i = 0; i < displayLines.size(); i++) {
            Component line = displayLines.get(i);
            float x = -mc.font.width(line) / 2f; // Center each line individually
            float y = i * 10; // 10 pixels down for each line
            mc.font.drawInBatch(line, x, y, 0xFFFFFF, false, matrix, buffer, true, 0, 0xF000F0);
        }
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onUserHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player user)) return;
        if (!isCompassFormActive(user)) return;

        if (getDodgeCharge(user)) {
            event.setCanceled(true);
            setDodgeCharge(user, false);
            user.level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        } else {
            if (user.getRandom().nextFloat() < 0.75f) {
                setDodgeCharge(user, true);
                user.level.playSound(null, user.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,0.5f, 2.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onUserAttacks(LivingDamageEvent event) {
        if (event.getSource().getMsgId().equals("compass_pierce")) { return; }
        if (!(event.getSource().getEntity() instanceof Player user)) return;
        if (!isCompassFormActive(user)) return;

        LivingEntity target = event.getEntity();
        target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
            if (cap.isGlowing() && cap.getExclusiveViewer() != null && cap.getExclusiveViewer().equals(user.getUUID())) {
                float multiplier = 1.0f + (target.getMaxHealth() / 40f);
                float damage = event.getAmount() * multiplier;

                event.setCanceled(true);
                DamageSource piercingSource = new EntityDamageSource("compass_pierce", user).setMagic();
                target.hurt(piercingSource, damage);
            }
        });
    }

    private static boolean isCompassFormActive(Player player) {
        ActiveForm activeForm = ActiveFormManager.getActiveForm(player);

        if (activeForm == null) {
            return false;
        }
        return activeForm.getForm() == DemonForms.DESTRUCTIVE_DEATH_1;
    }

    private static boolean getDodgeCharge(Player player) {
        // Get the PlayerProgression capability and check our new temporary flag.
        return player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).map(
                PlayerProgression::hasDodgeCharge
        ).orElse(false);
    }

    private static void setDodgeCharge(Player player, boolean hasCharge) {
        // Get the PlayerProgression capability and set our new temporary flag.
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                progression.setDodgeCharge(hasCharge)
        );
    }
}