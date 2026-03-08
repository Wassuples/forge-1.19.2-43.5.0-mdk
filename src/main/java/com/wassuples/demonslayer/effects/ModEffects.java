package com.wassuples.demonslayer.effects;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred registry for all custom {@link MobEffect} instances in the mod.
 *
 * <p>Includes Wisteria Poison (anti-demon DoT), Forced Sleep (Enmu's hypnosis),
 * Paralyzing Poison (Insect Breathing), and Heat Lightning (Thunder Breathing DoT).
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DemonSlayer.MOD_ID);

    // Register our new Wisteria Poison effect
    public static final RegistryObject<MobEffect> WISTERIA_POISON = MOB_EFFECTS.register("wisteria_poison",
            () -> new WisteriaPoisonEffect(MobEffectCategory.HARMFUL, 0x8A2BE2)); // A nice BlueViolet color

    public static final RegistryObject<MobEffect> FORCED_SLEEP = MOB_EFFECTS.register("forced_sleep",
            () -> new ForcedSleepEffect(MobEffectCategory.HARMFUL, 0x8A2BE2));

    public static final RegistryObject<MobEffect> PARALYZING_POISON = MOB_EFFECTS.register("paralyzing_poison",
            () -> new ParalyzingPoisonEffect(MobEffectCategory.HARMFUL, 0x8A2BE2));

    public static final RegistryObject<MobEffect> HEAT_LIGHTNING = MOB_EFFECTS.register("heat_lightning",
            () -> new HeatLightningEffect(MobEffectCategory.HARMFUL, 0xFFE66B));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
