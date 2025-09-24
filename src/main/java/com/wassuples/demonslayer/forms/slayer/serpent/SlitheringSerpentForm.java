package com.wassuples.demonslayer.forms.slayer.serpent;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

public class SlitheringSerpentForm extends TimedForm {
    private static final int DURATION = 30; // 1.5 second dash
    private static final double DASH_DISTANCE = 16.0;
    private static final double COIL_START_RADIUS = 3.0;
    private static final float COIL_ROTATION_SPEED = 30.0f;
    private static final float DAMAGE_PER_TICK = 4.0f;
    private static final double DAMAGE_RADIUS = 4.0;

    private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);

    private final Vec3 startPos;
    private final Vec3 direction;
    private float currentAngle;

    public SlitheringSerpentForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.startPos = user.position();
        this.direction = user.getLookAngle();
        this.currentAngle = user.getYRot();
    }

    @Override
    public void onTick() {
        super.onTick();
        double progress = (double) ticksActive / DURATION;
        Vec3 forwardPos = this.startPos.add(this.direction.scale(progress * DASH_DISTANCE));
        double currentCoilRadius = COIL_START_RADIUS * (1.0 - progress);
        this.currentAngle += COIL_ROTATION_SPEED;
        double angleRad = Math.toRadians(this.currentAngle);
        Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 offset = sideDir.scale(Math.cos(angleRad) * currentCoilRadius);
        Vec3 currentCurvePos = forwardPos.add(offset);
        user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

        double nextProgress = Math.min((double) (ticksActive + 1) / DURATION, 1.0);
        Vec3 nextForwardPos = this.startPos.add(this.direction.scale(nextProgress * DASH_DISTANCE));
        double nextCoilRadius = COIL_START_RADIUS * (1.0 - nextProgress);
        double nextAngleRad = Math.toRadians(this.currentAngle + COIL_ROTATION_SPEED);
        Vec3 nextOffset = sideDir.scale(Math.cos(nextAngleRad) * nextCoilRadius);
        Vec3 nextCurvePos = nextForwardPos.add(nextOffset);

        Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);
        user.setDeltaMovement(velocityHint);
        user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

        dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f);
        level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
        if (ticksActive % 5 == 0) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getEyeY(), user.getZ(), 2, 1.5, 0.1, 1.5, 0.0);
        }
    }
}
