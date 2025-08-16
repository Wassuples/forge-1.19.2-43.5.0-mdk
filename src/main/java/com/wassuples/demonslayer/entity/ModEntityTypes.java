package com.wassuples.demonslayer.entity;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.entity.custom.demon.*;
import com.wassuples.demonslayer.entity.custom.hashira.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DemonSlayer.MOD_ID);

    public static final RegistryObject<EntityType<BlindingMistCloudEntity>> BLINDING_MIST_CLOUD =
            ENTITY_TYPES.register("blinding_mist_cloud",
                    () -> EntityType.Builder.<BlindingMistCloudEntity>of(BlindingMistCloudEntity::new, MobCategory.MISC)
                            .sized(6.0F, 0.5F) // The cloud's initial hitbox size
                            .clientTrackingRange(10)
                            .updateInterval(Integer.MAX_VALUE) // The cloud doesn't need constant updates
                            .build("blinding_mist_cloud"));

    public static final RegistryObject<EntityType<MistCloneEntity>> MIST_CLONE =
            ENTITY_TYPES.register("mist_clone",
                    () -> EntityType.Builder.of(MistCloneEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .build("mist_clone"));

    public static final RegistryObject<EntityType<CuttingThreadCageEntity>> CUTTING_THREAD_CAGE =
            ENTITY_TYPES.register("cutting_thread_cage",
                    () -> EntityType.Builder.of(CuttingThreadCageEntity::new, MobCategory.MISC)
                            .sized(8.0f, 4.0f)
                            .noSave()
                            .fireImmune()
                            .build("cutting_thread_cage"));

    public static final RegistryObject<EntityType<ThreadWheelEntity>> THREAD_WHEEL =
            ENTITY_TYPES.register("thread_wheel",
                    () -> EntityType.Builder.of(ThreadWheelEntity::new, MobCategory.MISC)
                            .sized(2.0f, 2.0f)
                            .build("thread_wheel"));

    public static final RegistryObject<EntityType<MotherEyeEntity>> MOTHER_EYE =
            ENTITY_TYPES.register("mother_eye",
                    () -> EntityType.Builder.of(MotherEyeEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("mother_eye"));

    public static final RegistryObject<EntityType<BloodSickleProjectileEntity>> BLOOD_SICKLE =
            ENTITY_TYPES.register("blood_sickle",
                    () -> EntityType.Builder.<BloodSickleProjectileEntity>of(BloodSickleProjectileEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .build("blood_sickle"));

    public static final RegistryObject<EntityType<PoisonNeedleProjectileEntity>> POISON_NEEDLE =
            ENTITY_TYPES.register("poison_needle",
                    () -> EntityType.Builder.<PoisonNeedleProjectileEntity>of(PoisonNeedleProjectileEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .build("poison_needle"));

    public static final RegistryObject<EntityType<KillerGoldfishEntity>> KILLER_GOLDFISH =
            ENTITY_TYPES.register("killer_goldfish",
                    () -> EntityType.Builder.<KillerGoldfishEntity>of(KillerGoldfishEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("killer_goldfish"));

    public static final RegistryObject<EntityType<WaterPrisonEntity>> WATER_PRISON_POT =
            ENTITY_TYPES.register("water_prison_pot",
                    () -> EntityType.Builder.<WaterPrisonEntity>of(WaterPrisonEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("water_prison_pot"));

    public static final RegistryObject<EntityType<OctopusPotEntity>> OCTOPUS_POT =
            ENTITY_TYPES.register("octopus_pot",
                    () -> EntityType.Builder.<OctopusPotEntity>of(OctopusPotEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("octopus_pot"));

    public static final RegistryObject<EntityType<WeepingSpearProjectileEntity>> WEEPING_SPEAR =
            ENTITY_TYPES.register("weeping_spear",
                    () -> EntityType.Builder.<WeepingSpearProjectileEntity>of(WeepingSpearProjectileEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .build("weeping_spear"));

    public static final RegistryObject<EntityType<CompassNeedleEntity>> COMPASS_EFFECT =
            ENTITY_TYPES.register("compass_effect",
                    () -> EntityType.Builder.<CompassNeedleEntity>of(CompassNeedleEntity::new, MobCategory.MISC)
                            .sized(1.0f,1.0f)
                            .noSave()
                            .build("compass_effect"));

    public static final RegistryObject<EntityType<ColdWhitePrincessEntity>> COLD_WHITE_PRINCESS =
            ENTITY_TYPES.register("cold_white_princess",
                    () -> EntityType.Builder.of(ColdWhitePrincessEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.0f)
                            .build("cold_white_princess"));

    public static final RegistryObject<EntityType<LotusShardProjectileEntity>> LOTUS_SHARD =
            ENTITY_TYPES.register("lotus_shard",
                    () -> EntityType.Builder.<LotusShardProjectileEntity>of(LotusShardProjectileEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .build("lotus_shard"));

    public static final RegistryObject<EntityType<CrystallineDivineChildEntity>> CRYSTALLINE_DIVINE_CHILD =
            ENTITY_TYPES.register("crystalline_divine_child",
                    () -> EntityType.Builder.<CrystallineDivineChildEntity>of(CrystallineDivineChildEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build("crystalline_divine_child"));

    public static final RegistryObject<EntityType<BodhisattvaEntity>> BODHISATTVA =
            ENTITY_TYPES.register("bodhisattva",
                    () -> EntityType.Builder.of(BodhisattvaEntity::new, MobCategory.MONSTER)
                            .sized(1f, 1f)
                            .build("bodhisattva"));

    public static final RegistryObject<EntityType<ThrownNichirinEntity>> THROWN_NICHIRIN =
            ENTITY_TYPES.register("thrown_nichirin",
                    () -> EntityType.Builder.<ThrownNichirinEntity>of(ThrownNichirinEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .setShouldReceiveVelocityUpdates(true)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .setCustomClientFactory((spawnPacket, world) ->
                                    new ThrownNichirinEntity(ModEntityTypes.THROWN_NICHIRIN.get(), world)
                            )
                            .build("thrown_nichirin"));



    //##########################################################
    //              DEMONS
    //##########################################################
    public static final RegistryObject<EntityType<FodderDemonEntity>> FODDER_DEMON =
            ENTITY_TYPES.register("fodder_demon",
                    () -> EntityType.Builder.of(FodderDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f) // Slightly smaller than a player
                            .build("fodder_demon"));

    public static final RegistryObject<EntityType<LowlyDemonEntity>> LOWLY_DEMON =
            ENTITY_TYPES.register("lowly_demon",
                    () -> EntityType.Builder.of(LowlyDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f).build(new ResourceLocation(DemonSlayer.MOD_ID, "lowly_demon").toString()));

    public static final RegistryObject<EntityType<CommonDemonEntity>> COMMON_DEMON =
            ENTITY_TYPES.register("common_demon",
                    () -> EntityType.Builder.of(CommonDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f).build(new ResourceLocation(DemonSlayer.MOD_ID, "common_demon").toString()));

    public static final RegistryObject<EntityType<GreaterDemonEntity>> GREATER_DEMON =
            ENTITY_TYPES.register("greater_demon",
                    () -> EntityType.Builder.of(GreaterDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f).build(new ResourceLocation(DemonSlayer.MOD_ID, "greater_demon").toString()));

    public static final RegistryObject<EntityType<EliteDemonEntity>> ELITE_DEMON =
            ENTITY_TYPES.register("elite_demon",
                    () -> EntityType.Builder.of(EliteDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f).build(new ResourceLocation(DemonSlayer.MOD_ID, "elite_demon").toString()));

    public static final RegistryObject<EntityType<HandDemonEntity>> HAND_DEMON =
            ENTITY_TYPES.register("hand_demon",
                    () -> EntityType.Builder.of(HandDemonEntity::new, MobCategory.MONSTER)
                            .sized(2.0f, 3.5f) // A large entity
                            .build("hand_demon"));

    //##########################################################
    //              MOONS
    //##########################################################
    public static final RegistryObject<EntityType<LowerMoonSixEntity>> LOWER_MOON_SIX =
            ENTITY_TYPES.register("lower_moon_six",
                    () -> EntityType.Builder.of(LowerMoonSixEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f) // Player-sized
                            .build("lower_moon_six"));

    public static final RegistryObject<EntityType<LowerMoonFiveEntity>> LOWER_MOON_FIVE =
            ENTITY_TYPES.register("lower_moon_five",
                    () -> EntityType.Builder.of(LowerMoonFiveEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f) // Same size as a player
                            .build("lower_moon_five"));

    public static final RegistryObject<EntityType<LowerMoonFourEntity>> LOWER_MOON_FOUR =
            ENTITY_TYPES.register("lower_moon_four",
                    () -> EntityType.Builder.of(LowerMoonFourEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("lower_moon_four"));

    public static final RegistryObject<EntityType<LowerMoonThreeEntity>> LOWER_MOON_THREE =
            ENTITY_TYPES.register("lower_moon_three",
                    () -> EntityType.Builder.of(LowerMoonThreeEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("lower_moon_three"));

    public static final RegistryObject<EntityType<LowerMoonTwoEntity>> LOWER_MOON_TWO =
            ENTITY_TYPES.register("lower_moon_two",
                    () -> EntityType.Builder.of(LowerMoonTwoEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("lower_moon_two"));

    public static final RegistryObject<EntityType<LowerMoonOneEntity>> LOWER_MOON_ONE =
            ENTITY_TYPES.register("lower_moon_one",
                    () -> EntityType.Builder.of(LowerMoonOneEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f) // Same size as a player
                            .build("lower_moon_one"));

    public static final RegistryObject<EntityType<UpperMoonFiveEntity>> UPPER_MOON_FIVE =
            ENTITY_TYPES.register("upper_moon_five",
                    () -> EntityType.Builder.of(UpperMoonFiveEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("upper_moon_five"));

    public static final RegistryObject<EntityType<UpperMoonFourEntity>> UPPER_MOON_FOUR =
            ENTITY_TYPES.register("upper_moon_four",
                    () -> EntityType.Builder.of(UpperMoonFourEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("upper_moon_four"));

    public static final RegistryObject<EntityType<UpperMoonThreeEntity>> UPPER_MOON_THREE =
            ENTITY_TYPES.register("upper_moon_three",
                    () -> EntityType.Builder.of(UpperMoonThreeEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("upper_moon_three"));

    public static final RegistryObject<EntityType<UpperMoonTwoEntity>> UPPER_MOON_TWO =
            ENTITY_TYPES.register("upper_moon_two",
                    () -> EntityType.Builder.of(UpperMoonTwoEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("upper_moon_two"));

    public static final RegistryObject<EntityType<UpperMoonOneEntity>> UPPER_MOON_ONE =
            ENTITY_TYPES.register("upper_moon_one",
                    () -> EntityType.Builder.of(UpperMoonOneEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.7f)
                            .build("upper_moon_one"));

    //##########################################################
    //              HASHIRA
    //##########################################################
    public static final RegistryObject<EntityType<HashiraEntity>> HASHIRA =
            ENTITY_TYPES.register("hashira",
                    () -> EntityType.Builder.of(HashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f) // Standard player size
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "hashira").toString()));

    public static final RegistryObject<EntityType<WaterHashiraEntity>> WATER_HASHIRA =
            ENTITY_TYPES.register("giyu_tomioka",
                    () -> EntityType.Builder.of(WaterHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "giyu").toString()));

    public static final RegistryObject<EntityType<InsectHashiraEntity>> INSECT_HASHIRA =
            ENTITY_TYPES.register("shinobu_kocho",
                    () -> EntityType.Builder.of(InsectHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "shinobu").toString()));

    public static final RegistryObject<EntityType<LoveHashiraEntity>> LOVE_HASHIRA =
            ENTITY_TYPES.register("mitsuri_kanroji",
                    () -> EntityType.Builder.of(LoveHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "mitsuri").toString()));

    public static final RegistryObject<EntityType<SnakeHashiraEntity>> SNAKE_HASHIRA =
            ENTITY_TYPES.register("obanai_iguro",
                    () -> EntityType.Builder.of(SnakeHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "mitsuri").toString()));

    public static final RegistryObject<EntityType<WindHashiraEntity>> WIND_HASHIRA =
            ENTITY_TYPES.register("sanemi_shinazugawa",
                    () -> EntityType.Builder.of(WindHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "sanemi").toString()));

    public static final RegistryObject<EntityType<StoneHashiraEntity>> STONE_HASHIRA =
            ENTITY_TYPES.register("gyomei_himejima",
                    () -> EntityType.Builder.of(StoneHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "gyomei").toString()));

    public static final RegistryObject<EntityType<MistHashiraEntity>> MIST_HASHIRA =
            ENTITY_TYPES.register("muichiro_tokito",
                    () -> EntityType.Builder.of(MistHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "muichiro").toString()));

    public static final RegistryObject<EntityType<FlameHashiraEntity>> FLAME_HASHIRA =
            ENTITY_TYPES.register("kyojuro_rengoku",
                    () -> EntityType.Builder.of(FlameHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "kyojuro").toString()));

    public static final RegistryObject<EntityType<FlowerHashiraEntity>> FLOWER_HASHIRA =
            ENTITY_TYPES.register("kanae_kocho",
                    () -> EntityType.Builder.of(FlowerHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "kanae").toString()));

    public static final RegistryObject<EntityType<SoundHashiraEntity>> SOUND_HASHIRA =
            ENTITY_TYPES.register("tengen_uzui",
                    () -> EntityType.Builder.of(SoundHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "tengen").toString()));

    public static final RegistryObject<EntityType<ThunderHashiraEntity>> THUNDER_HASHIRA =
            ENTITY_TYPES.register("jigoro_kuwajima",
                    () -> EntityType.Builder.of(ThunderHashiraEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.4f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "jigoro").toString()));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}