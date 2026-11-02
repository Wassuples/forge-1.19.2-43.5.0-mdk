package com.wassuples.demonslayer.forms.demon.hantengu;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.custom.HantenguDrumsEntity;
import com.wassuples.demonslayer.entity.custom.ThunderDragonsEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.util.FormAnimations;
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

import javax.annotation.Nullable;
import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

/**
 * Hantengu Blood Demon Art form: Crazed Cry Of Thunderdeath.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CrazedCryOfThunderdeathForm extends TimedForm {
    private static final int WIND_UP_TICKS = 60;
    private static final int LIGHTNING_START_TICKS = 80;
    private static final int LIGHTNING_TICK_RATE = 5;
    private static final double ARENA_RADIUS = 25.0;

    private static final int DURATION = WIND_UP_TICKS + LIGHTNING_START_TICKS + 100; // 240 ticks = 12 seconds

    private boolean soundWaveFired = false;
    private final Vec3 arenaCenter;
    @Nullable
    private ThunderDragonsEntity dragons;
    @Nullable
    private HantenguDrumsEntity drums;

    public CrazedCryOfThunderdeathForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.arenaCenter = user.position().add(user.getLookAngle().scale(ARENA_RADIUS));
    }

    @Override
    public void onStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 1.2f);
        // Two wooden dragons circling the arena on the same path the old particle markers used.
        this.dragons = new ThunderDragonsEntity(level, user, arenaCenter, (float) ARENA_RADIUS, DURATION);
        level.addFreshEntity(this.dragons);
        this.drums = new HantenguDrumsEntity(level, user, DURATION);
        level.addFreshEntity(this.drums);
        this.drums.beat(); // "Zohakuten strikes a drum, commanding two wooden dragons..."
        FormAnimations.play(user, FormAnimations.HANTENGU_CRY);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (user.isCrouching()) {
            ActiveFormManager.stop(user);
            return;
        }

        // The circling dragons are drawn by ThunderDragonsEntity; the drums boom through the wind-up.
        if (drums != null && ticksActive < WIND_UP_TICKS && ticksActive % 6 == 4) {
            drums.beat();
        }

        if (!soundWaveFired && ticksActive >= WIND_UP_TICKS) {
            soundWaveFired = true;
            level.playSound(null, arenaCenter.x, arenaCenter.y, arenaCenter.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.8f);

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
                    if (dragons != null) dragons.bolt(randomTarget.position());
                }
            }
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        if (dragons != null) {
            dragons.discard();
            dragons = null;
        }
        if (drums != null) {
            drums.discard();
            drums = null;
        }
        FormAnimations.play(user, FormAnimations.HANTENGU_END);
    }
}
