package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PieSplatS2CPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class PieSplatForm extends ParryForm {
    private static final int PARRY_WINDOW = 12;

    public PieSplatForm(LivingEntity user, IForm formType) {
        super(user, formType, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        FormAnimations.play(user, FormAnimations.TOON_PIE_SPLAT);
        level.playSound(null, user.blockPosition(), ModSounds.TOON_SPLAT.get(), SoundSource.PLAYERS, 0.8f, 1.45f);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        if (attacker instanceof LivingEntity living) {
            ToonForceHelper.tryApplyDizzy(living, user, 0.35f);
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, living.getX(), living.getEyeY(), living.getZ(), 30, 0.45, 0.35, 0.45, 0.06);
            level.playSound(null, living.blockPosition(), ModSounds.TOON_SPLAT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            FormAnimations.play(living, FormAnimations.TOON_BONKED);

            if (living instanceof ServerPlayer player) {
                ModMessages.sendToPlayer(new PieSplatS2CPacket(90), player);
            }
        }
    }
}

