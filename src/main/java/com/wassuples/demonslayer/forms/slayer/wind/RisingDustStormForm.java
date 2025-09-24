package com.wassuples.demonslayer.forms.slayer.wind;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnCycloneParticles;

public class RisingDustStormForm extends TimedForm {
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 1.5f);

    public RisingDustStormForm(LivingEntity user, IForm form) {
        super(user, form, 80);
    }

    @Override
    public void onStart() {
        user.setDeltaMovement(user.getDeltaMovement().add(0, -2.5, 0));
        user.hurtMarked = true;
        performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();
        BlockPos stormCenter = user.blockPosition();
        double radius = 4.0;
        double height = 15.0;
        AABB damageZone = new AABB(stormCenter).expandTowards(0, height, 0).inflate(radius, 0, radius);

        if (ticksActive % 10 == 0) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());
            for (LivingEntity target : targets) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), 6.0f);
                } else {
                    target.hurt(DamageSource.mobAttack(user), 6.0f);
                }
                Vec3 pullDir = new Vec3(stormCenter.getX() + 0.5 - target.getX(), 2.0, stormCenter.getZ() + 0.5 - target.getZ()).normalize();
                target.push(pullDir.x * 0.2, pullDir.y * 1.2, pullDir.z * 0.2);
            }
        }
        spawnCycloneParticles(level, ParticleTypes.CLOUD, GREEN_DUST, Vec3.atCenterOf(stormCenter), radius, height, 20, ticksActive);
    }
}