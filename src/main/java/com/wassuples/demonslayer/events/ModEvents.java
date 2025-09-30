package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreath;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.command.DemonSlayerCommands;
import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.entity.custom.MotherEyeEntity;
import com.wassuples.demonslayer.entity.custom.WaterPrisonEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.missions.*;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.wassuples.demonslayer.ranking.PlayerProgressionProvider.PLAYER_PROGRESSION;
import static com.wassuples.demonslayer.util.CombatHelper.performDemonAttack;
import static com.wassuples.demonslayer.util.CombatHelper.performNichirinSwingEffect;

@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID)
public class ModEvents {
    private static final Map<UUID, CompoundTag> styleDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> progressionDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> breathDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> missionDeathData =new HashMap<>();
    private static final Map<UUID, CompoundTag> demonDeathData = new HashMap<>();

    private static final UUID ENHANCED_VIGOR_MODIFIER_ID = UUID.fromString("a7a2f5a2-a3a4-492c-a8e2-89255ac3a7e4");
    private static final UUID DEMONIC_STRENGTH_MODIFIER_ID = UUID.fromString("f3b1f1a0-75da-4c4b-b72e-c4e9c7a6e12e");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerBreathProvider.PLAYER_BREATH).isPresent()) {
                event.addCapability(PlayerBreathProvider.KEY, new PlayerBreathProvider());
            }
            if (!event.getObject().getCapability(PlayerStyleProvider.PLAYER_STYLE).isPresent()) {
                event.addCapability(PlayerStyleProvider.KEY, new PlayerStyleProvider());
            }
            if (!event.getObject().getCapability(PLAYER_PROGRESSION).isPresent()) {
                event.addCapability(PlayerProgressionProvider.KEY, new PlayerProgressionProvider());
            }
            if (!event.getObject().getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).isPresent()) {
                event.addCapability(PlayerMissionsProvider.KEY, new PlayerMissionsProvider());
            }
            if (!event.getObject().getCapability(PlayerDemonProvider.PLAYER_DEMON).isPresent()) {
                event.addCapability(PlayerDemonProvider.KEY, new PlayerDemonProvider());
            }
            if (!event.getObject().getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS).isPresent()) {
                event.addCapability(PlayerCooldownsProvider.KEY, new PlayerCooldownsProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // When a player dies, we save all their capability data to our maps.
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Save Style data
            serverPlayer.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                CompoundTag nbt = new CompoundTag();
                style.saveNBTData(nbt);
                styleDeathData.put(serverPlayer.getUUID(), nbt);
            });
            // Save Progression data
            serverPlayer.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> {
                CompoundTag nbt = new CompoundTag();
                progression.saveNBTData(nbt);
                progressionDeathData.put(serverPlayer.getUUID(), nbt);
            });
            // Save Breath data
            serverPlayer.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                CompoundTag nbt = new CompoundTag();
                breath.saveNBTData(nbt);
                breathDeathData.put(serverPlayer.getUUID(), nbt);
            });
            // Save Mission data
            serverPlayer.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(mission -> {
                CompoundTag nbt = new CompoundTag();
                mission.saveNBTData(nbt);
                missionDeathData.put(serverPlayer.getUUID(), nbt);
            });
            // Save Demon data
            serverPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                CompoundTag nbt = new CompoundTag();
                demon.saveNBTData(nbt);
                demonDeathData.put(serverPlayer.getUUID(), nbt);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // When a player respawns, we load their data back from our maps.
        if (event.getEntity() instanceof ServerPlayer player) {
            // Load Style data
            if (styleDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = styleDeathData.get(player.getUUID());
                player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> style.loadNBTData(nbt));
                styleDeathData.remove(player.getUUID());
            }
            // Load Progression data
            if (progressionDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = progressionDeathData.get(player.getUUID());
                player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> progression.loadNBTData(nbt));
                progressionDeathData.remove(player.getUUID());
            }
            // Load Breath data
            if (breathDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = breathDeathData.get(player.getUUID());
                player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> breath.loadNBTData(nbt));
                breathDeathData.remove(player.getUUID());
            }
            // Load Mission data
            if (missionDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = missionDeathData.get(player.getUUID());
                player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> missions.loadNBTData(nbt));
                missionDeathData.remove(player.getUUID());
            }
            // Load Demon data
            if (demonDeathData.containsKey(player.getUUID())) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> demon.loadNBTData(demonDeathData.get(player.getUUID())));
                demonDeathData.remove(player.getUUID());
            }

            // After loading the data on the server, sync it all to the client.
            syncAllDataToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // When a player logs in, sync all their server-side data to the client.
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllDataToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // When a player changes dimension, sync all their server-side data to the client.
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllDataToClient(player);
        }
    }

    private static void syncAllDataToClient(ServerPlayer player) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), player));

        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player));

        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(player.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), player));

        player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(mission -> {
            CompoundTag nbt = new CompoundTag();
            mission.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(nbt), player);
        });
        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            CompoundTag nbt = new CompoundTag();
            demon.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(nbt), player);
        });
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        // We leave this empty because the onPlayerDeath/Respawn system is more reliable with other mods.
        // If you had other capabilities that *do* work with Clone, you could leave their logic here.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerBreath.class);
        event.register(PlayerStyle.class);
        event.register(PlayerProgression.class);
        event.register(PlayerMissions.class);
        event.register(PlayerDemon.class);
        event.register(PlayerCooldowns.class);
    }

    @SubscribeEvent
    public static void onPlayerFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(ModItems.MUZANS_BLOOD.get())) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) {
                    demon.setDemon(true);
                    // Apply buffs that scale with rank
                    int rankOrdinal = demon.getRank().ordinal();
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
                    // Jump Boost: Starts at 0, increases to I then II for elite demons/moons
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, Math.min(rankOrdinal / 4, 2), false, false, true));
                    // Regeneration: Starts at 0, increases up to III for high-rank moons
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, Math.min(rankOrdinal / 3, 3), false, false, true));

                    if (player.level.isDay() && !player.level.isRaining() && player.level.canSeeSky(player.blockPosition()) && !player.isCreative() && !demon.isOvercomer()) {
                        if (player.getRemainingFireTicks() <= 10) {
                            player.setSecondsOnFire(8);
                        }
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.player instanceof ServerPlayer serverPlayer) {
            // --- SLAYER BREATH REGEN ---
            event.player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() < breath.maxBreath && serverPlayer.getRandom().nextFloat() < 0.025f) {
                    breath.addBreath(1);
                }

                if (serverPlayer.tickCount % 20 == 0) {
                    ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), serverPlayer);
                }
            });

            // --- DEMON PASSIVE HANDLING ---
            serverPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                // This handles attributes (Health/Damage), regeneration, and sun weakness/resistance
                handleDemonPassives(serverPlayer, demon);

                // This handles passives that only run when toggled on and at the end of the tick
                if (event.phase == TickEvent.Phase.END && demon.isDemon()) {
                    handleStalkersGait(serverPlayer, demon);
                    handleOverwhelmingAura(serverPlayer, demon);
                }
            });

            if (serverPlayer.tickCount % 20 == 0) { // Check once per second
                final ResourceLocation finalSelectionId = new ResourceLocation(DemonSlayer.MOD_ID, "story_final_selection");

                serverPlayer.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                    if (missions.isActive(finalSelectionId)) {
                        final ResourceLocation requiredBiome = new ResourceLocation(DemonSlayer.MOD_ID, "fujikasane_forest");
                        final String biomeEnteredKey = "fujikasane_entered";
                        final String bossSpawnedKey = "boss_spawned";

                        Map<String, Integer> progress = missions.getProgress(finalSelectionId);
                        ResourceLocation currentBiome = serverPlayer.level.getBiome(serverPlayer.blockPosition()).unwrapKey().get().location();

                        Mission mission = MissionRegistry.getMission(finalSelectionId);
                        if (mission == null) return;

                        if (currentBiome.equals(requiredBiome)) {
                            // --- Player is INSIDE the biome ---
                            if (!progress.containsKey(biomeEnteredKey)) {
                                missions.updateProgress(finalSelectionId, biomeEnteredKey, 1);
                                serverPlayer.sendSystemMessage(Component.literal("You have entered Mt. Fujikasane. The trial begins now. Do not leave.").withStyle(ChatFormatting.DARK_PURPLE));
                            }

                            if (!progress.containsKey(bossSpawnedKey)) {
                                int startTime = progress.getOrDefault("survive_objective", (int)serverPlayer.level.getGameTime());
                                long timeElapsed = serverPlayer.level.getGameTime() - startTime;

                                // Check if 6 days have passed
                                if (timeElapsed >= (24000 * 6)) {
                                    BlockPos spawnPos = serverPlayer.blockPosition().north(5);
                                    ModEntityTypes.HAND_DEMON.get().spawn((ServerLevel)serverPlayer.level, null, null, spawnPos, MobSpawnType.EVENT, true, false);
                                    serverPlayer.sendSystemMessage(Component.literal("A massive, grotesque demon emerges from the trees... It reeks of countless devoured slayers.").withStyle(ChatFormatting.DARK_RED));
                                    missions.updateProgress(finalSelectionId, bossSpawnedKey, 1);
                                }
                            }
                        } else {
                            // --- Player is OUTSIDE the biome ---
                            if (progress.containsKey(biomeEnteredKey)) {
                                missions.abandonMission(finalSelectionId);
                                serverPlayer.sendSystemMessage(Component.literal("You have fled Mt. Fujikasane. Your trial has ended in failure.").withStyle(ChatFormatting.RED));
                                serverPlayer.level.playSound(null, serverPlayer.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 1.0f);
                                ModUtils.syncMissions(serverPlayer);
                            }
                        }
                    }
                });
            }

            if (serverPlayer.tickCount % 40 == 0) {
                serverPlayer.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                    Set<ResourceLocation> activeMissionIds = new HashSet<>(missions.getActiveMissionIds());

                    activeMissionIds.forEach(missionId -> {
                        Mission mission = MissionRegistry.getMission(missionId);
                        if (mission != null) {
                            boolean allObjectivesComplete = true;
                            for (MissionObjective objective : mission.objectives()) {
                                if (!objective.isComplete(serverPlayer, missions.getProgress(missionId))) {
                                    allObjectivesComplete = false;
                                    break;
                                }
                            }

                            if (allObjectivesComplete) {
                                // Grant rewards and complete the mission
                                mission.rewards().forEach(reward -> reward.grant(serverPlayer));
                                missions.completeMission(mission);
                                serverPlayer.sendSystemMessage(Component.literal("Mission Complete: ").withStyle(ChatFormatting.GOLD).append(mission.title()));

                                // Sync the changes to the client
                                CompoundTag missionData = new CompoundTag();
                                missions.saveNBTData(missionData);
                                ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), serverPlayer);
                            }
                        }
                    });
                });
            }

            if (serverPlayer.tickCount % 4 == 0) {
                if (serverPlayer.hasEffect(ModEffects.FORCED_SLEEP.get())) return;
                if (serverPlayer.getInventory().getArmor(3).is(Items.CARVED_PUMPKIN)) return;

                for (Entity entity : ((ServerLevel) serverPlayer.level).getEntities().getAll()) {
                    if (entity instanceof MotherEyeEntity motherEye) {
                        if (serverPlayer.getUUID().equals(motherEye.getCasterUUID())) continue;
                        if (serverPlayer.distanceToSqr(motherEye) > 25 * 25) continue;

                        for (BlockPos eyePos : motherEye.getLesserEyePositions()) {
                            Vec3 playerLook = serverPlayer.getLookAngle();
                            Vec3 playerToEye = Vec3.atCenterOf(eyePos).subtract(serverPlayer.getEyePosition()).normalize();
                            double dot = playerLook.dot(playerToEye);

                            if (dot > 0.98) {
                                ClipContext ctx = new ClipContext(serverPlayer.getEyePosition(), Vec3.atCenterOf(eyePos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, serverPlayer);
                                BlockHitResult ray = serverPlayer.level.clip(ctx);

                                if (ray.getType() == HitResult.Type.MISS) {
                                    serverPlayer.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), 1200, 0));
                                    ForcedSleepEffect.setCaster(serverPlayer, (LivingEntity) motherEye.getCaster());

                                    List<Integer> keys = new ArrayList<>(List.of(0, 1, 2, 3)); // 0=up, 1=left, 2=down, 3=right
                                    Collections.shuffle(keys);
                                    ModMessages.sendToPlayer(new StartSleepQTES2CPacket(keys), serverPlayer);

                                    return;
                                }
                            }
                        }
                    }
                }
            }

            if (serverPlayer.isPassenger() && serverPlayer.getVehicle() instanceof WaterPrisonEntity) {
                serverPlayer.setAirSupply(0);
            }

            if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide && event.player instanceof ServerPlayer player) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    if (!demon.isDemon()) return;

                    handleStalkersGait(player, demon);
                    handleOverwhelmingAura(player, demon);
                });
            }
        }
    }

    private static void handleDemonPassives(ServerPlayer player, PlayerDemon demon) {
        // Guard clause for non-demons
        if (!demon.isDemon()) return;

        // --- ENHANCED VIGOR (Max Health) ---
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            // Always remove the modifier first to ensure it's updated correctly if the tier changes
            maxHealth.removeModifier(ENHANCED_VIGOR_MODIFIER_ID);
            if (demon.isDemon() && demon.isPassiveActive(DemonPassive.ENHANCED_VIGOR)) {
                int tier = demon.getPassiveTier(DemonPassive.ENHANCED_VIGOR);
                if (tier > 0) {
                    AttributeModifier modifier = new AttributeModifier(ENHANCED_VIGOR_MODIFIER_ID, "Enhanced Vigor", tier * 2.0, AttributeModifier.Operation.ADDITION);
                    maxHealth.addPermanentModifier(modifier);
                }
            }
        }

        // --- DEMONIC_STRENGTH (Melee Damage) ---
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(DEMONIC_STRENGTH_MODIFIER_ID);
            if (demon.isDemon() && demon.isPassiveActive(DemonPassive.DEMONIC_STRENGTH)) {
                int tier = demon.getPassiveTier(DemonPassive.DEMONIC_STRENGTH);
                if (tier > 0) {
                    AttributeModifier modifier = new AttributeModifier(DEMONIC_STRENGTH_MODIFIER_ID, "Demonic Strength", tier * 1.0, AttributeModifier.Operation.ADDITION);
                    attackDamage.addPermanentModifier(modifier);
                }
            }
        }

        // --- HYPER REGENERATION ---
        if (demon.isPassiveActive(DemonPassive.HYPER_REGENERATION)) {
            int tier = demon.getPassiveTier(DemonPassive.HYPER_REGENERATION);
            if (tier > 0) {
                // Apply Regeneration effect. Amplifier is tier-1 (Tier 1 = Regen I)
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, tier - 1, true, false, true));
            }
        }

        // --- SUNLIGHT WEAKNESS & RESISTANCE ---
        boolean isInSun = player.level.isDay() && !player.level.isRaining() && player.level.canSeeSky(player.blockPosition());
        if (isInSun) {
            if (demon.isPassiveActive(DemonPassive.SUNLIGHT_RESISTANCE)) {
                int tier = demon.getPassiveTier(DemonPassive.SUNLIGHT_RESISTANCE);
                int maxGraceTicks = tier * 4 * 20; // 4 seconds of grace time per tier

                if (demon.getSunGraceTicks() < maxGraceTicks) {
                    demon.incrementSunGraceTicks();
                    return; // Exit before the burning logic is applied
                }
            }
            // either the passive is off, or the grace period has expired
            if (player.getRemainingFireTicks() <= 1) {
                player.setSecondsOnFire(8);
            }
        } else {
            // If not in the sun, reset the grace period timer
            demon.resetSunGraceTicks();
        }
    }

    private static void handleStalkersGait(ServerPlayer player, PlayerDemon demon) {
        if (demon.getPassiveTier(DemonPassive.STALKERS_GAIT) > 0 && demon.isPassiveActive(DemonPassive.STALKERS_GAIT)) {
            player.setSilent(true);
        } else {
            if (player.isSilent()) {
                player.setSilent(false);
            }
        }
    }

    private static void handleOverwhelmingAura(ServerPlayer player, PlayerDemon demon) {
        int auraTier = demon.getPassiveTier(DemonPassive.OVERWHELMING_AURA);
        if (auraTier <= 0 || !demon.isPassiveActive(DemonPassive.OVERWHELMING_AURA)) {
            demon.setGazeTarget(null);
            return;
        }

        //(1.5s -> 1.25s -> 1s)
        int requiredTicks = 30 - ((auraTier - 1) * 5);

        HitResult hitResult = player.pick(20, 0, false);
        UUID currentTargetId = null;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) hitResult).getEntity();
            if (targetEntity instanceof LivingEntity && targetEntity != player) {
                currentTargetId = targetEntity.getUUID();
            }
        }

        demon.setGazeTarget(currentTargetId);

        if (currentTargetId != null && demon.getGazeTargetId() != null) {
            demon.incrementGazeTicks();
            if (demon.getGazeTicks() == requiredTicks) {
                Entity target = player.getServer().getLevel(player.level.dimension()).getEntity(currentTargetId);
                if (target instanceof LivingEntity livingTarget) {
                    int effectDuration = 40 + (auraTier * 20); // 2s -> 3s -> 4s
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration, 1));
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration, 2));
                    demon.incrementGazeTicks();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        DemonSlayerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof HashiraEntity) {
            event.setCanceled(true);
        }

        if (event.getEntity() instanceof Player player && !player.level.isClientSide) {
            boolean isHoldingSword = player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                    player.getOffhandItem().getItem() instanceof NichirinSwordItem;

            if (isHoldingSword) {
                event.setCanceled(true);
                player.fallDistance = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();

        if (target instanceof Player player && player.isPassenger() && player.getVehicle() instanceof MotherEyeEntity) {
            event.setCanceled(true);
            return;
        }

        if (target.hasEffect(ModEffects.FORCED_SLEEP.get())) {
            UUID casterUUID = ForcedSleepEffect.getCasterUUID(target);
            Entity attacker = event.getSource().getEntity();

            if (attacker != null && !attacker.getUUID().equals(casterUUID)) {
                target.removeEffect(ModEffects.FORCED_SLEEP.get());
                target.level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            event.setCanceled(true);
            return;
        }

        ActiveForm activeForm = ActiveFormManager.getActiveForm(target);
        if (activeForm != null) {
            activeForm.onHurt(event);
            if (activeForm.getForm() == DemonForms.DESTRUCTIVE_DEATH_1 && !event.isCanceled()) {
                if (target.level.random.nextFloat() < 0.5f) {
                    event.setCanceled(true);
                    target.level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 2.0f);
                    if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                        attacker.hurt(DamageSource.thorns(target), 4.0f);
                    }
                }
            }
        }

        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon() || !demon.isPassiveActive(DemonPassive.TOUGHENED_HIDE)) {
                return;
            }

            int tier = demon.getPassiveTier(DemonPassive.TOUGHENED_HIDE);
            if (tier > 0) {
                float reductionMultiplier = 1.0f - (tier * 0.04f); // 4% reduction per tier
                float newDamage = event.getAmount() * reductionMultiplier;
                event.setAmount(newDamage);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            final ResourceLocation finalSelectionId = new ResourceLocation(DemonSlayer.MOD_ID, "story_final_selection");

            player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                if (missions.isActive(finalSelectionId)) {
                    if (event.getEntity().getType() == ModEntityTypes.HAND_DEMON.get()) {
                        missions.updateProgress(finalSelectionId, "boss_defeated", 1);
                        player.sendSystemMessage(Component.literal("You have vanquished the Hand Demon! Now, you must only survive the remaining time.").withStyle(ChatFormatting.GOLD));
                        ModUtils.syncMissions(player);
                    }
                }
            });
        }

        if (deadEntity instanceof Player && deadEntity.getTags().contains("mist_clone")) {
            if (deadEntity.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, deadEntity.getX(), deadEntity.getY(), deadEntity.getZ(),
                        30, 0.5, 0.5, 0.5, 0.1);
            }
            deadEntity.level.playSound(null, deadEntity.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.8f, 1.5f);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon()) {
                    LivingEntity killedEntity = event.getEntity();
                    int points = 0;
                    if (killedEntity instanceof HashiraEntity hashira) {
                        // Grant bonus points for killing a powerful enemy
                        points = 50 + (hashira.getBreathingStyle().ordinal() * 5);
                    } else if (killedEntity instanceof Player killedPlayer) {
                        // Check if the killed player was a Demon Slayer
                        killedPlayer.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(slayerProg -> {
                            // Grant points based on the Slayer's rank
                            demon.addBloodPoints(10 + slayerProg.getRank().ordinal() * 5);
                        });
                    }

                    if (points > 0) {
                        demon.addBloodPoints(points);
                        player.sendSystemMessage(Component.literal("You have consumed their essence. Gained " + points + " blood points.").withStyle(ChatFormatting.DARK_RED), true);
                        // Sync changes to the client
                        CompoundTag nbt = new CompoundTag();
                        demon.saveNBTData(nbt);
                        ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(nbt), player);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level.isClientSide() || !(event.getTarget() instanceof LivingEntity)) {
            return;
        }

        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(bda -> {
                    if (bda.getEquippedBda() == BloodDemonArts.DESTRUCTIVE_DEATH) {
                        ItemStack heldItem = player.getMainHandItem();
                        if (heldItem.isEmpty()) { // Unarmed
                            event.getTarget().hurt(DamageSource.playerAttack(player), 8.0f);
                        } else { // Armed
                            event.setCanceled(true);
                            event.getTarget().hurt(DamageSource.playerAttack(player), 1.0f);
                            if (player instanceof ServerPlayer sp) {
                                sp.sendSystemMessage(Component.literal("Your movements with a weapon are clumsy.").withStyle(ChatFormatting.RED), true);
                            }
                        }
                    }
                });
            }
        });

        // --- Standard Swing Effects & Mastery Gain ---
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getMainHandItem().getItem() instanceof NichirinSwordItem) {
                performNichirinSwingEffect(serverPlayer);
            }

            // Check if the player is a demon and is performing a melee attack
            serverPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon()) {
                    performDemonAttack(serverPlayer);
                }
            });

            // --- Mastery Gain Logic ---
            serverPlayer.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> serverPlayer.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                BreathingStyles currentStyle = style.getCurrentStyles();
                progression.addStyleMastery(currentStyle, 1);
                ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(
                                player.getId(),
                                progression.getRank(),
                                progression.getUnlockedStyles(),
                                progression.getStyleMasteryMap(),
                                progression.getFormMasteryMap()),
                        serverPlayer
                );
            }));
        }
    }

    public static void syncProgression(ServerPlayer player) {
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> ModMessages.sendToClients(new PlayerProgressionSyncS2CPacket(
                player.getId(),
                progression.getRank(),
                progression.getUnlockedStyles(),
                progression.getStyleMasteryMap(),
                progression.getFormMasteryMap()
        )));
    }
}