package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class RagingSunForm extends TimedForm {
    private static final int DURATION = 20;
    private static final int FIRST_SLASH_TICK = 2;
    private static final int SECOND_SLASH_TICK = 8;

    private static final float SLASH_DAMAGE = 25.0f;
    private static final float RETALIATION_DAMAGE = 50.0f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    public RagingSunForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive == FIRST_SLASH_TICK) {
            performSlash(4.0);
        }
        if (ticksActive == SECOND_SLASH_TICK) {
            performSlash(7.0);
        }

        AABB defenseBox = user.getBoundingBox().inflate(3.0);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, defenseBox,
                p -> p.getOwner() != user && !(p.getOwner() instanceof TamableAnimal tamable && tamable.getOwner() == user));

        for (Projectile projectile : projectiles) {
            level.playSound(null, projectile.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.sendParticles(ParticleTypes.FLAME, projectile.getX(), projectile.getY(), projectile.getZ(), 20, 0.1, 0.1, 0.1, 0.1);
            projectile.discard();
        }
    }

    private void performSlash(double radius) {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
        dealAOEDamage(user, user.position(), radius, SLASH_DAMAGE, 0.8f, true, false);
        spawnParticleArcInWorld(level, user.position().add(0, 1.0, 0), radius, 360, (int) (radius * 10), 0, 0, RED_HOT);
    }

    @Override
    public void onHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            attacker.hurt(DamageSource.playerAttack((Player) user).setIsFire(), RETALIATION_DAMAGE);
            attacker.setSecondsOnFire(10);
            level.playSound(null, attacker.blockPosition(), SoundEvents.GENERIC_BURN, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.sendParticles(ParticleTypes.LAVA, attacker.getX(), attacker.getEyeY(), attacker.getZ(), 30, 0.4, 0.4, 0.4, 1.0);
        }
    }
}
