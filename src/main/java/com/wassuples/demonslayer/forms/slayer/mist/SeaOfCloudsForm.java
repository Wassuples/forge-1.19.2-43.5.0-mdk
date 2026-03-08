package com.wassuples.demonslayer.forms.slayer.mist;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BlindingMistCloudEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

/**
 * Mist Breathing form: Sea Of Clouds.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SeaOfCloudsForm extends TimedForm {
    private static final int DASH_DURATION = 20; // A slighly shorter dash
    private static final double DASH_DISTANCE = 15.0; // The distance for the dash
    private static final double ZIGZAG_AMPLITUDE = 2.0;
    private static final double ZIGZAG_FREQUENCY = 10.0;
    private static final float DAMAGE_PER_TICK = 1.5f;
    private static final double DAMAGE_RADIUS = 2.5;

    private final Vec3 startPos;
    private final Vec3 endPos;

    public SeaOfCloudsForm(LivingEntity user, IForm form) {
        super(user, form, DASH_DURATION);
        this.startPos = user.position();
        this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double dashProgress = Math.min((double) ticksActive / DASH_DURATION, 1.0);

        Vec3 forwardDir = endPos.subtract(startPos).normalize();
        Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 straightPos = startPos.lerp(endPos, dashProgress);
        double zigzagOffset = ZIGZAG_AMPLITUDE * Math.signum(Math.sin(dashProgress * Math.PI * ZIGZAG_FREQUENCY));
        Vec3 currentCurvePos = straightPos.add(sideDir.scale(zigzagOffset));

        user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

        double nextDashProgress = Math.min((double) (ticksActive + 1) / DASH_DURATION, 1.0);
        Vec3 nextStraightPos = startPos.lerp(endPos, nextDashProgress);
        double nextZigzagOffset = ZIGZAG_AMPLITUDE * Math.signum(Math.sin(nextDashProgress * Math.PI * ZIGZAG_FREQUENCY));
        Vec3 nextCurvePos = nextStraightPos.add(sideDir.scale(nextZigzagOffset));
        Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);

        user.setDeltaMovement(velocityHint);
        user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

        dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f);

        if (ticksActive > 0 && ticksActive % 2 == 0) {
            BlindingMistCloudEntity cloud = ModEntityTypes.BLINDING_MIST_CLOUD.get().create(level);
            if (cloud != null) {
                cloud.setOwner(user);
                cloud.setPos(user.position());
                cloud.setParticle(ParticleTypes.CLOUD);
                cloud.setRadius(3.0f);
                cloud.setDuration(100);
                cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                level.addFreshEntity(cloud);
            }
        }
    }
}
