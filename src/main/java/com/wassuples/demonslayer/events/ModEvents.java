package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.command.DemonSlayerCommands;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.crow.CrowManager;
import com.wassuples.demonslayer.crow.PlayerCrowProvider;
import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import com.wassuples.demonslayer.item.custom.HashiraSwordHelper;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionObjective;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.reputation.ReputationService;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.util.DemonThreatHelper;
import com.wassuples.demonslayer.util.ModUtils;
import com.wassuples.demonslayer.util.ToonForceHelper;
import com.wassuples.demonslayer.util.WisteriaHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.wassuples.demonslayer.ranking.PlayerProgressionProvider.PLAYER_PROGRESSION;
import static com.wassuples.demonslayer.util.CombatHelper.performDemonAttack;
import static com.wassuples.demonslayer.util.CombatHelper.performNichirinSwingEffect;

/**
 * Server-side Forge event handler for the Demon Slayer mod.
 *
 * <p>Subscribes to the Forge event bus and handles:
 * <ul>
 *   <li><b>Capability attachment</b> — attaches all player capabilities (breath, style, demon, progression, cooldowns, crow, missions)</li>
 *   <li><b>Player lifecycle</b> — clones capabilities on respawn, syncs state on login/dimension change</li>
 *   <li><b>Combat events</b> — demon sun damage, wisteria poison, form hurt delegation, passive ability effects</li>
 *   <li><b>Entity events</b> — demon kill rewards (Blood Points), mission objective tracking</li>
 *   <li><b>Command registration</b> — registers {@code /demonslayer} commands</li>
 * </ul>
 *
 * @see ClientEvents
 * @see com.wassuples.demonslayer.command.DemonSlayerCommands
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID)
public class ModEvents {
    private static final Map<UUID, CompoundTag> styleDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> progressionDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> breathDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> missionDeathData =new HashMap<>();
    private static final Map<UUID, CompoundTag> demonDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> crowDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> cooldownDeathData = new HashMap<>();
    private static final Map<UUID, CompoundTag> gravityDeathData = new HashMap<>();

    private static final UUID ENHANCED_VIGOR_MODIFIER_ID = UUID.fromString("a7a2f5a2-a3a4-492c-a8e2-89255ac3a7e4");
    private static final UUID DEMONIC_STRENGTH_MODIFIER_ID = UUID.fromString("f3b1f1a0-75da-4c4b-b72e-c4e9c7a6e12e");

    private static final String DEMON_THREAT_TAG = "ds_threat_until";
    private static final int THREAT_TIMEOUT_TICKS = 60; // ~3 seconds after last relevant hit

    private static final int WISTERIA_SCAN_RADIUS = 4;
    private static final int WISTERIA_POISON_DURATION_TICKS = 60; // refreshes while in range
    private static final int WISTERIA_POISON_AMPLIFIER = 4;

    private static final String TAMAYO_COOLDOWN_TAG = "demonslayer:tamayo_offer_cooldown";
    private static final String TAMAYO_COOLDOWN_UNTIL_PREFIX = "demonslayer:tamayo_offer_until:";
    private static final int TAMAYO_SCAN_INTERVAL_TICKS = 20; // 1s
    private static final int TAMAYO_COOLDOWN_TICKS = 20 * 60; // 60s
    private static final double TAMAYO_SCAN_RADIUS = 8.0D;
    private static final float TAMAYO_CRITICAL_HEALTH = 1.0f; // half a heart

    private static void startTamayoCooldown(ServerPlayer player) {
        player.getTags().removeIf(t -> t.startsWith(TAMAYO_COOLDOWN_UNTIL_PREFIX));
        player.addTag(TAMAYO_COOLDOWN_TAG);
        long until = player.level.getGameTime() + TAMAYO_COOLDOWN_TICKS;
        player.addTag(TAMAYO_COOLDOWN_UNTIL_PREFIX + until);
    }

    private static boolean isTamayoOnCooldown(ServerPlayer player) {
        if (!player.getTags().contains(TAMAYO_COOLDOWN_TAG)) return false;
        String untilTag = player.getTags().stream().filter(t -> t.startsWith(TAMAYO_COOLDOWN_UNTIL_PREFIX)).findFirst().orElse(null);
        if (untilTag == null) return true;
        try {
            long until = Long.parseLong(untilTag.substring(TAMAYO_COOLDOWN_UNTIL_PREFIX.length()));
            return player.level.getGameTime() < until;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static void tickTamayoCooldown(ServerPlayer player) {
        if (!player.getTags().contains(TAMAYO_COOLDOWN_TAG)) return;
        String untilTag = player.getTags().stream().filter(t -> t.startsWith(TAMAYO_COOLDOWN_UNTIL_PREFIX)).findFirst().orElse(null);
        if (untilTag == null) return;
        try {
            long until = Long.parseLong(untilTag.substring(TAMAYO_COOLDOWN_UNTIL_PREFIX.length()));
            if (player.level.getGameTime() >= until) {
                player.getTags().remove(TAMAYO_COOLDOWN_TAG);
                player.getTags().remove(untilTag);
            }
        } catch (NumberFormatException ignored) {
            player.getTags().remove(TAMAYO_COOLDOWN_TAG);
            player.getTags().remove(untilTag);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(PlayerBreathProvider.PLAYER_BREATH).isPresent()) {
                event.addCapability(PlayerBreathProvider.KEY, new PlayerBreathProvider());
            }

            if (!player.getCapability(PlayerStyleProvider.PLAYER_STYLE).isPresent()) {
                event.addCapability(PlayerStyleProvider.KEY, new PlayerStyleProvider());
            }

            if (!player.getCapability(PLAYER_PROGRESSION).isPresent()) {
                event.addCapability(PlayerProgressionProvider.KEY, new PlayerProgressionProvider());
            }

            if (!player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).isPresent()) {
                event.addCapability(PlayerMissionsProvider.KEY, new PlayerMissionsProvider());
            }

            if (!player.getCapability(PlayerDemonProvider.PLAYER_DEMON).isPresent()) {
                event.addCapability(PlayerDemonProvider.KEY, new PlayerDemonProvider());
            }

            if (!player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS).isPresent()) {
                event.addCapability(PlayerCooldownsProvider.KEY, new PlayerCooldownsProvider());
            }

            if (!player.getCapability(PlayerCrowProvider.PLAYER_CROW).isPresent()) {
                event.addCapability(PlayerCrowProvider.KEY, new PlayerCrowProvider());
            }

            if (!player.getCapability((PlayerGravity.GRAVITY_CAPABILITY)).isPresent()) {
                event.addCapability(PlayerGravity.GRAVITY_RESOURCE, new PlayerGravity());
            }
        }

        // Attach DemonTrait capability to NemesisDemonEntity instances
        if (event.getObject() instanceof com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity) {
            event.addCapability(
                    new ResourceLocation(DemonSlayer.MOD_ID, "demon_trait"),
                    new com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // When a player dies, we save all their capability data to our maps.
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Stop any active form so it doesn't persist across death/respawn
            ActiveFormManager.stop(serverPlayer);

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
            // Save Crow data
            serverPlayer.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(crow -> {
                CompoundTag nbt = new CompoundTag();
                crow.saveNBTData(nbt);
                crowDeathData.put(serverPlayer.getUUID(), nbt);
            });

            // Save Cooldown data
            serverPlayer.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS).ifPresent(cooldowns -> {
                CompoundTag nbt = new CompoundTag();
                // store remaining ticks relative to current level time
                cooldowns.saveNBTData(serverPlayer.level, nbt);
                cooldownDeathData.put(serverPlayer.getUUID(), nbt);
            });

            // Save Gravity data
            serverPlayer.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
                CompoundTag nbt = new CompoundTag();
                gravity.saveNBTData(nbt);
                gravityDeathData.put(serverPlayer.getUUID(), nbt);
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
            // Load Crow data
            if (crowDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = crowDeathData.get(player.getUUID());
                player.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(data -> data.loadNBTData(nbt));
                crowDeathData.remove(player.getUUID());
            }

            // Load Cooldown data
            if (cooldownDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = cooldownDeathData.get(player.getUUID());
                player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS)
                        .ifPresent(cooldowns -> cooldowns.loadNBTData(player.level, nbt));
                cooldownDeathData.remove(player.getUUID());
            }

            // Load Gravity data
            if (gravityDeathData.containsKey(player.getUUID())) {
                CompoundTag nbt = gravityDeathData.get(player.getUUID());
                player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> gravity.loadNBTData(nbt));
                gravityDeathData.remove(player.getUUID());
            }

            // After loading the data on the server, sync it all to the client.
            syncAllDataToClient(player);
            CrowManager.ensureCrowFor(player);

            // Apply rank stat bonuses (max breath, max health) so they persist across death
            player.getCapability(PLAYER_PROGRESSION).ifPresent(progression ->
                    ModUtils.applyRankStatBonuses(player, progression.getRank()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // When a player logs in, sync all their server-side data to the client.
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllDataToClient(player);
            CrowManager.ensureCrowFor(player);

            // Apply rank stat bonuses (max breath, max health) so they persist across sessions
            player.getCapability(PLAYER_PROGRESSION).ifPresent(progression ->
                    ModUtils.applyRankStatBonuses(player, progression.getRank()));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // When a player changes dimension, sync all their server-side data to the client.
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllDataToClient(player);
            CrowManager.ensureCrowFor(player);
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

        player.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(crow -> {
            CompoundTag nbt = new CompoundTag();
            crow.saveNBTData(nbt);
            // optionally send packet later
        });

        ModUtils.syncGravity(player);
    }

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CrowManager.handlePlayerSleep(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;

        // CUSTOM GRAVITY PHYSICS HANDLING
        player.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection direction = GravityUtil.getEffectiveGravity(player);
            boolean isCustomGravity  = direction != GravityDirection.DOWN
                && !player.isPassenger() && !player.isSpectator();

            if (event.phase == TickEvent.Phase.START) {
                GravityUtil.refreshGravityHitbox(player, direction);
                // Player.travel temporarily converts velocity to the gravity-local frame,
                // so vanilla applies its Y gravity before movement returns to world space.
                if (isCustomGravity && player.isNoGravity()) player.setNoGravity(false);
                return;
            }

            if (!isCustomGravity) return;
            gravity.tickInterpolation();
            gravity.tickTransientState();
            player.fallDistance = 0.0F;
        });

        if (event.phase != TickEvent.Phase.END) return;
        CrowManager.ensureCrowFor(player);

        ToonForceHelper.tickHotPotatoBomb(player);
        ToonForceHelper.tickFlattened(player);
        ToonForceHelper.tickBananaSpin(player);

        // WISTERIA: demon players are poisoned near wisteria leaves.
        if (player.tickCount % 10 == 0) {
            applyWisteriaPoisonIfNear(player);
        }

        // Demon threat expiry (runs server-side, cheap O(1))
        String tag = player.getTags().stream().filter(t -> t.startsWith(DEMON_THREAT_TAG + ":")).findFirst().orElse(null);
        if (tag != null) {
            try {
                long until = Long.parseLong(tag.substring((DEMON_THREAT_TAG + ":").length()));
                if (player.level.getGameTime() >= until) {
                    ModMessages.sendToPlayer(new DemonThreatS2CPacket(0.0f), player);
                    player.getTags().remove(tag);
                }
            } catch (NumberFormatException ignored) {
                player.getTags().remove(tag);
            }
        }

        // SLAYER BREATH REGEN
        event.player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            if (breath.getBreath() < breath.maxBreath && player.getRandom().nextFloat() < 0.025f) {
                breath.addBreath(1);
            }

            if (player.tickCount % 20 == 0) {
                ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player);
            }
        });

        // DEMON PASSIVE HANDLING
        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            // This handles attributes (Health/Damage), regeneration, and sun weakness/resistance
            handleDemonPassives(player, demon);

            // This handles passives that only run when toggled on and at the end of the tick
            if (event.phase == TickEvent.Phase.END && demon.isDemon()) {
                handleStalkersGait(player, demon);
                handleOverwhelmingAura(player, demon);
            }

            handleToonForceAirwalk(player, demon);
        });

        if (player.tickCount % 20 == 0) { // Check once per second
            final ResourceLocation finalSelectionId = new ResourceLocation(DemonSlayer.MOD_ID, "story_final_selection");

            player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                if (missions.isActive(finalSelectionId)) {
                    final ResourceLocation requiredBiome = new ResourceLocation(DemonSlayer.MOD_ID, "fujikasane_forest");
                    final String biomeEnteredKey = "fujikasane_entered";
                    final String bossSpawnedKey = "boss_spawned";

                    Map<String, Integer> progress = missions.getProgress(finalSelectionId);
                    ResourceLocation currentBiome = player.level.getBiome(player.blockPosition()).unwrapKey().get().location();

                    Mission mission = MissionRegistry.getMission(finalSelectionId);
                    if (mission == null) return;

                    if (currentBiome.equals(requiredBiome)) {
                        // --- Player is INSIDE the biome ---
                        if (!progress.containsKey(biomeEnteredKey)) {
                            missions.updateProgress(finalSelectionId, biomeEnteredKey, 1);
                            player.sendSystemMessage(Component.literal("You have entered Mt. Fujikasane. The trial begins now. Do not leave.").withStyle(ChatFormatting.DARK_PURPLE));
                        }

                        if (!progress.containsKey(bossSpawnedKey)) {
                            int startTime = progress.getOrDefault("survive_objective", (int)player.level.getGameTime());
                            long timeElapsed = player.level.getGameTime() - startTime;

                            // Check if 6 days have passed
                            if (timeElapsed >= (24000 * 6)) {
                                BlockPos spawnPos = player.blockPosition().north(5);
                                ModEntityTypes.HAND_DEMON.get().spawn((ServerLevel)player.level, null, null, spawnPos, MobSpawnType.EVENT, true, false);
                                player.sendSystemMessage(Component.literal("A massive, grotesque demon emerges from the trees... It reeks of countless devoured slayers.").withStyle(ChatFormatting.DARK_RED));
                                missions.updateProgress(finalSelectionId, bossSpawnedKey, 1);
                            }
                        }
                    } else {
                        // --- Player is OUTSIDE the biome ---
                        if (progress.containsKey(biomeEnteredKey)) {
                            missions.abandonMission(finalSelectionId);
                            player.sendSystemMessage(Component.literal("You have fled Mt. Fujikasane. Your trial has ended in failure.").withStyle(ChatFormatting.RED));
                            player.level.playSound(null, player.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 1.0f);
                            ModUtils.syncMissions(player);
                        }
                    }
                }
            });
        }

        if (player.tickCount % 40 == 0) {
            player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                Set<ResourceLocation> activeMissionIds = new HashSet<>(missions.getActiveMissionIds());

                activeMissionIds.forEach(missionId -> {
                    Mission mission = MissionRegistry.getMission(missionId);
                    if (mission != null) {
                        boolean allObjectivesComplete = true;
                        for (MissionObjective objective : mission.objectives()) {
                            if (!objective.isComplete(player, missions.getProgress(missionId))) {
                                allObjectivesComplete = false;
                                break;
                            }
                        }

                        if (allObjectivesComplete) {
                            // Grant rewards and complete the mission
                            mission.rewards().forEach(reward -> reward.grant(player));
                            missions.completeMission(mission);
                            ReputationService.onMissionCompleted(player, mission);
                            ModUtils.checkAndApplyRankUp(player);
                            player.sendSystemMessage(Component.literal("Mission Complete: ").withStyle(ChatFormatting.GOLD).append(mission.title()));

                            // Sync the changes to the client
                            CompoundTag missionData = new CompoundTag();
                            missions.saveNBTData(missionData);
                            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), player);
                        }
                    }
                });
            });
        }

        if (player.tickCount % 4 == 0) {
            if (player.hasEffect(ModEffects.FORCED_SLEEP.get())) return;
            if (player.getInventory().getArmor(3).is(Items.CARVED_PUMPKIN)) return;

            for (Entity entity : ((ServerLevel) player.level).getEntities().getAll()) {
                if (entity instanceof MotherEyeEntity motherEye) {
                    if (player.getUUID().equals(motherEye.getCasterUUID())) continue;
                    if (player.distanceToSqr(motherEye) > 25 * 25) continue;

                    for (BlockPos eyePos : motherEye.getLesserEyePositions()) {
                        Vec3 playerLook = player.getLookAngle();
                        Vec3 playerToEye = Vec3.atCenterOf(eyePos).subtract(player.getEyePosition()).normalize();
                        double dot = playerLook.dot(playerToEye);

                        if (dot > 0.98) {
                            ClipContext ctx = new ClipContext(player.getEyePosition(), Vec3.atCenterOf(eyePos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
                            BlockHitResult ray = player.level.clip(ctx);

                            if (ray.getType() == HitResult.Type.MISS) {
                                player.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), 1200, 0));
                                ForcedSleepEffect.setCaster(player, (LivingEntity) motherEye.getCaster());

                                List<Integer> keys = new ArrayList<>(List.of(0, 1, 2, 3)); // 0=up, 1=left, 2=down, 3=right
                                Collections.shuffle(keys);
                                ModMessages.sendToPlayer(new StartSleepQTES2CPacket(keys), player);

                                return;
                            }
                        }
                    }
                }
            }
        }

        if (player.isPassenger() && player.getVehicle() instanceof WaterPrisonEntity) {
            player.setAirSupply(0);
        }

        if (!event.player.level.isClientSide) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) return;

                handleStalkersGait(player, demon);
                handleOverwhelmingAura(player, demon);
            });
        }

        // Tamayo offer cooldown expiry (cheap)
        if (event.phase == TickEvent.Phase.END) {
            tickTamayoCooldown(player);
        }

        // Tamayo offer trigger: extremely low health + nearby + not demon + not on cooldown
        if (player.tickCount % TAMAYO_SCAN_INTERVAL_TICKS == 0) {
            if (player.isAlive() && !player.isCreative() && player.getHealth() <= TAMAYO_CRITICAL_HEALTH) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    if (demon.isDemon()) return;
                    if (isTamayoOnCooldown(player)) return;

                    var box = player.getBoundingBox().inflate(TAMAYO_SCAN_RADIUS);
                    TamayoEntity tamayo = player.level.getEntitiesOfClass(TamayoEntity.class, box)
                            .stream()
                            .findFirst()
                            .orElse(null);
                    if (tamayo == null) return;

                    // Start cooldown immediately to prevent UI spam.
                    startTamayoCooldown(player);

                    java.util.List<net.minecraft.network.chat.Component> pages = java.util.List.of(
                            net.minecraft.network.chat.Component.literal("Wait... You're bleeding out. Please, hold still. I'm Tamayo.\n\nI can stabilize you — but what I offer isn't ordinary medicine."),
                            net.minecraft.network.chat.Component.literal("I developed a transformation process independent of Muzan's direct blood creation.\n\nIt will turn you into a demon."),
                            net.minecraft.network.chat.Component.literal("If you accept: your body will change. You'll gain strength and regeneration.\n\nBut you will also develop intense cravings, and sunlight will become lethal unless you later overcome it."),
                            net.minecraft.network.chat.Component.literal("This is not a cure. It's a different kind of life — and it may cost you everything you value.\n\nI won't force this on you."),
                            net.minecraft.network.chat.Component.literal("If you truly understand the risks and still want to live... say so.\n\nDo you consent to the transformation?")
                    );

                    ModMessages.sendToPlayer(new OpenTamayoOfferS2CPacket(tamayo.getId(), pages), player);
                });
            }
        }
    }



    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level.isClientSide()) return;
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof Player) return;

        ToonForceHelper.tickHotPotatoBomb(livingEntity);
        ToonForceHelper.tickFlattened(livingEntity);
        ToonForceHelper.tickToonText(livingEntity);

        // Poison demon NPCs close to wisteria leaves.
        if (livingEntity.tickCount % 10 != 0) return;
        applyWisteriaPoisonIfNear(livingEntity);
    }

    private static void applyWisteriaPoisonIfNear(LivingEntity livingEntity) {
        if (!isWisteriaSusceptibleDemon(livingEntity)) return;
        if (!WisteriaHelper.isWisteriaLeavesNearby(livingEntity.level, livingEntity.blockPosition(), WISTERIA_SCAN_RADIUS)) return;

        livingEntity.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), WISTERIA_POISON_DURATION_TICKS, WISTERIA_POISON_AMPLIFIER, true, true));
    }

    private static boolean isWisteriaSusceptibleDemon(LivingEntity livingEntity) {
        if (livingEntity instanceof DemonEntity || livingEntity instanceof HandDemonEntity || livingEntity instanceof NemesisDemonEntity) {
            return true;
        }

        if (livingEntity instanceof Player player) {
            return player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(PlayerDemon::isDemon)
                    .orElse(false);
        }

        return false;
    }

    private static void handleDemonPassives(ServerPlayer player, PlayerDemon demon) {
        // Always remove our modifiers first so turning demon->human or resetting passives actually reverts stats.
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(ENHANCED_VIGOR_MODIFIER_ID);
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(DEMONIC_STRENGTH_MODIFIER_ID);
        }

        // If not a demon anymore, we're done (attributes have been cleaned up).
        if (!demon.isDemon()) {
            return;
        }

        // --- ENHANCED VIGOR (Max Health) ---
        if (maxHealth != null) {
            if (demon.isPassiveActive(DemonPassive.ENHANCED_VIGOR)) {
                int tier = demon.getPassiveTier(DemonPassive.ENHANCED_VIGOR);
                if (tier > 0) {
                    AttributeModifier modifier = new AttributeModifier(ENHANCED_VIGOR_MODIFIER_ID, "Enhanced Vigor", tier * 2.0, AttributeModifier.Operation.ADDITION);
                    maxHealth.addPermanentModifier(modifier);
                }
            }
        }

        // --- DEMONIC_STRENGTH (Melee Damage) ---
        if (attackDamage != null) {
            if (demon.isPassiveActive(DemonPassive.DEMONIC_STRENGTH)) {
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
        boolean isInSun = player.level.isDay() && player.level.canSeeSky(player.blockPosition());
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

            // Rain puts out flames, but it should not negate sunlight lethality for demons.
            if (player.level.isRainingAt(player.blockPosition()) && player.tickCount % 20 == 0) {
                player.hurt(DamageSource.ON_FIRE, 1.0F);
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

    private static void handleToonForceAirwalk(ServerPlayer player, PlayerDemon demon) {
        if (!demon.isDemon() || demon.getEquippedBda() != BloodDemonArts.TOON_FORCE) {
            player.getPersistentData().remove(ToonForceHelper.AIRWALK_TAG);
            player.getPersistentData().remove("ds_toon_airwalk_ground_y");
            return;
        }

        if (player.isOnGround()) {
            player.getPersistentData().putDouble("ds_toon_airwalk_ground_y", player.getY());
            player.getPersistentData().remove(ToonForceHelper.AIRWALK_TAG);
            return;
        }

        float pitch = player.getXRot();
        boolean lookingDown = pitch > 50.0f; // Slightly more generous
        if (lookingDown) {
            player.getPersistentData().remove(ToonForceHelper.AIRWALK_TAG);
            player.getPersistentData().remove("ds_toon_airwalk_ground_y");
            return;
        }

        if (player.getPersistentData().contains("ds_toon_airwalk_ground_y")) {
            double groundY = player.getPersistentData().getDouble("ds_toon_airwalk_ground_y");
            if (player.getY() >= groundY - 0.65 && player.getY() <= groundY + 0.2) {
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, 0.0, vel.z);
                player.setPos(player.getX(), groundY, player.getZ());
                player.setOnGround(true);
                player.fallDistance = 0.0f;
                player.getPersistentData().putBoolean(ToonForceHelper.AIRWALK_TAG, true);
                if (player.tickCount % 8 == 0) {
                    ((ServerLevel) player.level).sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            player.getX(), groundY - 0.05, player.getZ(), 2, 0.2, 0.02, 0.2, 0.0);
                }
            } else if (player.getY() < groundY - 0.1) {
                player.getPersistentData().remove("ds_toon_airwalk_ground_y");
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

        Entity attackerEntity = event.getSource().getEntity();
        if (attackerEntity instanceof LivingEntity attacker && target.isAlive()) {
            if (ToonForceHelper.hasHotPotatoBomb(attacker)) {
                ToonForceHelper.transferHotPotatoBomb(attacker, target);
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

        // === Demon intimidation effects (server-authoritative) ===
        if (!target.level.isClientSide) {
            var sourceEntity = event.getSource().getEntity();
            if (sourceEntity instanceof LivingEntity attacker) {
                // We only care about demon players as the "receiving" side, and demon entities or demon players as the opponent.
                if (target instanceof ServerPlayer targetPlayer) {
                    targetPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(targetDemon -> {
                        if (!targetDemon.isDemon()) return;

                        float targetStrength = DemonThreatHelper.getDemonStrengthScore(targetDemon);

                        // Determine opponent strength.
                        float opponentStrength = 0.0f;
                        if (attacker instanceof ServerPlayer attackerPlayer) {
                            attackerPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(attackerDemon -> {
                                // only demon-vs-demon
                                if (!attackerDemon.isDemon()) return;
                                float opp = DemonThreatHelper.getDemonStrengthScore(attackerDemon);
                                float intensity = DemonThreatHelper.computeThreatIntensity(targetStrength, opp);
                                sendThreatIfRelevant(targetPlayer, intensity);
                            });
                        } else {
                            // Mob demon types: approximate using their max health + attack damage if present.
                            double maxHp = attacker.getMaxHealth();
                            double atk = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                                    ? attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue()
                                    : 2.0;
                            opponentStrength = (float) (maxHp * 25.0 + atk * 60.0);
                            float intensity = DemonThreatHelper.computeThreatIntensity(targetStrength, opponentStrength);
                            sendThreatIfRelevant(targetPlayer, intensity);
                        }
                    });
                }

                // Also if a demon player is the attacker hitting a stronger demon player/mob, apply to the attacker too.
                if (sourceEntity instanceof ServerPlayer attackerPlayer) {
                    attackerPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(attackerDemon -> {
                        if (!attackerDemon.isDemon()) return;

                        float attackerStrength = DemonThreatHelper.getDemonStrengthScore(attackerDemon);

                        if (target instanceof ServerPlayer targetPlayer) {
                            targetPlayer.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(targetDemon -> {
                                if (!targetDemon.isDemon()) return;
                                float opp = DemonThreatHelper.getDemonStrengthScore(targetDemon);
                                float intensity = DemonThreatHelper.computeThreatIntensity(attackerStrength, opp);
                                sendThreatIfRelevant(attackerPlayer, intensity);
                            });
                        } else {
                            double maxHp = target.getMaxHealth();
                            double atk = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                                    ? target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue()
                                    : 2.0;
                            float opp = (float) (maxHp * 25.0 + atk * 60.0);
                            float intensity = DemonThreatHelper.computeThreatIntensity(attackerStrength, opp);
                            sendThreatIfRelevant(attackerPlayer, intensity);
                        }
                    });
                }
            }
        }
    }

    private static void sendThreatIfRelevant(ServerPlayer player, float intensity) {
        // Clamp and only send meaningful updates.
        if (intensity < 0.01f) {
            // Send a 0 to tell the client to decay immediately when combat isn't threatening.
            ModMessages.sendToPlayer(new DemonThreatS2CPacket(0.0f), player);
            player.getTags().removeIf(t -> t.startsWith(DEMON_THREAT_TAG + ":"));
            return;
        }

        intensity = Math.min(1.0f, Math.max(0.0f, intensity));
        ModMessages.sendToPlayer(new DemonThreatS2CPacket(intensity), player);

        // Track an expiry moment using a simple entity tag with the server gameTime.
        long until = player.level.getGameTime() + THREAT_TIMEOUT_TICKS;
        player.getTags().removeIf(t -> t.startsWith(DEMON_THREAT_TAG + ":"));
        player.addTag(DEMON_THREAT_TAG + ":" + until);
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

        // Standard Swing Effects & Mastery Gain
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
                ModUtils.checkAndApplyRankUp(serverPlayer);
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

    @SubscribeEvent
    public static void onHashiraDeathDropSword(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof HashiraEntity hashira)) {
            return;
        }

        // Let players keep their loot logic separate; only spawn for real Hashira mobs.
        ItemStack sword = HashiraSwordHelper.createSwordForStyle(hashira.getBreathingStyle());
        if (sword.isEmpty()) {
            return;
        }

        // Spawn with randomized durability (10% - 85% remaining).
        if (sword.isDamageableItem()) {
            int max = sword.getMaxDamage();
            if (max > 0) {
                // Remaining durability percentage in [10, 85]
                int minPct = 10;
                int maxPct = 85;
                int pct = minPct + hashira.getRandom().nextInt((maxPct - minPct) + 1);

                int remaining = Math.max(1, Math.round(max * (pct / 100.0f)));
                int damage = Math.min(max - 1, max - remaining);
                sword.setDamageValue(damage);
            }
        }

        // Drop at the Hashira's position.
        hashira.spawnAtLocation(sword);
    }

    @SubscribeEvent
    public static void onDemonKilledByPlayer(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity dead = event.getEntity();
        boolean isDemon = dead instanceof DemonEntity
            || dead instanceof HandDemonEntity
            || dead instanceof NemesisDemonEntity
            || (dead instanceof Player && dead.getCapability(PlayerDemonProvider.PLAYER_DEMON)
            .map(PlayerDemon::isDemon).orElse(false));

        if (!isDemon) return;

        player.getCapability(PLAYER_PROGRESSION).ifPresent(progression -> {
            progression.incrementDemonKills();

            boolean killedKizuki = false;
            if (dead instanceof DemonEntity demonEntity) {
                killedKizuki = demonEntity.getDemonRank().isMoonRank();
            } else if (dead instanceof NemesisDemonEntity && dead.level instanceof ServerLevel serverLevel) {
                DemonHierarchyManager manager = DemonHierarchyManager.get(serverLevel);
                killedKizuki = manager.isKizuki(dead.getUUID());
            } else if (dead instanceof Player) {
                killedKizuki = dead.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(PlayerDemon::getRank)
                    .map(rank -> rank != null && rank.isMoonRank())
                    .orElse(false);
            }

            if (killedKizuki) {
                progression.recordKizukiKill();
            }
        });

        ModUtils.checkAndApplyRankUp(player);
    }
}
