package com.wassuples.demonslayer.entity;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.entity.custom.demon.*;
import com.wassuples.demonslayer.entity.custom.hashira.*;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred registry for all custom {@link EntityType} instances in the mod.
 *
 * <p>Registers entity types for:
 * <ul>
 *   <li><b>Demons</b> — Fodder through Upper Moon tiers, plus Nemesis demons</li>
 *   <li><b>Hashira</b> — NPC Hashira for each breathing style</li>
 *   <li><b>Projectiles</b> — Blood Sickle, Lotus Shard, Poison Needle, thrown Nichirin, etc.</li>
 *   <li><b>Summons</b> — Mist Clone, Bodhisattva, Crystalline Divine Child, Compass Needle, etc.</li>
 *   <li><b>Companions</b> — Crow, Tamayo</li>
 * </ul>
 */
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DemonSlayer.MOD_ID);

    public static final RegistryObject<EntityType<BlindingMistCloudEntity>> BLINDING_MIST_CLOUD =
            ENTITY_TYPES.register("blinding_mist_cloud",
                    () -> EntityType.Builder.of(BlindingMistCloudEntity::new, MobCategory.MISC)
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
                    () -> EntityType.Builder.<CuttingThreadCageEntity>of(CuttingThreadCageEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f) // trap radius comes from ThreadTrapEntity, not the box
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("cutting_thread_cage"));

    public static final RegistryObject<EntityType<ThreadBasketEntity>> THREAD_BASKET =
            ENTITY_TYPES.register("thread_basket",
                    () -> EntityType.Builder.<ThreadBasketEntity>of(ThreadBasketEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("thread_basket"));

    public static final RegistryObject<EntityType<ThreadWheelEntity>> THREAD_WHEEL =
            ENTITY_TYPES.register("thread_wheel",
                    () -> EntityType.Builder.<ThreadWheelEntity>of(ThreadWheelEntity::new, MobCategory.MISC)
                            .sized(ThreadWheelEntity.DIAMETER, ThreadWheelEntity.DIAMETER)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
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
                            .clientTrackingRange(8)
                            .updateInterval(1) // smooth homing curves for the spinning blade
                            .build("blood_sickle"));

    public static final RegistryObject<EntityType<PoisonNeedleProjectileEntity>> POISON_NEEDLE =
            ENTITY_TYPES.register("poison_needle",
                    () -> EntityType.Builder.<PoisonNeedleProjectileEntity>of(PoisonNeedleProjectileEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .build("poison_needle"));

    public static final RegistryObject<EntityType<KillerGoldfishEntity>> KILLER_GOLDFISH =
            ENTITY_TYPES.register("killer_goldfish",
                    () -> EntityType.Builder.of(KillerGoldfishEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("killer_goldfish"));

    public static final RegistryObject<EntityType<WaterPrisonEntity>> WATER_PRISON_POT =
            ENTITY_TYPES.register("water_prison_pot",
                    () -> EntityType.Builder.of(WaterPrisonEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build("water_prison_pot"));

    public static final RegistryObject<EntityType<OctopusPotEntity>> OCTOPUS_POT =
            ENTITY_TYPES.register("octopus_pot",
                    () -> EntityType.Builder.of(OctopusPotEntity::new, MobCategory.MONSTER)
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
                    () -> EntityType.Builder.of(CrystallineDivineChildEntity::new, MobCategory.MONSTER)
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

    public static final RegistryObject<EntityType<ToonEyeProjectileEntity>> TOON_EYE =
            ENTITY_TYPES.register("toon_eye",
                    () -> EntityType.Builder.<ToonEyeProjectileEntity>of(ToonEyeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .setShouldReceiveVelocityUpdates(true)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("toon_eye"));

    public static final RegistryObject<EntityType<ToonBananaPeelEntity>> TOON_BANANA_PEEL =
            ENTITY_TYPES.register("toon_banana_peel",
                    () -> EntityType.Builder.<ToonBananaPeelEntity>of(ToonBananaPeelEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.1f)
                            .setShouldReceiveVelocityUpdates(true)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("toon_banana_peel"));

    public static final RegistryObject<EntityType<ToonShadowAnchorEntity>> TOON_SHADOW_ANCHOR =
            ENTITY_TYPES.register("toon_shadow_anchor",
                    () -> EntityType.Builder.<ToonShadowAnchorEntity>of(ToonShadowAnchorEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .noSave()
                            .build("toon_shadow_anchor"));

    public static final RegistryObject<EntityType<ToonFakeTunnelEntity>> TOON_FAKE_TUNNEL =
            ENTITY_TYPES.register("toon_fake_tunnel",
                    () -> EntityType.Builder.<ToonFakeTunnelEntity>of(ToonFakeTunnelEntity::new, MobCategory.MISC)
                            .sized(2.0f, 2.0f)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .noSave()
                            .build("toon_fake_tunnel"));

    public static final RegistryObject<EntityType<ToonAnvilEntity>> TOON_ANVIL =
            ENTITY_TYPES.register("toon_anvil",
                    () -> EntityType.Builder.<ToonAnvilEntity>of(ToonAnvilEntity::new, MobCategory.MISC)
                            .sized(1.2f, 1.0f)
                            .build("toon_anvil"));

    public static final RegistryObject<EntityType<WhisperWaveEntity>> WHISPER_WAVE =
            ENTITY_TYPES.register("whisper_wave",
                    () -> EntityType.Builder.<WhisperWaveEntity>of(WhisperWaveEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("whisper_wave"));

    public static final RegistryObject<EntityType<DreamAuraEntity>> DREAM_AURA =
            ENTITY_TYPES.register("dream_aura",
                    () -> EntityType.Builder.<DreamAuraEntity>of(DreamAuraEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.6f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("dream_aura"));

    public static final RegistryObject<EntityType<BloodArcDomeEntity>> BLOOD_ARC_DOME =
            ENTITY_TYPES.register("blood_arc_dome",
                    () -> EntityType.Builder.<BloodArcDomeEntity>of(BloodArcDomeEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("blood_arc_dome"));

    public static final RegistryObject<EntityType<BloodRingWaveEntity>> BLOOD_RING_WAVE =
            ENTITY_TYPES.register("blood_ring_wave",
                    () -> EntityType.Builder.<BloodRingWaveEntity>of(BloodRingWaveEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("blood_ring_wave"));

    public static final RegistryObject<EntityType<ObiSashEntity>> OBI_SASH =
            ENTITY_TYPES.register("obi_sash",
                    () -> EntityType.Builder.<ObiSashEntity>of(ObiSashEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("obi_sash"));

    public static final RegistryObject<EntityType<FishSchoolEntity>> FISH_SCHOOL =
            ENTITY_TYPES.register("fish_school",
                    () -> EntityType.Builder.<FishSchoolEntity>of(FishSchoolEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("fish_school"));

    public static final RegistryObject<EntityType<ScaleArmorEntity>> SCALE_ARMOR =
            ENTITY_TYPES.register("scale_armor",
                    () -> EntityType.Builder.<ScaleArmorEntity>of(ScaleArmorEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("scale_armor"));

    public static final RegistryObject<EntityType<HantenguDrumsEntity>> HANTENGU_DRUMS =
            ENTITY_TYPES.register("hantengu_drums",
                    () -> EntityType.Builder.<HantenguDrumsEntity>of(HantenguDrumsEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("hantengu_drums"));

    public static final RegistryObject<EntityType<WoodDragonStrikeEntity>> WOOD_DRAGON_STRIKE =
            ENTITY_TYPES.register("wood_dragon_strike",
                    () -> EntityType.Builder.<WoodDragonStrikeEntity>of(WoodDragonStrikeEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("wood_dragon_strike"));

    public static final RegistryObject<EntityType<ThunderDragonsEntity>> THUNDER_DRAGONS =
            ENTITY_TYPES.register("thunder_dragons",
                    () -> EntityType.Builder.<ThunderDragonsEntity>of(ThunderDragonsEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("thunder_dragons"));

    public static final RegistryObject<EntityType<SoundBlastEntity>> SOUND_BLAST =
            ENTITY_TYPES.register("sound_blast",
                    () -> EntityType.Builder.<SoundBlastEntity>of(SoundBlastEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("sound_blast"));

    public static final RegistryObject<EntityType<com.wassuples.demonslayer.entity.custom.OreOfferingEntity>> ORE_OFFERING =
            ENTITY_TYPES.register("ore_offering",
                    () -> EntityType.Builder.<com.wassuples.demonslayer.entity.custom.OreOfferingEntity>of(com.wassuples.demonslayer.entity.custom.OreOfferingEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.6f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("ore_offering"));

    public static final RegistryObject<EntityType<com.wassuples.demonslayer.entity.custom.BreathSlashEntity>> BREATH_SLASH =
            ENTITY_TYPES.register("breath_slash",
                    () -> EntityType.Builder.<com.wassuples.demonslayer.entity.custom.BreathSlashEntity>of(com.wassuples.demonslayer.entity.custom.BreathSlashEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("breath_slash"));

    public static final RegistryObject<EntityType<AkazaArtEntity>> AKAZA_ART =
            ENTITY_TYPES.register("akaza_art",
                    () -> EntityType.Builder.<AkazaArtEntity>of(AkazaArtEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("akaza_art"));

    public static final RegistryObject<EntityType<DomaArtEntity>> DOMA_ART =
            ENTITY_TYPES.register("doma_art",
                    () -> EntityType.Builder.<DomaArtEntity>of(DomaArtEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .noSave()
                            .fireImmune()
                            .build("doma_art"));

    public static final RegistryObject<EntityType<ToonSfxTextEntity>> TOON_SFX_TEXT =
            ENTITY_TYPES.register("toon_sfx_text",
                    () -> EntityType.Builder.<ToonSfxTextEntity>of(ToonSfxTextEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("toon_sfx_text"));

    public static final RegistryObject<EntityType<ToonDizzyBirdsEntity>> TOON_DIZZY_BIRDS =
            ENTITY_TYPES.register("toon_dizzy_birds",
                    () -> EntityType.Builder.<ToonDizzyBirdsEntity>of(ToonDizzyBirdsEntity::new, MobCategory.MISC)
                            .sized(0.8f, 0.4f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("toon_dizzy_birds"));

    public static final RegistryObject<EntityType<ToonPieEntity>> TOON_PIE =
            ENTITY_TYPES.register("toon_pie",
                    () -> EntityType.Builder.<ToonPieEntity>of(ToonPieEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.3f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("toon_pie"));

    public static final RegistryObject<EntityType<ToonSlotMachineEntity>> TOON_SLOT_MACHINE =
            ENTITY_TYPES.register("toon_slot_machine",
                    () -> EntityType.Builder.<ToonSlotMachineEntity>of(ToonSlotMachineEntity::new, MobCategory.MISC)
                            .sized(1.2f, 0.9f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("toon_slot_machine"));



    //##########################################################
    //              DEMONS
    //##########################################################
    public static final RegistryObject<EntityType<NemesisDemonEntity>> NEMESIS_DEMON =
            ENTITY_TYPES.register("nemesis_demon",
                    () -> EntityType.Builder.of(NemesisDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "nemesis_demon").toString()));


    public static final RegistryObject<EntityType<HandDemonEntity>> HAND_DEMON =
            ENTITY_TYPES.register("hand_demon",
                    () -> EntityType.Builder.of(HandDemonEntity::new, MobCategory.MONSTER)
                            .sized(2.8f, 4.6f) // an enormous round mass bound in its own arms (model drawn at 1.15x)
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

    /** Named story characters (Ubuyashiki twins, Final Selection candidates...): see StoryCharacter. */
    public static final RegistryObject<EntityType<com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity>> STORY_NPC =
            ENTITY_TYPES.register("story_npc",
                    () -> EntityType.Builder.of(com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "story_npc").toString()));

    /** Demons penned on Mt. Fujikasane's mountaintop for the Final Selection (not part of the Nemesis hierarchy). */
    public static final RegistryObject<EntityType<com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity>> CAPTIVE_DEMON =
            ENTITY_TYPES.register("captive_demon",
                    () -> EntityType.Builder.of(com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "captive_demon").toString()));

    public static final RegistryObject<EntityType<CrowEntity>> CROW =
            ENTITY_TYPES.register("crow",
                    () -> EntityType.Builder.of(CrowEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 0.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "crow").toString()));

    //##########################################################
    //              NPCs
    //##########################################################
    public static final RegistryObject<EntityType<TamayoEntity>> TAMAYO =
            ENTITY_TYPES.register("tamayo",
                    () -> EntityType.Builder.of(TamayoEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .build(new ResourceLocation(DemonSlayer.MOD_ID, "tamayo").toString()));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
