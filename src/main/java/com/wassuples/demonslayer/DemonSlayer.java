package com.wassuples.demonslayer;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.wassuples.demonslayer.client.gui.SleepQTEOverlay;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.entity.custom.demon.*;
import com.wassuples.demonslayer.events.ModEvents;
import com.wassuples.demonslayer.gamerule.ModGameRules;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

import java.io.IOException;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod(DemonSlayer.MOD_ID)
public class DemonSlayer {
    public static final String MOD_ID = "demonslayer";

    public DemonSlayer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModGameRules.register();
        ModEntityTypes.register(modEventBus);
        ModEffects.register(modEventBus);
        MissionRegistry.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreate);

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

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("sleep_qte", SleepQTEOverlay.HUD_SLEEP_QTE);
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
        // MOONS
        event.put(ModEntityTypes.LOWER_MOON_SIX.get(), LowerMoonSixEntity.createAttributes().build());
        event.put(ModEntityTypes.LOWER_MOON_FIVE.get(), LowerMoonFiveEntity.createAttributes().build());
    }
}
