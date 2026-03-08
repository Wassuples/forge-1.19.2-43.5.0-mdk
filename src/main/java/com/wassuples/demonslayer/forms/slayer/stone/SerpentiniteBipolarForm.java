package com.wassuples.demonslayer.forms.slayer.stone;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

/**
 * Stone Breathing form: Serpentinite Bipolar.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SerpentiniteBipolarForm extends TimedForm {
    private static final double MAX_RANGE = 12.0;
    private static final double TRAVEL_SPEED = 1.0;
    private static final float ROTATION_SPEED = 40.0f; // degrees per tick
    private static final double DRILL_RADIUS = 1.5; // The distance of the axe/flail from the center
    private static final float DAMAGE_PER_TICK = 6.0f;

    private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);
    private final Vec3 direction;
    private Vec3 drillCenterPos;
    private float rotationAngle = 0;

    public SerpentiniteBipolarForm(LivingEntity user, IForm form) {
        super(user, form, (int) (MAX_RANGE / TRAVEL_SPEED));
        this.direction = user.getLookAngle();
        this.drillCenterPos = user.getEyePosition();
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.GRINDSTONE_USE, 1.0f, 0.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();
        this.drillCenterPos = this.drillCenterPos.add(this.direction.scale(TRAVEL_SPEED));

        AABB hitBox = new AABB(this.drillCenterPos, this.drillCenterPos).inflate(DRILL_RADIUS);
        List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

        for (LivingEntity target : hitTargets) {
            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
            }
            target.setDeltaMovement(this.direction.scale(TRAVEL_SPEED));
        }
        this.rotationAngle += ROTATION_SPEED;
        Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 upDir = sideDir.cross(this.direction).normalize();
        double angleRad = Math.toRadians(this.rotationAngle);

        Vec3 axePos = this.drillCenterPos.add(sideDir.scale(Math.cos(angleRad) * DRILL_RADIUS)).add(upDir.scale(Math.sin(angleRad) * DRILL_RADIUS));
        Vec3 flailPos = this.drillCenterPos.add(sideDir.scale(-Math.cos(angleRad) * DRILL_RADIUS)).add(upDir.scale(-Math.sin(angleRad) * DRILL_RADIUS));

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), axePos.x, axePos.y, axePos.z, 2, 0.1, 0.1, 0.1, 0.1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 2, 0.1, 0.1, 0.1, 0.1);
        spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 4);
    }
}
