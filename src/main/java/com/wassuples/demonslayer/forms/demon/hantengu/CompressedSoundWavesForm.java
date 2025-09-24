package com.wassuples.demonslayer.forms.demon.hantengu;

import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.SpawnSonicBeamS2CPacket;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class CompressedSoundWavesForm extends ActiveForm {
    private static final double MAX_RANGE = 30.0;
    private static final int KNOCKOUT_DURATION = 140; // 7 seconds

    public CompressedSoundWavesForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        Vec3 start = user.getEyePosition();
        Vec3 end = start.add(user.getLookAngle().scale(MAX_RANGE));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, user, start, end, new AABB(start, end), e -> e instanceof LivingEntity && !e.is(user));
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.2f);

        if (entityHit != null) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            target.hurt(DamageSource.sonicBoom(user), 25.0F);
            target.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), KNOCKOUT_DURATION, 0));
            ForcedSleepEffect.setCaster(target, user);
            ModMessages.sendToClients(new SpawnSonicBeamS2CPacket(start, entityHit.getLocation()));
        } else {
            ModMessages.sendToClients(new SpawnSonicBeamS2CPacket(start, end));
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
