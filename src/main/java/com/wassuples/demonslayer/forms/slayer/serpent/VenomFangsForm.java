package com.wassuples.demonslayer.forms.slayer.serpent;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;

public class VenomFangsForm extends TimedForm {
    private static final int DURATION = 12; // A very short duration for a quick dash/attack
    private static final double DASH_SPEED = 2.5;
    private static final int SLASH_DELAY = 3; // Slash happens 3 ticks after teleporting
    private static final float STAB_DAMAGE = 8.0f;

    private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.8f), 1.0f);

    @Nullable
    private final LivingEntity target;

    public VenomFangsForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.target = getLookedAtEntity(user, 12.0);
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public void onStart() {
        if (this.target != null) {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.sendParticles(ParticleTypes.POOF, user.getX(), user.getY(), user.getZ(), 20, 0.3, 0.5, 0.3, 0.2);

            Vec3 teleportPos = target.position().add(target.getLookAngle().scale(-2.0));
            user.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
            user.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        } else {
            Vec3 dashVec = user.getLookAngle().scale(DASH_SPEED);
            user.setDeltaMovement(dashVec);
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.target != null) {
            if (!this.target.isAlive()) {
                ActiveFormManager.stop(user);
                return;
            }

            if (ticksActive == SLASH_DELAY) {
                performGenericAttack(user, SoundEvents.BEE_STING, 1.2f, 1.0f);
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), STAB_DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), STAB_DAMAGE);
                }
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
            }
        } else {
            level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0);
        }
    }
}
