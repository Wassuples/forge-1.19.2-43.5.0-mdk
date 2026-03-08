package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Drilling Slashes.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class DrillingSlashesForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double MAX_RANGE = 30.0;
    private static final double SAW_RADIUS = 2.0;
    private static final float DAMAGE_PER_TICK = 5.0f;
    private static final double SPREAD = 2.5;

    private static final DustParticleOptions PURPLE_SAW = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);

    private final Vec3 startPos;
    private final Vec3 direction;
    private final Vec3 sideVector;
    private final Set<UUID> hitEntitiesThisTick = new HashSet<>();

    public DrillingSlashesForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.startPos = user.getEyePosition();
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        this.sideVector = this.direction.cross(new Vec3(0, 1, 0)).normalize();
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();
        this.hitEntitiesThisTick.clear();

        double progress = (double) this.ticksActive / DURATION;
        double currentDistance = MAX_RANGE * progress;
        Vec3 forwardPoint = this.startPos.add(this.direction.scale(currentDistance));

        Vec3 centerSawPos = forwardPoint;
        Vec3 leftSawPos = forwardPoint.add(this.sideVector.scale(SPREAD));
        Vec3 rightSawPos = forwardPoint.add(this.sideVector.scale(-SPREAD));

        processSaw(centerSawPos, this.ticksActive * 20);
        processSaw(leftSawPos, this.ticksActive * 20 + 120);
        processSaw(rightSawPos, this.ticksActive * 20 - 120);
    }

    private void processSaw(Vec3 center, float rotation) {
        AABB damageBox = new AABB(center, center).inflate(SAW_RADIUS);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageBox,
                e -> e != user && e.isAlive() && !this.hitEntitiesThisTick.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
            }
            this.hitEntitiesThisTick.add(target.getUUID());
        }
        spawnParticleArcInWorld(level, center, SAW_RADIUS, 360, 20, user.getYRot(), 90, PURPLE_SAW);
        spawnParticleArcInWorld(level, center, SAW_RADIUS, 360, 10, user.getYRot(), 90, ParticleTypes.CRIT);
    }
}
