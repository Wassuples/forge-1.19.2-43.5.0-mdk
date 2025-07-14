package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.client.LesserEyeRendererManager;
import com.wassuples.demonslayer.client.gui.BreathHudOverlay;
import com.wassuples.demonslayer.client.gui.BreathingMenuScreen;
import com.wassuples.demonslayer.client.gui.SleepQTEHandler;
import com.wassuples.demonslayer.client.gui.VignetteHudOverlay;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import com.wassuples.demonslayer.entity.renderers.*;
import com.wassuples.demonslayer.entity.renderers.demon.*;
import com.wassuples.demonslayer.entity.custom.BlindingMistCloudEntity;
import com.wassuples.demonslayer.entity.renderers.hashira.*;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.FormUtils;
import com.wassuples.demonslayer.util.KeyBinding;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        private static boolean useKeyHeld = false;
        private static int useKeyHoldTime = 0;
        public static boolean isVignetteActive = false;
        private static boolean canAirJump = true;
        private static boolean wasJumping = false;
        private static final List<Entity> tempGlowers = new ArrayList<>();
        private static final ResourceLocation VIGNETTE_TEXTURE =
                new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/vignette.png");

        public static int lastSyncedStyle = -1;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null || mc.player == null) {
                return;
            }

            LesserEyeRendererManager.tick();

            if (mc.player.getVehicle() instanceof BodhisattvaEntity) {
                // Read movement keys
                Vec3 input = new Vec3(
                        mc.player.input.leftImpulse, // Strafe left/right
                        0,                          // No vertical input from player
                        mc.player.input.forwardImpulse // Move forward/backward
                );

                boolean primaryAttack = mc.options.keyAttack.isDown();
                boolean secondaryAttack = mc.options.keyUse.isDown();

                ModMessages.sendToServer(new BodhisattvaInputC2SPacket(input, primaryAttack, secondaryAttack));
            }

            if (lastSyncedStyle != -1) {
                mc.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    if (style.getStyleIndex() != lastSyncedStyle) {
                        style.changeStyle(lastSyncedStyle);
                    }
                });
            }

            if (KeyBinding.CHANGE_KEY.consumeClick()) {
                ModMessages.sendToServer(new ChangeFormC2SPacket());
            }

            if (KeyBinding.BDA_TOGGLE_KEY.consumeClick()) {
                ModMessages.sendToServer(new ToggleBdaModeC2SPacket());
            }

            if (KeyBinding.OPEN_MENU_KEY.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new BreathingMenuScreen());
                }
            }

            mc.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                BreathingForms currentForm = style.getCurrentForms();
                if (KeyBinding.USE_KEY.isDown()) {
                    if (!useKeyHeld) {
                        useKeyHeld = true;
                        if (currentForm.getInputType() == BreathingForms.InputType.HOLD) {
                            ModMessages.sendToServer(new ChargeFormC2SPacket(ChargeFormC2SPacket.ChargeType.START));
                        }
                    }
                    useKeyHoldTime++;
                } else {
                    if (useKeyHeld) {
                        if (currentForm.getInputType() == BreathingForms.InputType.PRESS) {
                            ModMessages.sendToServer(new UseFormC2SPacket());
                        } else if (currentForm.getInputType() == BreathingForms.InputType.HOLD) {
                            ModMessages.sendToServer(new ChargeFormC2SPacket(ChargeFormC2SPacket.ChargeType.RELEASE, useKeyHoldTime));
                        }
                    }
                    useKeyHeld = false;
                    useKeyHoldTime = 0;
                }
            });

            FormUtils.ActiveForm activeForm = FormUtils.ActiveFormManager.getActiveForm(mc.player);
            boolean isFormActive = activeForm != null && activeForm.getForm() == BreathingForms.WATER_9;

            if (isFormActive) {
                if (mc.player.isOnGround()) {
                    canAirJump = true;
                }

                boolean isCurrentlyJumping = mc.player.getDeltaMovement().y > 0;

                if (wasJumping && !isCurrentlyJumping && !mc.player.isOnGround() && canAirJump) {
                    if (mc.options.keyJump.isDown()) {
                        canAirJump = false;
                        ModMessages.sendToServer(new PerformAirJumpC2SPacket());
                    }
                }
                wasJumping = isCurrentlyJumping;
            } else {
                canAirJump = true;
                wasJumping = false;
            }
        }

        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            Camera camera = event.getCamera();
            if (!(camera.getEntity() instanceof Player player)) return;

            boolean isImmune = player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                    // Check if their current style is MIST
                    .map(style -> style.getCurrentStyles() == BreathingStyles.MIST)
                    // If they don't have the capability for some reason, they are not immune
                    .orElse(false);

            // If the player is a Mist Breather, skip the fog effect entirely for them.
            if (isImmune) {
                return;
            }

            Level level = camera.getEntity().level;

            // Check if the camera (the player) is inside any of our mist clouds.
            List<BlindingMistCloudEntity> nearbyClouds = level.getEntitiesOfClass(
                    BlindingMistCloudEntity.class,
                    camera.getEntity().getBoundingBox().inflate(0.5) // Check a small box around the player
            );

            // If the player is inside at least one mist cloud...
            if (!nearbyClouds.isEmpty()) {
                // ...dramatically reduce the fog distance to create a dense "pea soup" effect.
                float fogEnd = 12.0f; // Player can't see anything beyond 12 blocks.
                float fogStart = 1.0f; // Fog starts very close to the player.

                // These methods override the default fog settings.
                event.setFarPlaneDistance(fogEnd);
                event.setNearPlaneDistance(fogStart);
                event.setCanceled(true); // This tells the game to use our new fog values.
            }
        }

        @SubscribeEvent
        public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
            Camera camera = event.getCamera();
            if (!(camera.getEntity() instanceof Player player)) return;

            boolean isImmune = player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                    // Check if their current style is MIST
                    .map(style -> style.getCurrentStyles() == BreathingStyles.MIST)
                    // If they don't have the capability for some reason, they are not immune
                    .orElse(false);

            // If the player is a Mist Breather, skip the fog effect entirely for them.
            if (isImmune) {
                return;
            }

            Level level = camera.getEntity().level;

            List<BlindingMistCloudEntity> nearbyClouds = level.getEntitiesOfClass(
                    BlindingMistCloudEntity.class,
                    camera.getEntity().getBoundingBox().inflate(0.5)
            );

            if (!nearbyClouds.isEmpty()) {
                // If inside a cloud, set the fog color to a hazy gray-white.
                event.setRed(0.9f);

                event.setGreen(0.9f);
                event.setBlue(0.95f);
            }
        }

        @SubscribeEvent
        public static void onLeftClickAir(PlayerInteractEvent.LeftClickEmpty event) {
            Player player = event.getEntity();
            boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(PlayerDemon::isDemon)
                    .orElse(false);
            if (isDemon) {
                ModMessages.sendToServer(new DemonMeleeAttackC2SPacket());
            } else if (player.getMainHandItem().getItem() instanceof NichirinSwordItem) {
                ModMessages.sendToServer(new SwingInAirC2SPacket());
            }
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() == 1) {
                if (SleepQTEHandler.isActive()) {
                    SleepQTEHandler.handleKeyInput(event.getKey());
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.CHANGE_KEY);
            event.register(KeyBinding.USE_KEY);
            event.register(KeyBinding.OPEN_MENU_KEY);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            // We register our overlay here.
            // The key "breath" is a unique ID for our overlay.
            // We want it to render just above the player's health bar.
            event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "breath", BreathHudOverlay.HUD_BREATH);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "vignette", VignetteHudOverlay.HUD_VIGNETTE);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // MISC.
            event.registerEntityRenderer(ModEntityTypes.BLINDING_MIST_CLOUD.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MIST_CLONE.get(), MistCloneRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MOTHER_EYE.get(), MotherEyeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.BLOOD_SICKLE.get(), BloodSickleRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.KILLER_GOLDFISH.get(), KillerGoldfishRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.POISON_NEEDLE.get(), PoisonNeedleRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.WATER_PRISON_POT.get(), WaterPrisonPotRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.OCTOPUS_POT.get(), OctopusPotRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.WEEPING_SPEAR.get(), WeepingSpearRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.COMPASS_EFFECT.get(), CompassNeedleRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.COLD_WHITE_PRINCESS.get(), ColdWhitePrincessRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOTUS_SHARD.get(), LotusShardRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CRYSTALLINE_DIVINE_CHILD.get(), CrystallineDivineChildRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.BODHISATTVA.get(), BodhisattvaRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.THROWN_NICHIRIN.get(), ThrownNichirinRenderer::new);
            // HASHIRA
            event.registerEntityRenderer(ModEntityTypes.HASHIRA.get(), HashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.WATER_HASHIRA.get(), WaterHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.INSECT_HASHIRA.get(), InsectHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOVE_HASHIRA.get(), LoveHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SNAKE_HASHIRA.get(), SnakeHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.WIND_HASHIRA.get(), WindHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.STONE_HASHIRA.get(), StoneHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MIST_HASHIRA.get(), MistHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FLAME_HASHIRA.get(), FlameHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FLOWER_HASHIRA.get(), FlowerHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SOUND_HASHIRA.get(), SoundHashiraRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.THUNDER_HASHIRA.get(), ThunderHashiraRenderer::new);
            // DEMONS
            event.registerEntityRenderer(ModEntityTypes.FODDER_DEMON.get(), FodderDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWLY_DEMON.get(), LowlyDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.COMMON_DEMON.get(), CommonDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.GREATER_DEMON.get(), GreaterDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.ELITE_DEMON.get(), EliteDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.HAND_DEMON.get(), HandDemonRenderer::new);
            // MOONS
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_SIX.get(), LowerMoonSixRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_FIVE.get(), LowerMoonFiveRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CUTTING_THREAD_CAGE.get(), CuttingThreadCageRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.THREAD_WHEEL.get(), ThreadWheelRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public class ForgeEventHandlers {

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof LivingEntity) {
                if (!event.getObject().getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).isPresent()) {
                    event.addCapability(new ResourceLocation(DemonSlayer.MOD_ID, "glow"), new GlowCapabilityProvider());
                }
            }
        }
    }
}