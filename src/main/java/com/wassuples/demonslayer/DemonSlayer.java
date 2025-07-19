package com.wassuples.demonslayer;

import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.client.gui.SleepQTEOverlay;
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
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.worldgen.biome.ModBiomes;
import com.wassuples.demonslayer.worldgen.terrablender.ModOverworldRegion;
import com.wassuples.demonslayer.worldgen.terrablender.ModTerraBlender;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import terrablender.api.Regions;

@Mod(DemonSlayer.MOD_ID)
public class DemonSlayer {
    public static final String MOD_ID = "demonslayer";

    public DemonSlayer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModGameRules.register();
        ModEntityTypes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModBlocks.register(modEventBus);
        MissionRegistry.register();

        ModBiomes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreate);
        modEventBus.addListener(this::registerTerraBlender);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ModEvents.class);
        GeckoLib.initialize();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // This now compiles correctly because DemonEntity.canSpawn is generic
            SpawnPlacements.register(ModEntityTypes.FODDER_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DemonEntity::canSpawn);
            SpawnPlacements.register(ModEntityTypes.LOWLY_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DemonEntity::canSpawn);
            SpawnPlacements.register(ModEntityTypes.COMMON_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DemonEntity::canSpawn);
            SpawnPlacements.register(ModEntityTypes.GREATER_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DemonEntity::canSpawn);
            SpawnPlacements.register(ModEntityTypes.ELITE_DEMON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DemonEntity::canSpawn);
        });

        ModMessages.register();
    }

    private void registerTerraBlender(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ModOverworldRegion(new ResourceLocation(MOD_ID, "overworld"), 5));
        });
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("sleep_qte", SleepQTEOverlay.HUD_SLEEP_QTE);
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
            event.registerEntityRenderer(ModEntityTypes.CUTTING_THREAD_CAGE.get(), CuttingThreadCageRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.THREAD_WHEEL.get(), ThreadWheelRenderer::new);
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
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_FOUR.get(), LowerMoonFourRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_THREE.get(), LowerMoonThreeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_TWO.get(), LowerMoonTwoRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LOWER_MOON_ONE.get(), LowerMoonOneRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_FIVE.get(), UpperMoonFiveRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_FOUR.get(), UpperMoonFourRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_THREE.get(), UpperMoonThreeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_TWO.get(), UpperMoonTwoRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.UPPER_MOON_ONE.get(), UpperMoonOneRenderer::new);
        }
    }

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
        event.put(ModEntityTypes.FODDER_DEMON.get(), FodderDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWLY_DEMON.get(), LowlyDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.COMMON_DEMON.get(), CommonDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.GREATER_DEMON.get(), GreaterDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.ELITE_DEMON.get(), EliteDemonEntity.createAttributes().build());
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


    }
}
