package com.wassuples.demonslayer.forms.demon.hantengu;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

public class CrazedCryOfThunderdeathForm extends TimedForm {
    private static final int WIND_UP_TICKS = 60;
    private static final int LIGHTNING_START_TICKS = 80;
    private static final int LIGHTNING_TICK_RATE = 5;
    private static final double ARENA_RADIUS = 25.0;

    private boolean soundWaveFired = false;
    private final Vec3 arenaCenter;

    public CrazedCryOfThunderdeathForm(LivingEntity user, IForm form) {
        super(user, form, WIND_UP_TICKS + LIGHTNING_START_TICKS + 100); // 60 + 80 + 100 = 240 ticks total or 12 seconds
        this.arenaCenter = user.position().add(user.getLookAngle().scale(ARENA_RADIUS));
    }

    @Override
    public void onStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (user.isCrouching()) {
            ActiveFormManager.stop(user);
            return;
        }

        final DustParticleOptions woodParticle = new DustParticleOptions(new Vector3f(0.4f, 0.2f, 0.1f), 1.0f);
        double angle1 = (ticksActive * 0.05);
        Vec3 dragon1Pos = arenaCenter.add(Math.cos(angle1) * ARENA_RADIUS, 10, Math.sin(angle1) * ARENA_RADIUS);
        level.sendParticles(woodParticle, dragon1Pos.x, dragon1Pos.y, dragon1Pos.z, 10, 1, 1, 1, 0);

        double angle2 = (ticksActive * 0.05) + Math.PI;
        Vec3 dragon2Pos = arenaCenter.add(Math.cos(angle2) * ARENA_RADIUS, 10, Math.sin(angle2) * ARENA_RADIUS);
        level.sendParticles(woodParticle, dragon2Pos.x, dragon2Pos.y, dragon2Pos.z, 10, 1, 1, 1, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, dragon2Pos.x, dragon2Pos.y, dragon2Pos.z, 5, 0.5, 0.5, 0.5, 0.1);

        if (!soundWaveFired && ticksActive >= WIND_UP_TICKS) {
            soundWaveFired = true;
            level.playSound(null, arenaCenter.x, arenaCenter.y, arenaCenter.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.8f);
            level.sendParticles(ParticleTypes.SONIC_BOOM, arenaCenter.x, arenaCenter.y, arenaCenter.z, 1, 0, 0, 0, 0);

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(arenaCenter, arenaCenter).inflate(ARENA_RADIUS));
            for (LivingEntity target : targets) {
                if (target != user) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2));
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                }
            }
        }

        if (soundWaveFired && ticksActive >= LIGHTNING_START_TICKS) {
            if (ticksActive % LIGHTNING_TICK_RATE == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(arenaCenter, arenaCenter).inflate(ARENA_RADIUS),
                        e -> e != user);

                if (!targets.isEmpty()) {
                    LivingEntity randomTarget = targets.get(level.random.nextInt(targets.size()));
                    EntityType.LIGHTNING_BOLT.spawn(level, null, null, randomTarget.blockPosition(), MobSpawnType.TRIGGERED, true, false);
                }
            }
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
