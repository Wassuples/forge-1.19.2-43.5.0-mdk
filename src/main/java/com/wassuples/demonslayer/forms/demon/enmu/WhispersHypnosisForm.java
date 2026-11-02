package com.wassuples.demonslayer.forms.demon.enmu;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.custom.WhisperWaveEntity;
import com.wassuples.demonslayer.util.FormAnimations;
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

/**
 * Enmu Blood Demon Art form: Whispers Hypnosis.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
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
        FormAnimations.play(user, FormAnimations.ENMU_WHISPER);
        if (target != null) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);
            // The whisper travels visibly from the mouth on Enmu's hand; the sleep (and the QTE for players)
            // is applied by the wave when it reaches the target.
            level.addFreshEntity(new WhisperWaveEntity(level, user, target, SLEEP_DURATION));
        } else {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
    }
}
