package com.wassuples.demonslayer.forms.demon.akaza;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class ExplosiveFlurryForm extends TimedForm {
    private static final int NUMBER_OF_KICKS = 5;
    private static final int TICKS_BETWEEN_KICKS = 3;
    private static final float DAMAGE_PER_KICK = 10.0f;

    private static final float FINAL_KICK_DAMAGE = 25.0f;
    private static final float FINAL_KICK_KNOCKBACK = 3.5f;

    private static final DustParticleOptions AFTERIMAGE_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.6f, 1.0f), 0.8f);

    public ExplosiveFlurryForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_KICKS * TICKS_BETWEEN_KICKS);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_KICKS == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.3f, 2.0f);

            dealDirectionalDamage(user, 4.0, DAMAGE_PER_KICK, 0.2);

            spawnParticleArc(user, ParticleTypes.SONIC_BOOM, 1.0, 60, 1);
            spawnAfterimageVFX();
        }
    }

    @Override
    public void onStop() {
        LivingEntity target = getLookedAtEntity(user, 4.5);
        if (target != null) {
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getEyeY(), target.getZ(), 1, 0, 0, 0, 0);

            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), FINAL_KICK_DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), FINAL_KICK_DAMAGE);
            }

            Vec3 launchVec = user.getLookAngle().scale(FINAL_KICK_KNOCKBACK);
            target.setDeltaMovement(launchVec.x, 0.8, launchVec.z);
            target.hurtMarked = true;
        }
    }

    private void spawnAfterimageVFX() {
        Vec3 pos = user.position();
        for (double y = 0; y <= user.getBbHeight(); y += 0.2) {
            for (int i = 0; i < 4; i++) {
                level.sendParticles(AFTERIMAGE_PARTICLE,
                        pos.x + (level.random.nextDouble() - 0.5) * user.getBbWidth(),
                        pos.y + y,
                        pos.z + (level.random.nextDouble() - 0.5) * user.getBbWidth(),
                        1, 0, 0, 0, 0);
            }
        }
    }
}
