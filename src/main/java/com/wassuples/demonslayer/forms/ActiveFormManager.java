package com.wassuples.demonslayer.forms;

import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.forms.demon.akaza.*;
import com.wassuples.demonslayer.forms.demon.daki.EightLayeredObiSlashForm;
import com.wassuples.demonslayer.forms.demon.doma.*;
import com.wassuples.demonslayer.forms.demon.enmu.EyesOfSleepForm;
import com.wassuples.demonslayer.forms.demon.enmu.WhispersHypnosisForm;
import com.wassuples.demonslayer.forms.demon.gyokko.*;
import com.wassuples.demonslayer.forms.demon.gyutaro.FlyingBloodSicklesForm;
import com.wassuples.demonslayer.forms.demon.gyutaro.RampantArcRampageParry;
import com.wassuples.demonslayer.forms.demon.gyutaro.RotatingCircularSlashesForm;
import com.wassuples.demonslayer.forms.demon.hantengu.CompressedSoundWavesForm;
import com.wassuples.demonslayer.forms.demon.hantengu.CountlessStrikingTreesForm;
import com.wassuples.demonslayer.forms.demon.hantengu.CrazedCryOfThunderdeathForm;
import com.wassuples.demonslayer.forms.demon.hantengu.WeepingSpearsForm;
import com.wassuples.demonslayer.forms.demon.rui.CuttingThreadCageForm;
import com.wassuples.demonslayer.forms.demon.rui.CuttingThreadRotationForm;
import com.wassuples.demonslayer.forms.demon.rui.MurderousEyeBasketForm;
import com.wassuples.demonslayer.forms.slayer.beast.*;
import com.wassuples.demonslayer.forms.slayer.flame.*;
import com.wassuples.demonslayer.forms.slayer.flower.HonorableShadowPlumParry;
import com.wassuples.demonslayer.forms.slayer.flower.PeoniesOfFutilityForm;
import com.wassuples.demonslayer.forms.slayer.flower.VermilionEyeForm;
import com.wassuples.demonslayer.forms.slayer.flower.WhirlingPeachForm;
import com.wassuples.demonslayer.forms.slayer.insect.BeeStingFlutterForm;
import com.wassuples.demonslayer.forms.slayer.insect.ButterflyDanceCapriceForm;
import com.wassuples.demonslayer.forms.slayer.insect.CentipedeDanceForm;
import com.wassuples.demonslayer.forms.slayer.insect.DragonflyDanceForm;
import com.wassuples.demonslayer.forms.slayer.love.*;
import com.wassuples.demonslayer.forms.slayer.mist.*;
import com.wassuples.demonslayer.forms.slayer.moon.*;
import com.wassuples.demonslayer.forms.slayer.serpent.CoilChokeForm;
import com.wassuples.demonslayer.forms.slayer.serpent.SlitheringSerpentForm;
import com.wassuples.demonslayer.forms.slayer.serpent.VenomFangsForm;
import com.wassuples.demonslayer.forms.slayer.sound.ResoundingSlashesForm;
import com.wassuples.demonslayer.forms.slayer.sound.RoarForm;
import com.wassuples.demonslayer.forms.slayer.sound.StringPerformanceForm;
import com.wassuples.demonslayer.forms.slayer.stone.*;
import com.wassuples.demonslayer.forms.slayer.sun.*;
import com.wassuples.demonslayer.forms.slayer.thunder.*;
import com.wassuples.demonslayer.forms.slayer.water.*;
import com.wassuples.demonslayer.forms.slayer.wind.*;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.StopFormS2CPacket;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.wassuples.demonslayer.styles.BreathingForms.*;
import static com.wassuples.demonslayer.styles.DemonForms.*;


@Mod.EventBusSubscriber(modid = "demonslayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
public  class ActiveFormManager {
    private static final boolean DEBUG_FORMS = true;
    private static final Map<UUID, ActiveForm> ACTIVE_FORMS = new HashMap<>();
    private static final Map<UUID, Map<IForm, ActiveForm>> BUFF_FORMS = new HashMap<>();
    private static final Map<IForm, BiFunction<LivingEntity, IForm, ActiveForm>> FORM_FACTORIES;
    public static final UUID KNOCKBACK_IMMUNITY_UUID = UUID.fromString("d8a6a6dc-34d3-4e42-8c19-3c871343399b");

    // --- Manager Public API ---
    public static void start(LivingEntity user, IForm form) {
        if (DEBUG_FORMS && user instanceof ServerPlayer sp) {
            DemonSlayer.LOGGER.info("[ActiveForm] start request player={} form={} tick={} dim={} buff={}",
                    sp.getGameProfile().getName(), form.getClass().getName() + "#" + form, sp.level.getGameTime(), sp.level.dimension().location(), form.isBuff());
        }
        if (form.isBuff()) {
            startBuffForm(user, form);
        } else {
            startActiveForm(user, form);
        }
    }

    private static void startActiveForm(LivingEntity user, IForm form) {
        if (ACTIVE_FORMS.containsKey(user.getUUID())) {
            ActiveForm existingForm = ACTIVE_FORMS.get(user.getUUID());
            if (!existingForm.isInterruptible()) {
                if (DEBUG_FORMS && user instanceof ServerPlayer sp) {
                    DemonSlayer.LOGGER.info("[ActiveForm] startActive blocked: existing not interruptible existingForm={} player={}",
                            existingForm.getForm().getClass().getName() + "#" + existingForm.getForm(), sp.getGameProfile().getName());
                }
                return;
            }
        }
        stop(user);

        BiFunction<LivingEntity, IForm, ActiveForm> factory = FORM_FACTORIES.get(form);
        if (factory != null) {
            ActiveForm activeForm = factory.apply(user, form);
            ACTIVE_FORMS.put(user.getUUID(), activeForm);
            if (DEBUG_FORMS && user instanceof ServerPlayer sp) {
                DemonSlayer.LOGGER.info("[ActiveForm] started activeFormClass={} form={} player={}",
                        activeForm.getClass().getSimpleName(), form.getClass().getName() + "#" + form, sp.getGameProfile().getName());
            }
            activeForm.onStart();
        } else if (DEBUG_FORMS && user instanceof ServerPlayer sp) {
            DemonSlayer.LOGGER.warn("[ActiveForm] no factory for form={} player={}", form.getClass().getName() + "#" + form, sp.getGameProfile().getName());
        }
    }
    private static void startBuffForm(LivingEntity user, IForm form) {
        Map<IForm, ActiveForm> playerBuffs = BUFF_FORMS.computeIfAbsent(user.getUUID(), k -> new HashMap<>());

        if (playerBuffs.containsKey(form)) {
            stopBuffForm(user, form);
        } else {
            BiFunction<LivingEntity, IForm, ActiveForm> factory = FORM_FACTORIES.get(form);
            if (factory != null) {
                ActiveForm activeForm = factory.apply(user, form);
                playerBuffs.put(form, activeForm);
                activeForm.onStart();
            }
        }
    }


    public static void stop(LivingEntity user) {
        stop(user, true);
    }
    public static void stop(LivingEntity user, boolean notifyClient) {
        ActiveForm form = ACTIVE_FORMS.remove(user.getUUID());
        if (form != null) {
            if (DEBUG_FORMS && user instanceof ServerPlayer sp) {
                DemonSlayer.LOGGER.info("[ActiveForm] stop player={} form={} notifyClient={} tick={}",
                        sp.getGameProfile().getName(), form.getForm().getClass().getName() + "#" + form.getForm(), notifyClient, sp.level.getGameTime());
            }
            form.onStop();

            if (notifyClient && user instanceof ServerPlayer player) {
                ModMessages.sendToPlayer(new StopFormS2CPacket(), player);
            }
        }
    }
    public static void stopBuffForm(LivingEntity user, IForm form) {
        Map<IForm, ActiveForm> playerBuffs = BUFF_FORMS.get(user.getUUID());
        if (playerBuffs != null) {
            ActiveForm activeForm = playerBuffs.remove(form);
            if (activeForm != null) {
                activeForm.onStop();
            }
        }
    }

    public static void startCharge(LivingEntity user, IForm form) {
        if (form.getInputType() != BreathingForms.InputType.HOLD) return;
        start(user, form);
    }
    public static void releaseCharge(LivingEntity user) {
        ActiveForm activeForm = ACTIVE_FORMS.get(user.getUUID());
        if (activeForm != null) {
            System.out.println("[DEBUG] ==> Attempting to RELEASE charge for active form: " + activeForm.getForm().getDisplayName());
            if (activeForm instanceof ChargeableForm chargeable) {
                boolean wasFullyCharged = chargeable.chargeTicks >= chargeable.chargeGoal;
                System.out.println("[DEBUG] Charge was " + (wasFullyCharged ? "SUCCESSFUL" : "INCOMPLETE"));
                chargeable.release(wasFullyCharged);
            } else {
                System.out.println("[DEBUG] ERROR: Active form is not a ChargeableForm!");
            }
        } else {
            System.out.println("[DEBUG] ERROR: releaseCharge() called, but no active form was found.");
        }
    }

    @Nullable
    public static ActiveForm getActiveForm(LivingEntity user) {
        return ACTIVE_FORMS.get(user.getUUID());
    }

    public static boolean isBuffActive(LivingEntity user, IForm form) {
        Map<IForm, ActiveForm> playerBuffs = BUFF_FORMS.get(user.getUUID());
        return playerBuffs != null && playerBuffs.containsKey(form);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            ActiveForm activeForm = ACTIVE_FORMS.get(player.getUUID());
            if (activeForm != null) {
                activeForm.tick();
            }

            Map<IForm, ActiveForm> buffForms = BUFF_FORMS.get(player.getUUID());
            if (buffForms != null && !buffForms.isEmpty()) {
                buffForms.values().removeIf(buff -> {
                    buff.tick();
                    // This part is for future use with buffs that have duration
                    return false;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
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

        if (target instanceof ServerPlayer player) {
            ActiveForm activeForm = ACTIVE_FORMS.get(player.getUUID());
            if (activeForm != null) {
                activeForm.onHurt(event);
            }

            Map<IForm, ActiveForm> buffForms = BUFF_FORMS.get(player.getUUID());
            if (buffForms != null) {
                for (ActiveForm buff : buffForms.values()) {
                    buff.onHurt(event);
                }
            }
        }
    }

    // --- Static Initializer for Form Factories ---
    static {
        FORM_FACTORIES = new ImmutableMap.Builder<IForm, BiFunction<LivingEntity, IForm, ActiveForm>>()
                // WATER
                .put(WATER_2, WaterWheelForm::new)
                .put(WATER_3, FlowingDanceForm::new)
                .put(WATER_6, WhirlpoolForm::new)
                .put(WATER_7, WaterThrustForm::new)
                .put(WATER_8, WaterfallBasinForm::new)
                .put(WATER_10,ConstantFluxForm::new)
                .put(WATER_9, SplashingWaterFlowForm::new)
                .put(WATER_11,DeadCalmParry::new)
                // FLAME
                .put(FLAME_1, UnknowingFireForm::new)
                .put(FLAME_3, BlazingUniverseForm::new)
                .put(FLAME_4, FlameUndulationParry::new)
                .put(FLAME_5, FlameTigerForm::new)
                .put(FLAME_9, RengokuForm::new)
                // WIND
                .put(WIND_1, DustWhirlwindCutterForm::new)
                .put(WIND_3, ClearStormWindTreeParry::new)
                .put(WIND_4, RisingDustStormForm::new)
                .put(WIND_5, ColdMountainWindForm::new)
                .put(WIND_7, GaleSuddenGustsForm::new)
                .put(WIND_8, PrimaryGaleSlashForm::new)
                .put(WIND_9, IdatenTyphoonForm::new)
                // THUNDER
                .put(THUNDER_1, ThunderclapAndFlashForm::new)
                .put(THUNDER_2, RiceSpiritForm::new)
                .put(THUNDER_3, ThunderSwarmForm::new)
                .put(THUNDER_4, DistantThunderForm::new)
                .put(THUNDER_5, com.wassuples.demonslayer.forms.slayer.thunder.HeatLightningForm::new)
                .put(THUNDER_6, RumbleAndFlashForm::new)
                .put(THUNDER_7, HonoikazuchiNoKamiForm::new)
                // MIST
                .put(MIST_2, EightLayeredMistForm::new)
                .put(MIST_3, ScatteringMistParry::new)
                .put(MIST_4, ShiftingFlowSlashForm::new)
                .put(MIST_5, SeaOfCloudsForm::new)
                .put(MIST_6, LunarDispersingMist::new)
                // SOUND
                .put(SOUND_1, RoarForm::new)
                .put(SOUND_4, ResoundingSlashesForm::new)
                .put(SOUND_5, StringPerformanceForm::new)
                // LOVE
                .put(LOVE_1, ShiversOfFirstLoveForm::new)
                .put(LOVE_2, LovePangsForm::new)
                .put(LOVE_3, CatloveShowerForm::new)
                .put(LOVE_5, SwayingLoveWildclawForm::new)
                .put(LOVE_6, CatLeggedWindsForm::new)
                // INSECT
                .put(INSECT_1, ButterflyDanceCapriceForm::new)
                .put(INSECT_2, BeeStingFlutterForm::new)
                .put(INSECT_3, DragonflyDanceForm::new)
                .put(INSECT_4, CentipedeDanceForm::new)
                // STONE
                .put(STONE_1, SerpentiniteBipolarForm::new)
                .put(STONE_2, UpperSmashForm::new)
                .put(STONE_3, StoneSkinParry::new)
                .put(STONE_4, VolcanicRockForm::new)
                .put(STONE_5, ArcsOfJusticeForm::new)
                // FLOWER
                .put(FLOWER_2, HonorableShadowPlumParry::new)
                .put(FLOWER_5, PeoniesOfFutilityForm::new)
                .put(FLOWER_6, WhirlingPeachForm::new)
                .put(FLOWER_7, VermilionEyeForm::new)
                // SERPENT
                .put(SERPENT_2, VenomFangsForm::new)
                .put(SERPENT_3, CoilChokeForm::new)
                .put(SERPENT_5, SlitheringSerpentForm::new)
                // MOON
                .put(MOON_2, PearlFlowerMoongazingForm::new)
                .put(MOON_5, CalamitousEddyForm::new)
                .put(MOON_6, PerpetualNightForm::new)
                .put(MOON_7, MirrorOfMisfortuneForm::new)
                .put(MOON_8, MoonDragonRingtailForm::new)
                .put(MOON_9, WaningMoonswathsForm::new)
                .put(MOON_10, DrillingSlashesForm::new)
                .put(MOON_14, CatastropheForm::new)
                .put(MOON_16, MoonbowForm::new)
                // BEAST BREATHING
                .put(BEAST_4, SliceNDiceForm::new)
                .put(BEAST_5, CrazyCuttingForm::new)
                .put(BEAST_6, PalisadeBiteForm::new)
                .put(BEAST_7, SpatialAwarenessForm::new)
                .put(BEAST_8, ExplosiveRushForm::new)
                .put(BEAST_9, ExtendingBendySlashForm::new)
                .put(BEAST_10, WhirlingFangsForm::new)
                // BDA, Rui
                .put(THREADS_1, CuttingThreadCageForm::new)
                .put(THREADS_2, MurderousEyeBasketForm::new)
                .put(THREADS_3, CuttingThreadRotationForm::new)
                // BDA, Enmu
                .put(SLEEP_1, WhispersHypnosisForm::new)
                .put(SLEEP_2, EyesOfSleepForm::new)
                // BDA, Daki
                .put(SASH_1, EightLayeredObiSlashForm::new)
                // BDA, Gyutaro
                .put(HAEMO_1, FlyingBloodSicklesForm::new)
                .put(HAEMO_2, RampantArcRampageParry::new)
                .put(HAEMO_3, RotatingCircularSlashesForm::new)
                // BDA, Gyokko
                .put(FISH_1, ThousandNeedleFishKillForm::new)
                .put(FISH_2, WaterPrisonPotForm::new)
                .put(FISH_3, OctopusVaseHellForm::new)
                .put(FISH_4, TenThousandFishBarrageForm::new)
                .put(FISH_5, KillerFishScalesForm::new)
                // BDA, Hantengu
                .put(EMOTION_1, WeepingSpearsForm::new)
                .put(EMOTION_2, CrazedCryOfThunderdeathForm::new)
                .put(EMOTION_3, CountlessStrikingTreesForm::new)
                .put(EMOTION_4, CompressedSoundWavesForm::new)
                // BDA, Akaza
                .put(DESTRUCTIVE_DEATH_1, CompassNeedleForm::new)
                .put(DESTRUCTIVE_DEATH_2, AirTypeForm::new)
                .put(DESTRUCTIVE_DEATH_3, DisorderForm::new)
                .put(DESTRUCTIVE_DEATH_4, AnnihilationTypeForm::new)
                .put(DESTRUCTIVE_DEATH_5, CrownSplitterForm::new)
                .put(DESTRUCTIVE_DEATH_6, ExplosiveFlurryForm::new)
                .put(DESTRUCTIVE_DEATH_7, FlyingPlanetThousandWheelsForm::new)
                .put(DESTRUCTIVE_DEATH_8, EightLayeredDemonCoreForm::new)
                .put(DESTRUCTIVE_DEATH_9, TenThousandLeavesWillowForm::new)
                .put(DESTRUCTIVE_DEATH_10, FinalFormChaoticAfterglowForm::new)
                // BDA, Doma
                .put(CRYO_1, FrozenLotusForm::new)
                .put(CRYO_2, BarrenHangingGardenForm::new)
                .put(CRYO_3, FreezingCloudsForm::new)
                .put(CRYO_4, LotusVinesForm::new)
                .put(CRYO_5, ColdWhitePrincessesForm::new)
                .put(CRYO_6, WintryIciclesForm::new)
                .put(CRYO_7, ScatteringLotusesForm::new)
                .put(CRYO_8, CrystallineDivineChildForm::new)
                .put(CRYO_9, RimeBodhisattvaForm::new)
                // HINOKAMI KAGURA, SUN
                .put(SUN_2, ClearBlueSkyForm::new)
                .put(SUN_3, RagingSunForm::new)
                .put(SUN_4, BurningBonesForm::new)
                .put(SUN_5, SettingSunForm::new)
                .put(SUN_6, SolarHeatHazeForm::new)
                .put(SUN_7, BeneficentRadianceForm::new)
                .put(SUN_8, SunflowerThrustForm::new)
                .put(SUN_9, SunHaloDragonForm::new)
                .put(SUN_10, FireWheelForm::new)
                .put(SUN_11, FakeRainbowForm::new)
                .put(SUN_12, FlameDanceForm::new)
                .put(SUN_13, ThirteenthForm::new)
                .build();
    }
}

