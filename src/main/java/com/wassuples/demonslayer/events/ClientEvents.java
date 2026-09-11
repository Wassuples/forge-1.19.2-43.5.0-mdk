package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.client.gui.*;
import com.wassuples.demonslayer.client.LesserEyeRendererManager;
import com.wassuples.demonslayer.client.DemonThreatEffects;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import com.wassuples.demonslayer.entity.custom.BlindingMistCloudEntity;
import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.styles.*;
import com.wassuples.demonslayer.util.KeyBinding;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client-side event handlers for input processing, rendering hooks, and HUD management.
 *
 * <p>Contains nested event subscriber classes for:
 * <ul>
 *   <li><b>Input handling</b> — keybind processing for form use, style switching, form cycling, BDA toggle</li>
 *   <li><b>Rendering</b> — glow capability rendering, vignette effects, blinding mist, Bodhisattva ice overlay</li>
 *   <li><b>HUD overlays</b> — breath bar, demon threat indicator, lesser eye rendering</li>
 *   <li><b>Entity capabilities</b> — attaches glow capability to non-player living entities</li>
 * </ul>
 */
public class ClientEvents {
    private static boolean abilityKeyIsDown = false;
    public static void resetAbilityKeyState() {
        abilityKeyIsDown = false;
    }

    @Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        private static final int useKeyHoldTime = 0;
        public static boolean isVignetteActive = false;

        @SubscribeEvent
        public static void onRenderBlockOverlay(net.minecraftforge.client.event.RenderBlockScreenEffectEvent event) {
            if (event.getOverlayType() == net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
                Player player = event.getPlayer();
                if (player == null) return;

                player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
                    GravityDirection dir = gravity.getGravity();
                    if (dir != GravityDirection.DOWN) {
                        // Vanilla checks the block position exactly at the player's eye coordinates.
                        // Because the player is completely flush against the custom gravity "floor" (the wall),
                        // floating-point precision can cause the eye coordinate to round inside the solid wall.
                        // We push the checked position mathematically 0.1 blocks away from the floor.
                        Vec3 adjustedEyePos = player.getEyePosition().add(GravityUtil.getWorldUp(dir).scale(0.1));
                        net.minecraft.core.BlockPos adjustedBlockPos = new net.minecraft.core.BlockPos(adjustedEyePos);
                        net.minecraft.world.level.block.state.BlockState actualState = player.level.getBlockState(adjustedBlockPos);

                        // If the mathematically adjusted block where our eyes ACTUALLY are is not view-blocking
                        // (like air), we are suffering a rounding error. Cancel the fake suffocation.
                        if (!actualState.isViewBlocking(player.level, adjustedBlockPos)) {
                            event.setCanceled(true);
                        }
                    }
                });
            }
        }

        private static final List<Entity> tempGlowers = new ArrayList<>();
        private static final ResourceLocation VIGNETTE_TEXTURE =
                new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/vignette.png");
        private static final int HOLD_DURATION_TICKS = 10;
        private static int switchFormKeyHoldTicks = 0;
        private static boolean wasSwitchFormKeyDown = false;
        private static final java.util.Set<Integer> gravityPosePushed = new java.util.HashSet<>();

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

            mc.player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
                gravity.tickInterpolation();
                gravity.tickTransientState();

                GravityDirection direction = gravity.getGravity();
                boolean creativeFlight = mc.player.getAbilities().flying && mc.player.getAbilities().mayfly;
                boolean isCustomGravity = direction != GravityDirection.DOWN
                        && !mc.player.isPassenger()
                        && !mc.player.isSpectator();
                boolean useCustomGravity = isCustomGravity && !creativeFlight;

                GravityUtil.refreshGravityHitbox(mc.player, direction);
                if (isCustomGravity) {
                    mc.player.setNoGravity(true);
                } else if (mc.player.isNoGravity()) {
                    mc.player.setNoGravity(false);
                }

                if (useCustomGravity) {
                    // Mirror server-side custom gravity locally to keep client movement packets consistent.
                    boolean grounded = GravityUtil.updateGroundStateForCustomGravity(mc.player, direction, gravity.isSurfaceDetachActive());
                    Vec3 vel = GravityUtil.applyCustomGravityStep(mc.player.getDeltaMovement(), direction, grounded);
                    mc.player.setDeltaMovement(vel);

                    mc.player.fallDistance = 0.0F;
                }
            });

            DemonThreatEffects.clientTick();

            PieSplatOverlay.tick();
            LesserEyeRendererManager.tick();

            if (mc.player.getVehicle() instanceof BodhisattvaEntity) {
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

            if (KeyBinding.CHANGE_KEY.isDown()) {
                // Key is currently being held down
                if (Minecraft.getInstance().screen == null) { // Don't tick up if a GUI is already open
                    switchFormKeyHoldTicks++;
                }

                if (switchFormKeyHoldTicks == HOLD_DURATION_TICKS) {
                    // We've held it long enough, open the wheel
                    openFormSelectionScreen();
                }
                wasSwitchFormKeyDown = true;
            } else {
                // Key is not being held down
                if (wasSwitchFormKeyDown) {
                    // This means the key was JUST released
                    if (switchFormKeyHoldTicks < HOLD_DURATION_TICKS) {
                        // It was a short press (a tap), so just cycle the form
                        ModMessages.sendToServer(new ChangeFormC2SPacket());
                    }
                    // Reset for the next key press
                    switchFormKeyHoldTicks = 0;
                }
                wasSwitchFormKeyDown = false;
            }

            if (KeyBinding.BDA_TOGGLE_KEY.consumeClick()) {
                ModMessages.sendToServer(new ToggleBdaModeC2SPacket());
            }

            if (KeyBinding.CHANGE_BACK_KEY.consumeClick()) {
                ModMessages.sendToServer(new ChangeFormC2SPacket(true));
            }

            if (KeyBinding.OPEN_MENU_KEY.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new BreathingMenuScreen());
                }
            }

            if (KeyBinding.USE_KEY.isDown()) {
                // Edge-detect the press: only send PRESS on the first tick the key is held,
                // not every tick (consumeClick() fires true every tick while held, which
                // caused start/stop/start/stop every server tick for HOLD forms).
                if (!abilityKeyIsDown) {
                    abilityKeyIsDown = true;
                    ModMessages.sendToServer(new AbilityKeyC2SPacket(AbilityKeyC2SPacket.KeyState.PRESS));
                }
            } else {
                if (abilityKeyIsDown) {
                    abilityKeyIsDown = false;
                    ModMessages.sendToServer(new AbilityKeyC2SPacket(AbilityKeyC2SPacket.KeyState.RELEASE));
                }
            }
        }

        private static void openFormSelectionScreen() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }

            mc.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon() && demon.isBdaModeActive()) {
                    // Player is a DEMON and in BDA MODE
                    BloodDemonArts currentBda = demon.getEquippedBda();
                    if (currentBda != BloodDemonArts.NONE) {
                        List<IForm> availableForms = Arrays.stream(DemonForms.values())
                                .filter(form -> form.getStyle() == currentBda)
                                .collect(Collectors.toList());

                        mc.setScreen(new FormSelectionScreen(availableForms, true));
                    }
                } else {
                    // Player is a SLAYER, or a DEMON NOT in BDA mode
                    mc.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                        List<IForm> availableForms = style.getFormsForCurrentStyle()
                                .collect(Collectors.toList());

                        mc.setScreen(new FormSelectionScreen(availableForms, false));
                    });
                }
            });
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
        public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            Camera camera = event.getCamera();
            if (!(camera.getEntity() instanceof Player player)) return;

            player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
                GravityDirection direction = gravity.getGravity();
                if (direction == GravityDirection.DOWN) return;

                float partialTicks = (float) event.getPartialTick();
                float yawToUse;
                float pitchToUse;

                if (gravity.getTransitionProgress() >= 1.0f) {
                    yawToUse = gravity.getGravityYaw();
                    pitchToUse = gravity.getGravityPitch();
                } else {
                    // Use rotLerp to avoid 360-degree spins during transitions
                    yawToUse = Mth.rotLerp(partialTicks, gravity.getGravityYawOld(), gravity.getGravityYaw());
                    pitchToUse = Mth.lerp(partialTicks, gravity.getGravityPitchOld(), gravity.getGravityPitch());
                }

                float[] vanilla = GravityUtil.gravityLookToVanilla(
                    yawToUse,
                    pitchToUse,
                    direction
                );

                event.setYaw(vanilla[0]);
                event.setPitch(vanilla[1]);
                event.setRoll(vanilla[2]);
            });
        }

        @SubscribeEvent
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            event.getEntity().getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
                GravityDirection direction = gravity.getGravity();
                if (direction == GravityDirection.DOWN) {
                    return;
                }

                event.getPoseStack().pushPose();
                gravityPosePushed.add(event.getEntity().getId());
                applyGravityRotation(event.getPoseStack(), direction);
            });
        }

        @SubscribeEvent
        public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
            if (gravityPosePushed.remove(event.getEntity().getId())) {
                event.getPoseStack().popPose();
            }
        }

        private static void applyGravityRotation(com.mojang.blaze3d.vertex.PoseStack poseStack, GravityDirection direction) {
            // RenderPlayerEvent.Pre is already anchored at the entity origin. Rotating around that
            // origin keeps the gravity foot-point fixed; the previous height translations were
            // pushing the rotated model a full body-length away from the correct anchor.
            switch (direction) {
                case UP -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0f));
                case NORTH -> poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0f));
                case SOUTH -> poseStack.mulPose(Vector3f.XP.rotationDegrees(-90.0f));
                case WEST -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90.0f));
                case EAST -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0f));
                default -> {
                }
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
            event.register(KeyBinding.CHANGE_BACK_KEY);
            event.register(KeyBinding.USE_KEY);
            event.register(KeyBinding.OPEN_MENU_KEY);
            event.register(KeyBinding.BDA_TOGGLE_KEY);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "breath", BreathHudOverlay.HUD_BREATH);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "vignette", VignetteHudOverlay.HUD_VIGNETTE);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "red_static", RedStaticHudOverlay.HUD_RED_STATIC);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "pie_splat", PieSplatOverlay.HUD_PIE_SPLAT);
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
