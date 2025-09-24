package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AirTypeForm extends TimedForm {
    // --- Constants ---
    private static final double MAX_RANGE = 25.0;
    private static final double TRAVEL_SPEED = 5.0;       // Blocks per tick
    private static final double HITBOX_SIZE = 2.0;
    private static final float DAMAGE = 8.0f;
    private static final float KNOCKBACK_STRENGTH = 1.8f;

    private final Vec3 direction;
    private Vec3 currentPos;
    private final List<UUID> hitEntities = new ArrayList<>();

    public AirTypeForm(LivingEntity user, IForm form) {
        // The duration is calculated based on how long it takes to travel the max range
        super(user, form, (int) (MAX_RANGE / TRAVEL_SPEED));
        this.direction = user.getLookAngle();
        this.currentPos = user.getEyePosition(); // Start from the player's eyes
    }

    @Override
    public void onStart() {
        // Play a sharp, punchy sound effect
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
        this.currentPos = this.currentPos.add(this.direction.scale(TRAVEL_SPEED));

        level.sendParticles(ParticleTypes.SONIC_BOOM, this.currentPos.x, this.currentPos.y, this.currentPos.z, 1, 0, 0, 0, 0);

        AABB hitBox = new AABB(this.currentPos, this.currentPos).inflate(HITBOX_SIZE);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

        if (!potentialTargets.isEmpty()) {
            for (LivingEntity target : potentialTargets) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }
                target.knockback(KNOCKBACK_STRENGTH, -direction.x, -direction.z);
                this.hitEntities.add(target.getUUID());
            }

            level.playSound(null, this.currentPos.x, this.currentPos.y, this.currentPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.2f);
            ActiveFormManager.stop(user);
            return;
        }

        if (!level.getBlockState(new BlockPos(this.currentPos)).isAir()) {
            ActiveFormManager.stop(user);
        }
    }
}
