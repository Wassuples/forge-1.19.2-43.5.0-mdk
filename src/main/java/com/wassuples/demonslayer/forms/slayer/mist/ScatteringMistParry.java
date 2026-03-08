package com.wassuples.demonslayer.forms.slayer.mist;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Mist Breathing form: Scattering Mist Parry.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.ParryForm
 */
public class ScatteringMistParry extends ParryForm {
    private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

    public ScatteringMistParry(LivingEntity user, IForm form) {
        super(user, form, 30); // A 1-second parry window
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        // --- Offensive Circular Slash ---
        performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        // Damage and strongly knock back nearby enemies
        dealAOEDamage(user, user.position(), 4.0, 7.0f, 1.5f);
        // Create the 360-degree mist visual
        spawnParticleArc(user, DENSE_MIST, 3.5, 360, 40);
        spawnParticleArc(user, ParticleTypes.CLOUD, 4.0, 360, 40);

        // --- Defensive Projectile Deflection ---
        // Find all projectiles in a 5-block radius
        AABB deflectionBox = user.getBoundingBox().inflate(5.0);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, deflectionBox);

        for (Projectile projectile : projectiles) {
            // Change the projectile's owner to the player
            projectile.setOwner(user);
            // Calculate a vector pointing away from the player
            Vec3 awayVector = projectile.position().subtract(user.getEyePosition()).normalize();
            // Give the projectile a strong push outwards
            projectile.setDeltaMovement(awayVector.scale(1.5));
        }
    }
}
