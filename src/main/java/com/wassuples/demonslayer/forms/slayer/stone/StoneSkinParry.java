package com.wassuples.demonslayer.forms.slayer.stone;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class StoneSkinParry extends ParryForm {
    private static final int PARRY_WINDOW = 25; // 1.25-second parry window
    private static final float COUNTER_DAMAGE = 25.0f;
    private static final double COUNTER_RADIUS = 6.0;
    private static final float COUNTER_KNOCKBACK = 2.5f;
    private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);
    private float rotationAngle = 0;

    public StoneSkinParry(LivingEntity user, IForm form) {
        super(user, form, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    public void onTick() {
        super.onTick();

        this.rotationAngle += 35.0f;
        double angleRad = Math.toRadians(this.rotationAngle);
        double radius = 1.5;

        Vec3 axePos = user.getEyePosition().add(new Vec3(Math.cos(angleRad) * radius, 0, Math.sin(angleRad) * radius));
        Vec3 flailPos = user.getEyePosition().add(new Vec3(Math.cos(angleRad + Math.PI) * radius, 0, Math.sin(angleRad + Math.PI) * radius));

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), axePos.x, axePos.y, axePos.z, 1, 0, 0, 0, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 1, 0, 0, 0, 0);
        spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 3);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 1.0f);
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);

        dealAOEDamage(user, user.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK, true, true);

        BlockState groundState = level.getBlockState(user.blockPosition().below());
        if (!groundState.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), user.getX(), user.getY() + 0.2, user.getZ(), 100, COUNTER_RADIUS * 0.8, 0.2, COUNTER_RADIUS * 0.8, 0.2);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getY() + 0.5, user.getZ(), 1, 0, 0, 0, 0);
    }
}
