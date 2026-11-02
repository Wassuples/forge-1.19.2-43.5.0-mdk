package com.wassuples.demonslayer.client.particle;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client factories for {@link ModParticles}. */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModParticleProviders {
    private ModParticleProviders() {
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.register(ModParticles.WISTERIA_PETAL.get(), PetalParticle.Provider::new);
        event.register(ModParticles.DEMON_ASH.get(), AshParticle.Provider::new);
        event.register(ModParticles.RESOLVE_SPARK.get(), SparkParticle.Provider::new);
        event.register(ModParticles.LANTERN_EMBER.get(), EmberParticle.Provider::new);
        event.register(ModParticles.INCENSE_SMOKE.get(), AshParticle.Provider::new);
        event.register(ModParticles.DISTRESS_SMOKE.get(), DistressParticle.Provider::new);   // drifts up and thins out the same way
    }

    /** Big, slow, long-lived smoke puff that climbs high and spreads (a distress column visible from afar). */
    static final class DistressParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        DistressParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            this.lifetime = 80 + random.nextInt(40);
            this.gravity = -0.03F;
            this.friction = 0.97F;
            this.quadSize = 0.35F + random.nextFloat() * 0.25F;
            this.xd = (random.nextDouble() - 0.5) * 0.02;
            this.yd = 0.08;
            this.zd = (random.nextDouble() - 0.5) * 0.02;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            quadSize *= 1.01F;
            alpha = 0.85F * (1.0F - (float) age / lifetime);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                         double dx, double dy, double dz) {
                return new DistressParticle(level, x, y, z, sprites);
            }
        }
    }

    /** Tumbles down on a lazy side-to-side drift, spinning, and settles briefly on the ground before fading. */
    static final class PetalParticle extends TextureSheetParticle {
        private final float spin;
        private final double swayPhase, swayRate;

        PetalParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.lifetime = 90 + random.nextInt(60);
            this.gravity = 0.012F;
            this.quadSize = 0.07F + random.nextFloat() * 0.04F;
            this.spin = (random.nextFloat() - 0.5F) * 0.18F;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            this.swayPhase = random.nextDouble() * Math.PI * 2;
            this.swayRate = 0.06 + random.nextDouble() * 0.05;
            this.xd = (random.nextDouble() - 0.5) * 0.02;
            this.zd = (random.nextDouble() - 0.5) * 0.02;
            this.yd = -0.01;
            float tint = 0.9F + random.nextFloat() * 0.1F;
            setColor(tint, tint, tint);
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) {
                xd = zd = 0;
                oRoll = roll;
            } else {
                oRoll = roll;
                roll += spin;
                double sway = Math.sin(swayPhase + age * swayRate) * 0.012;
                xd += sway * 0.25;
                zd += Math.cos(swayPhase + age * swayRate) * 0.003;
                yd = Math.max(yd, -0.045);
            }
            if (age > lifetime - 20) alpha = (lifetime - age) / 20.0F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                         double dx, double dy, double dz) {
                return new PetalParticle(level, x, y, z, sprites);
            }
        }
    }

    /** Flakes rise and scatter on the given velocity, darkening and shrinking as they burn away. */
    static final class AshParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        AshParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            this.lifetime = 30 + random.nextInt(30);
            this.gravity = -0.015F;
            this.friction = 0.92F;
            this.quadSize = 0.08F + random.nextFloat() * 0.08F;
            this.xd = dx + (random.nextDouble() - 0.5) * 0.04;
            this.yd = dy + random.nextDouble() * 0.03;
            this.zd = dz + (random.nextDouble() - 0.5) * 0.04;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float f = (float) age / lifetime;
            alpha = 1.0F - f * f;
            quadSize *= 0.985F;
            oRoll = roll;
            roll += 0.05F;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                         double dx, double dy, double dz) {
                return new AshParticle(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    /** Bright, full-bright spark that bursts outward and fades quickly. */
    static final class SparkParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        SparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            this.sprites = sprites;
            this.lifetime = 12 + random.nextInt(10);
            this.friction = 0.86F;
            this.gravity = 0.0F;
            this.quadSize = 0.06F + random.nextFloat() * 0.05F;
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            alpha = 1.0F - (float) age / lifetime;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                         double dx, double dy, double dz) {
                return new SparkParticle(level, x, y, z, dx, dy, dz, sprites);
            }
        }
    }

    /** A tiny ember that wobbles upward out of a lantern window and winks out. */
    static final class EmberParticle extends TextureSheetParticle {
        EmberParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            this.lifetime = 16 + random.nextInt(14);
            this.gravity = -0.01F;
            this.quadSize = 0.03F + random.nextFloat() * 0.02F;
            this.xd = (random.nextDouble() - 0.5) * 0.01;
            this.yd = 0.01;
            this.zd = (random.nextDouble() - 0.5) * 0.01;
        }

        @Override
        public void tick() {
            super.tick();
            xd += (random.nextDouble() - 0.5) * 0.004;
            zd += (random.nextDouble() - 0.5) * 0.004;
            alpha = 1.0F - (float) age / lifetime;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                         double dx, double dy, double dz) {
                return new EmberParticle(level, x, y, z, sprites);
            }
        }
    }
}
