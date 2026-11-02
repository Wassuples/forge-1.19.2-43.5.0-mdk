package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonPieEntity;
import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PieSplatS2CPacket;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public class PieSplatForm extends ParryForm {
    private static final int PARRY_WINDOW = 12;

    public PieSplatForm(LivingEntity user, IForm formType) {
        super(user, formType, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        FormAnimations.play(user, FormAnimations.TOON_PIE_SPLAT);
        level.playSound(null, user.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 1.4f);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        // Arrows/fireballs: the pie goes to whoever fired them.
        Entity source = attacker instanceof Projectile projectile && projectile.getOwner() != null
                ? projectile.getOwner() : attacker;
        if (!(source instanceof LivingEntity living) || living == user) {
            return;
        }

        // The visible pie flies into their face; impact (sound, SPLAT!, overlay, blinding) happens in ToonPieEntity.
        level.addFreshEntity(new ToonPieEntity(level, user, living));
        ToonForceHelper.tryApplyDizzy(living, user, 0.35f);
        FormAnimations.play(living, FormAnimations.TOON_BONKED);
        level.playSound(null, user.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.8f);
    }
}


