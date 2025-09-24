package com.wassuples.demonslayer.forms.slayer.wind;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnCycloneParticles;

public class ColdMountainWindForm extends TimedForm {
    private static final DustParticleOptions ICE_DUST = new DustParticleOptions(new Vector3f(0.7f, 0.9f, 1.0f), 1.0f);

    public ColdMountainWindForm(LivingEntity user, IForm form) {
        super(user, form, 60);
    }

    @Override
    public void onStart() {
        user.setDeltaMovement(user.getDeltaMovement().add(0, 2.5, 0));
        user.hurtMarked = true;
        performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        Vec3 stormCenter = user.position();
        double radius = 5.0;
        double height = 15.0;

        AABB damageZone = new AABB(stormCenter.x - radius, stormCenter.y - height, stormCenter.z - radius,
                stormCenter.x + radius, stormCenter.y, stormCenter.z + radius);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());
        float damagePerTick = 1.5f;

        for (LivingEntity target : targets) {
            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), damagePerTick);
            } else {
                target.hurt(DamageSource.mobAttack(user), damagePerTick);
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            target.push(0, -0.8, 0);
        }

        if (ticksActive % 10 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        spawnCycloneParticles(level, ICE_DUST, ParticleTypes.SNOWFLAKE, user.position().add(0, -height / 2.0, 0), radius, height, 25, -ticksActive);
    }
}
