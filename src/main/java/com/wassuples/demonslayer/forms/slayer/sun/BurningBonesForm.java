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
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class BurningBonesForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double TRAVEL_SPEED = 0.5;
    private static final double VORTEX_RADIUS = 3.0;
    private static final float DAMAGE_PER_TICK = 4.0f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    private final Vec3 direction;
    private Vec3 vortexCenter;

    public BurningBonesForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.direction = user.getLookAngle();
        this.vortexCenter = user.getEyePosition().add(direction.scale(1.5));
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        this.vortexCenter = this.vortexCenter.add(this.direction.scale(TRAVEL_SPEED));

        AABB defenseBox = new AABB(this.vortexCenter, this.vortexCenter).inflate(VORTEX_RADIUS);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, defenseBox,
                p -> p.getOwner() != user && !(p.getOwner() instanceof TamableAnimal tamable && tamable.getOwner() == user));

        for (Projectile projectile : projectiles) {
            level.playSound(null, projectile.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.sendParticles(ParticleTypes.FLAME, projectile.getX(), projectile.getY(), projectile.getZ(), 20, 0.1, 0.1, 0.1, 0.1);
            projectile.discard();
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, defenseBox, e -> e != user && e.isAlive());
        for (LivingEntity target : targets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player).setIsFire(), DAMAGE_PER_TICK);
            } else {
                target.hurt(DamageSource.mobAttack(user).setIsFire(), DAMAGE_PER_TICK);
            }
            target.setDeltaMovement(this.direction.scale(TRAVEL_SPEED * 1.1));
            target.hurtMarked = true;
        }

        float rotation = this.ticksActive * 30;
        spawnParticleArcInWorld(level, this.vortexCenter, VORTEX_RADIUS, 360, 30, user.getYRot(), 90 + rotation, RED_HOT);
        spawnParticleArcInWorld(level, this.vortexCenter, VORTEX_RADIUS, 360, 30, user.getYRot(), 90 - rotation, ParticleTypes.LAVA);
    }
}
