package com.wassuples.demonslayer.forms.slayer.stone;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class UpperSmashForm extends ActiveForm {
    private enum Phase {LAUNCHING, WAITING, SMASHING}

    private static final int LAUNCH_DURATION = 10; // 0.5s for the pincer to fly out
    private static final int WAIT_DURATION = 8;    // ~0.4s pause
    private static final int SMASH_DURATION = 4;   // ~0.2s for the final smash
    private static final double MAX_RANGE = 10.0;
    private static final double PINCER_ANGLE = 25.0; // How wide the V is
    private static final float SMASH_DAMAGE = 20.0f;
    private static final double SMASH_RADIUS = 3.0;

    private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

    private Phase currentPhase = Phase.LAUNCHING;
    private final Vec3 direction;
    private Vec3 axePos;
    private Vec3 flailPos;
    private int phaseTicks = 0;

    public UpperSmashForm(LivingEntity user, IForm form) {
        super(user, form);
        this.direction = user.getLookAngle();
        this.axePos = user.getEyePosition();
        this.flailPos = user.getEyePosition();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.ANVIL_USE, 1.0f, 0.9f);
    }

    @Override
    public void onTick() {
        this.phaseTicks++;

        switch (this.currentPhase) {
            case LAUNCHING:
                double speed = MAX_RANGE / LAUNCH_DURATION;
                Vec3 forwardVec = this.direction.scale(speed);
                Vec3 sideVec = this.direction.cross(new Vec3(0, 1, 0)).normalize().scale(Math.tan(Math.toRadians(PINCER_ANGLE)) * speed);

                this.axePos = this.axePos.add(forwardVec).add(sideVec);
                this.flailPos = this.flailPos.add(forwardVec).subtract(sideVec);

                AABB pincerBox = new AABB(this.axePos, this.flailPos).inflate(1.5);
                List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, pincerBox, e -> e != user && e.isAlive());

                if (!hitTargets.isEmpty() || this.phaseTicks >= LAUNCH_DURATION) {
                    this.currentPhase = Phase.WAITING;
                    this.phaseTicks = 0;
                }
                break;

            case WAITING:
                // HE'S JUST STANDING THERE, MENACINGLY!!
                level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.5f, 1.2f);
                if (this.phaseTicks >= WAIT_DURATION) {
                    this.currentPhase = Phase.SMASHING;
                    this.phaseTicks = 0;
                }
                break;

            case SMASHING:
                Vec3 smashCenter = this.axePos.add(this.flailPos).scale(0.5);
                double smashSpeed = this.flailPos.distanceTo(smashCenter) / SMASH_DURATION;
                Vec3 smashDirection = smashCenter.subtract(this.flailPos).normalize();

                this.flailPos = this.flailPos.add(smashDirection.scale(smashSpeed));

                if (this.phaseTicks >= SMASH_DURATION) {
                    dealAOEDamage(user, smashCenter, SMASH_RADIUS, SMASH_DAMAGE, 1.5f, false, true);
                    level.playSound(null, smashCenter.x, smashCenter.y, smashCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5f, 1.0f);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), smashCenter.x, smashCenter.y, smashCenter.z, 100, 1.5, 1.5, 1.5, 0.5);
                    ActiveFormManager.stop(user);
                }
                break;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), axePos.x, axePos.y, axePos.z, 1, 0, 0, 0, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 1, 0, 0, 0, 0);
        spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 5);
    }
}
