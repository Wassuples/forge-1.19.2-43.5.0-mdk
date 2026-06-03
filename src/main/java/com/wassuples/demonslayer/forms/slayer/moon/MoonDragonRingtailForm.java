package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * Moon Breathing form: Moon Dragon Ringtail.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class MoonDragonRingtailForm extends TimedForm {
    private static final int DURATION = 40;
    private static final double MAX_RANGE = 35.0;
    private static final double SLASH_WIDTH = 4.0;
    private static final float DAMAGE = 45.0f;
    private static final float KNOCKBACK = 1.2f;

    // Moon8: The ONLY lateral offset is driven by the player's camera. No autonomous weaving.
    private static final double CAMERA_CURVE_AMPLITUDE = 22.0;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.5f);

    private final Vec3 startPos;
    private final Vec3 startDirection;
    private final Vec3 sideVector;
    private final float startYaw;
    private final Set<UUID> hitEntities = new HashSet<>();
    private Vec3 lastSlashCenter;

    public MoonDragonRingtailForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.startPos = user.position();
        this.startDirection = horizontalLook(user);
        this.sideVector = this.startDirection.cross(new Vec3(0, 1, 0)).normalize();
        this.startYaw = user.getYRot();
        this.lastSlashCenter = this.startPos;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_HURT, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        Vec3 forwardPoint = this.startPos.add(this.startDirection.scale(progress * MAX_RANGE));

        // Camera-steered curve: the slash drifts left/right by however far the player has
        // turned their camera since the form started. The offset is purely proportional to the
        // current yaw delta — no progress multiplier — so it holds a constant curve when the
        // camera is still and responds immediately when the player turns.
        double yawDelta = Mth.wrapDegrees(user.getYRot() - this.startYaw);
        double cameraCurve = Mth.clamp(yawDelta / 90.0, -1.0, 1.0) * CAMERA_CURVE_AMPLITUDE;
        Vec3 slashCenter = forwardPoint.add(this.sideVector.scale(cameraCurve));
        Vec3 travelDirection = slashCenter.subtract(this.lastSlashCenter);
        if (travelDirection.lengthSqr() < 0.0001) {
            travelDirection = this.startDirection;
        } else {
            travelDirection = travelDirection.normalize();
        }
        this.lastSlashCenter = slashCenter;

        AABB damageBox = new AABB(slashCenter, slashCenter).inflate(SLASH_WIDTH, SLASH_WIDTH, SLASH_WIDTH);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageBox,
                e -> e != user && e.isAlive() && !this.hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
            }
            target.knockback(KNOCKBACK, -travelDirection.x, -travelDirection.z);
            this.hitEntities.add(target.getUUID());
        }
        spawnParticleArcInWorld(level, slashCenter.add(0, 2.0, 0), 4.0, 180, 40, user.getYRot(), 90, PURPLE_SLASH);
    }

    private static Vec3 horizontalLook(LivingEntity user) {
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 0.0001) {
            return Vec3.directionFromRotation(0.0f, user.getYRot());
        }
        return horizontal.normalize();
    }
}
