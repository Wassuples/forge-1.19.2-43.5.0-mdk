package com.wassuples.demonslayer;

import com.mojang.logging.LogUtils;
import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.block.ModFlammableBlocks;
import com.wassuples.demonslayer.client.gui.SleepQTEOverlay;
import com.wassuples.demonslayer.client.gui.SunForgeScreen;
import com.wassuples.demonslayer.client.gui.SwordsmithTableScreen;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.entity.custom.demon.*;
import com.wassuples.demonslayer.entity.renderers.*;
import com.wassuples.demonslayer.entity.renderers.demon.*;
import com.wassuples.demonslayer.entity.renderers.hashira.*;
import com.wassuples.demonslayer.events.ModEvents;
import com.wassuples.demonslayer.gamerule.ModGameRules;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.missions.MissionReloadListener;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.recipe.ModRecipes;
import com.wassuples.demonslayer.worldgen.biome.ModBiomes;
import com.wassuples.demonslayer.worldgen.inventory.ModMenuTypes;
import com.wassuples.demonslayer.worldgen.terrablender.ModOverworldRegion;
import com.wassuples.demonslayer.reputation.ReputationEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;
import terrablender.api.Regions;

/**
 * Main entry point for the Demon Slayer Minecraft Forge mod.
 *
 * <p>This class bootstraps all mod subsystems during Forge's mod loading lifecycle:
 *
 * <ul>
 *   <li><b>Registry</b> — items, blocks, block entities, menus, recipes, entities, effects, biomes</li>
 *   <li><b>Networking</b> — custom packet channel via {@link ModMessages}</li>
 *   <li><b>World generation</b> — TerraBlender biome region and spawn placements</li>
 *   <li><b>Client rendering</b> — entity renderers, GUI overlays, menu screens</li>
 *   <li><b>Data loading</b> — mission JSON reload listener</li>
 * </ul>
 *
 * <h2>Architecture overview</h2>
 * <p>Player state is stored in Forge capabilities (see {@code breathing/}, {@code demon/},
 * {@code ranking/}, {@code cooldown/}, {@code crow/}, {@code glow/}, {@code missions/} packages).
 * Client–server synchronization uses the packets in {@code networking.packet}. Combat forms are defined
 * as enum constants implementing {@link com.wassuples.demonslayer.styles.IForm} and executed via
 * {@link com.wassuples.demonslayer.forms.ActiveFormManager}.
 *
 * @see com.wassuples.demonslayer.styles.IForm
 * @see com.wassuples.demonslayer.forms.ActiveFormManager
 * @see com.wassuples.demonslayer.events.ModEvents
 */

/**
 * bootstraps.
 */
@Mod(DemonSlayer.MOD_ID)
public class DemonSlayer {
    /** Mod identifier used for resource locations, capability keys, and network channels. */
    public static final String MOD_ID = "demonslayer";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Registers all deferred registries on the mod event bus, wires event listeners, and
     * initializes third-party libraries (GeckoLib for animated models).
     */
    public DemonSlayer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        com.wassuples.demonslayer.block.entity.ModBlockEntities.register(modEventBus);
        com.wassuples.demonslayer.worldgen.inventory.ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModGameRules.register();
        ModEntityTypes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModBiomes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreate);
        modEventBus.addListener(this::registerTerraBlender);
        MinecraftForge.EVENT_BUS.addListener(this::registerReloadListeners);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ModEvents.class);
        MinecraftForge.EVENT_BUS.register(ReputationEvents.class);
        GeckoLib.initialize();
    }

    /**
     * Common setup phase — runs after all registries are frozen.
     *
     * <p>Registers spawn placements for demon entities and crow, sets up flammable blocks,
     * and initializes the network channel.
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SpawnPlacements.register(ModEntityTypes.NEMESIS_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (entityType, level, spawnType, pos, random) -> {
                        boolean isSolidGround = !level.getBlockState(pos.below()).isAir();
                        float time = level.getTimeOfDay(1.0F);
                        boolean isNightTime = time > 0.25F && time < 0.75F;
                        return isSolidGround && isNightTime;
                    });
            SpawnPlacements.register(ModEntityTypes.CROW.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrowEntity::checkCrowSpawnRules);

            ModFlammableBlocks.register();
        });

        ModMessages.register();
    }

    /** Registers the TerraBlender overworld biome region for Wisteria Forest generation. */
    private void registerTerraBlender(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ModOverworldRegion(new ResourceLocation(MOD_ID, "overworld"), 5));
        });
    }

    /** Registers the mission JSON reload listener so missions refresh on {@code /reload}. */
    private void registerReloadListeners(final AddReloadListenerEvent event) {
         event.addListener(new MissionReloadListener());
     }

    /**
     * Client-only event subscribers registered on the <b>mod</b> event bus.
     *
     * <p>Handles entity renderer registration, GUI overlay registration, and menu screen binding.
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        /** Registers HUD overlays (e.g. Sleep QTE) above all other GUI layers. */
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("sleep_qte", SleepQTEOverlay.HUD_SLEEP_QTE);
        }

        /** Binds every custom entity type to its client-side renderer. */
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
            event.registerEntityRenderer(ModEntityTypes.CUTTING_THREAD_CAGE.get(), CuttingThreadCageRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.THREAD_WHEEL.get(), ThreadWheelRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CROW.get(), CrowRenderer::new);
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
            event.registerEntityRenderer(ModEntityTypes.NEMESIS_DEMON.get(), NemesisDemonRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.HAND_DEMON.get(), HandDemonRenderer::new);
            // MOONS
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_SIX.get(), LowerMoonSixRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_FIVE.get(), LowerMoonFiveRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_FOUR.get(), LowerMoonFourRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_THREE.get(), LowerMoonThreeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_TWO.get(), LowerMoonTwoRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_ONE.get(), LowerMoonOneRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_FIVE.get(), UpperMoonFiveRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_FOUR.get(), UpperMoonFourRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_THREE.get(), UpperMoonThreeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_TWO.get(), UpperMoonTwoRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_ONE.get(), UpperMoonOneRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.TAMAYO.get(), TamayoRenderer::new);
        }

        /** Binds menu types to their screen implementations (Sun Forge, Swordsmith Table). */
        @SubscribeEvent
        public static void onClientModSetup(FMLClientSetupEvent event) {
            MenuScreens.register(ModMenuTypes.SUN_FORGE_MENU.get(), SunForgeScreen::new);
            MenuScreens.register(ModMenuTypes.SWORDSMITH_TABLE_MENU.get(), SwordsmithTableScreen::new);
        }
    }

    /**
     * Registers attribute maps for all living entity types.
     *
     * <p>This must be called during the {@link EntityAttributeCreationEvent} to avoid
     * {@code IllegalStateException} when entities spawn without attributes.
     */
    public void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.MIST_CLONE.get(), MistCloneEntity.createAttributes().build());
        event.put(ModEntityTypes.CUTTING_THREAD_CAGE.get(), CuttingThreadCageEntity.createAttributes().build());
        event.put(ModEntityTypes.THREAD_WHEEL.get(), ThreadWheelEntity.createAttributes().build());
        event.put(ModEntityTypes.MOTHER_EYE.get(), MotherEyeEntity.createAttributes().build());
        event.put(ModEntityTypes.KILLER_GOLDFISH.get(), KillerGoldfishEntity.createAttributes().build());
        event.put(ModEntityTypes.WATER_PRISON_POT.get(), WaterPrisonEntity.createAttributes().build());
        event.put(ModEntityTypes.OCTOPUS_POT.get(), OctopusPotEntity.createAttributes().build());
        event.put(ModEntityTypes.COMPASS_EFFECT.get(), CompassNeedleEntity.createAttributes().build());
        event.put(ModEntityTypes.COLD_WHITE_PRINCESS.get(), ColdWhitePrincessEntity.createAttributes().build());
        event.put(ModEntityTypes.CRYSTALLINE_DIVINE_CHILD.get(), CrystallineDivineChildEntity.createAttributes().build());
        event.put(ModEntityTypes.BODHISATTVA.get(), BodhisattvaEntity.createAttributes().build());
        event.put(ModEntityTypes.CROW.get(), CrowEntity.createAttributes().build());

        // HASHIRA
        event.put(ModEntityTypes.HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.WATER_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.INSECT_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.LOVE_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.SNAKE_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.WIND_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.STONE_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.MIST_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.FLAME_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.FLOWER_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.SOUND_HASHIRA.get(), HashiraEntity.createAttributes().build());
        event.put(ModEntityTypes.THUNDER_HASHIRA.get(), HashiraEntity.createAttributes().build());
        // DEMONS
        event.put(ModEntityTypes.NEMESIS_DEMON.get(), NemesisDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.HAND_DEMON.get(), HandDemonEntity.createAttributes().build());
        // LOWER MOONS
        event.put(ModEntityTypes.LOWER_MOON_SIX.get(), LowerMoonSixEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_FIVE.get(), LowerMoonFiveEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_FOUR.get(), LowerMoonFourEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_THREE.get(), LowerMoonThreeEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_TWO.get(), LowerMoonTwoEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_ONE.get(), LowerMoonOneEntity.createAttributes().build());
        // UPPER MOONS
        event.put(ModEntityTypes.UPPER_MOON_FIVE.get(), UpperMoonFiveEntity.createAttributes().build());
        event.put(ModEntityTypes.UPPER_MOON_FOUR.get(), UpperMoonFourEntity.createAttributes().build());
        event.put(ModEntityTypes.UPPER_MOON_THREE.get(), UpperMoonThreeEntity.createAttributes().build());
        event.put(ModEntityTypes.UPPER_MOON_TWO.get(), UpperMoonTwoEntity.createAttributes().build());
        event.put(ModEntityTypes.UPPER_MOON_ONE.get(), UpperMoonOneEntity.createAttributes().build());
        event.put(ModEntityTypes.TAMAYO.get(), TamayoEntity.createAttributes().build());


    }
}
