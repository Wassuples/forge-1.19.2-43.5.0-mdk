package com.wassuples.demonslayer.forms.slayer.stone;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class VolcanicRockForm extends TimedForm {
    private static final int SWING_DURATION = 10; // The swing takes half a second to complete
    private static final double ATTACK_REACH = 6.0;
    private static final double IMPACT_RADIUS = 2.0; // The hitbox of each weapon head
    private static final float DAMAGE = 18.0f;
    private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

    private final List<UUID> hitEntities = new ArrayList<>();

    public VolcanicRockForm(LivingEntity user, IForm form) {
        super(user, form, SWING_DURATION);
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.ANVIL_USE, 1.0f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) ticksActive / SWING_DURATION;
        double currentReach = ATTACK_REACH * progress;

        Vec3 playerPos = user.getEyePosition();
        Vec3 lookDir = user.getLookAngle();
        Vec3 sideDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        Vec3 rightAxePos = playerPos.add(sideDir.scale(currentReach));
        Vec3 leftFlailPos = playerPos.add(sideDir.scale(-currentReach));

        AABB damageZone = new AABB(rightAxePos, leftFlailPos).inflate(IMPACT_RADIUS);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof ServerPlayer player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
            }
            hitEntities.add(target.getUUID());
        }

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), rightAxePos.x, rightAxePos.y, rightAxePos.z, 5, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), leftFlailPos.x, leftFlailPos.y, leftFlailPos.z, 5, 0.2, 0.2, 0.2, 0.1);
        spawnLineParticles(level, CHAIN_PARTICLE, playerPos, rightAxePos, 5);
        spawnLineParticles(level, CHAIN_PARTICLE, playerPos, leftFlailPos, 5);
    }
}
