package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.*;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Thunder Breathing form: Honoikazuchi No Kami.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class HonoikazuchiNoKamiForm extends TimedForm {
    private static final int CHARGE_PAUSE_DURATION = 15;
    private static final int DASH_DURATION_TICKS = 10;
    private static final double DASH_SPEED = 5.0;
    private static final float DASH_DAMAGE = 10.0f;
    private static final float FINAL_ATTACK_DAMAGE = 40.0f;
    private static final DustParticleOptions DRAGON_PARTICLE = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.4f), 1.5f);

    private final Vec3 dashDirection;

    public HonoikazuchiNoKamiForm(LivingEntity user, IForm form) {
        super(user, form, CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS + 1);
        this.dashDirection = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        // More epic, intimidating sound effects for an ultimate move
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        applyImmobility(user, this.duration + 10);
    }

    private void performSlash() {
        user.setDeltaMovement(Vec3.ZERO);
        performGenericAttack(user, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f);
        dealThrustDamage(user, 6.0, FINAL_ATTACK_DAMAGE);
        spawnParticleArc(user, DRAGON_PARTICLE, 4.0, 160, 60);
        spawnParticleArc(user, ParticleTypes.ELECTRIC_SPARK, 4.5, 180, 20);
    }

    @Override
    public void onTick() {
        if (ticksActive < CHARGE_PAUSE_DURATION) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;

            for (int i = 0; i < 10; i++) {
                double angle = (ticksActive * 0.5) + (i * (Math.PI / 5.0));
                double radius = 2.5 - (ticksActive / (double) CHARGE_PAUSE_DURATION * 2.5); // Radius shrinks to zero
                Vec3 offset = new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                Vec3 particlePos = user.position().add(offset).add(0, user.getBbHeight() / 2.0, 0);
                level.sendParticles(DRAGON_PARTICLE, particlePos.x, particlePos.y, particlePos.z, 1, -offset.x * 0.1, -offset.y * 0.1, -offset.z * 0.1, 0);
            }
        } else if (ticksActive < CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS) {
            if (user.isCrouching()) {
                performSlash();
                ActiveFormManager.stop(user);
                return;
            }

            Vec3 newPos = user.position().add(this.dashDirection.scale(DASH_SPEED));
            user.teleportTo(newPos.x, newPos.y, newPos.z);
            user.hurtMarked = true;

            dealAOEDamage(user, user.position(), 2.0, DASH_DAMAGE, 0.2f);

            Vec3 sideVector = dashDirection.cross(new Vec3(0, 1, 0));
            Vec3 upVector = new Vec3(0, 1, 0);

            // The "Head" of the dragon
            Vec3 headPos = user.getEyePosition().add(dashDirection.scale(1.5));
            level.sendParticles(DRAGON_PARTICLE, headPos.x, headPos.y, headPos.z, 10, 0.2, 0.2, 0.2, 0.1);

            // The "Body" of the dragon
            for (int i = 0; i < 3; i++) {
                double serpentAngle = (ticksActive * 1.5) + (i * Math.PI / 1.5);
                double serpentRadius = 1.2;
                Vec3 offset = upVector.scale(Math.sin(serpentAngle) * serpentRadius)
                        .add(sideVector.scale(Math.cos(serpentAngle) * serpentRadius));

                // Spawn particles behind the player to create the tail
                Vec3 particlePos = user.position().add(offset).add(0, 1, 0).subtract(dashDirection.scale(i * 0.5));
                level.sendParticles(DRAGON_PARTICLE, particlePos.x, particlePos.y, particlePos.z, 2, 0, 0, 0, 0);
            }
        } else {
            performSlash();
            ActiveFormManager.stop(user);
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}
