package com.wassuples.demonslayer.forms.demon.enmu;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.StartSleepQTES2CPacket;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class WhispersHypnosisForm extends ActiveForm {
    private static final int SLEEP_DURATION = 1200;
    private static final double MAX_RANGE = 16.0;

    public WhispersHypnosisForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        LivingEntity target = getLookedAtEntity(user, MAX_RANGE);
        if (target != null) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);

            target.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), SLEEP_DURATION, 0));
            ForcedSleepEffect.setCaster(target, user);

            final DustParticleOptions whisperParticle = new DustParticleOptions(new Vector3f(0.2f, 0.1f, 0.3f), 1.0f);
            spawnLineParticles(level, whisperParticle, user.getEyePosition(), target.getEyePosition(), 15);

            if (target instanceof ServerPlayer sleepingPlayer) {
                List<Integer> keys = new ArrayList<>(List.of(0, 1, 2, 3)); // 0=up, 1=left, 2=down, 3=right
                Collections.shuffle(keys);
                ModMessages.sendToPlayer(new StartSleepQTES2CPacket(keys), sleepingPlayer);
            }
        } else {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
