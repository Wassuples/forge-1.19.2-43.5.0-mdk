package com.wassuples.demonslayer.particle;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod's own particles (textures under {@code textures/particle/}, client factories in
 * {@code client.particle.ModParticleProviders}). Prefer these over vanilla particles for anything the mod shows.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DemonSlayer.MOD_ID);

    /** A wisteria petal tumbling down from blossoms. */
    public static final RegistryObject<SimpleParticleType> WISTERIA_PETAL = PARTICLES.register("wisteria_petal", () -> new SimpleParticleType(false));
    /** Grey-black ash flaking off a demon as it crumbles (dawn, beheading, withdrawal). */
    public static final RegistryObject<SimpleParticleType> DEMON_ASH = PARTICLES.register("demon_ash", () -> new SimpleParticleType(false));
    /** Gold-white spark of a slayer's resolve (Unyielding and other last-stand effects). */
    public static final RegistryObject<SimpleParticleType> RESOLVE_SPARK = PARTICLES.register("resolve_spark", () -> new SimpleParticleType(true));
    /** A small ember rising from a lit stone lantern. */
    public static final RegistryObject<SimpleParticleType> LANTERN_EMBER = PARTICLES.register("lantern_ember", () -> new SimpleParticleType(false));

    /** Lavender incense smoke curling upward. */
    public static final RegistryObject<SimpleParticleType> INCENSE_SMOKE = PARTICLES.register("incense_smoke", () -> new SimpleParticleType(false));

    /** Thick red-orange smoke rising high: someone in trouble (a cornered candidate). */
    public static final RegistryObject<SimpleParticleType> DISTRESS_SMOKE = PARTICLES.register("distress_smoke", () -> new SimpleParticleType(true));

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
