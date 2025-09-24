package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
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

public class MoonDragonRingtailForm extends TimedForm {
    private static final int DURATION = 40;
    private static final double MAX_RANGE = 35.0;
    private static final double SLASH_WIDTH = 4.0;
    private static final float DAMAGE = 45.0f;
    private static final float KNOCKBACK = 1.2f;

    private static final double WEAVE_AMPLITUDE = 6.0;
    private static final double WEAVE_FREQUENCY = 1.5;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.5f);

    private final Vec3 startPos;
    private final Vec3 direction;
    private final Vec3 sideVector;
    private final Set<UUID> hitEntities = new HashSet<>();

    public MoonDragonRingtailForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.startPos = user.position();
        this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        this.sideVector = this.direction.cross(new Vec3(0, 1, 0)).normalize();
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_HURT, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) this.ticksActive / DURATION;
        Vec3 forwardPoint = this.startPos.add(this.direction.scale(progress * MAX_RANGE));

        double weaveOffset = Math.sin(progress * Math.PI * WEAVE_FREQUENCY) * WEAVE_AMPLITUDE;
        Vec3 slashCenter = forwardPoint.add(this.sideVector.scale(weaveOffset));

        AABB damageBox = new AABB(slashCenter, slashCenter).inflate(SLASH_WIDTH, SLASH_WIDTH, SLASH_WIDTH);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageBox,
                e -> e != user && e.isAlive() && !this.hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
            }
            target.knockback(KNOCKBACK, -direction.x, -direction.z);
            this.hitEntities.add(target.getUUID());
        }
        spawnParticleArcInWorld(level, slashCenter.add(0, 2.0, 0), 4.0, 180, 40, user.getYRot(), 90, PURPLE_SLASH);
    }
}
