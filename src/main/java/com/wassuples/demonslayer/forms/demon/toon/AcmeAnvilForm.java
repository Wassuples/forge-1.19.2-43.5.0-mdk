package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.CombatHelper;
import com.wassuples.demonslayer.util.FormAnimations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AcmeAnvilForm extends TimedForm {
    private static final int WHISTLE_TICK = 10;
    private static final int IMPACT_TICK = 28;
    private static final int TOTAL_TICKS = 30;
    private LivingEntity target;
    private BlockPos targetPos;

    public AcmeAnvilForm(LivingEntity user, IForm formType) {
        super(user, formType, TOTAL_TICKS);
    }

    @Override
    public void onStart() {
        super.onStart();
        FormAnimations.play(user, FormAnimations.TOON_ANVIL);
        this.target = CombatHelper.getLookedAtEntity(user, 12.0);
        if (this.target == null) {
            HitResult ray = user.pick(12.0, 0.0f, false);
            if (ray.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) ray;
                this.targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            } else {
                Vec3 point = user.getEyePosition().add(user.getLookAngle().normalize().scale(8.0));
                BlockPos rough = new BlockPos(point.x, user.getY(), point.z);
                this.targetPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rough);
            }
        } else {
            this.targetPos = target.blockPosition();
        }

    }

    @Override
    public void onTick() {
        super.onTick();

        if (targetPos == null) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive >= WHISTLE_TICK && ticksActive < IMPACT_TICK) {
            // Silhouette logic
            double x = target != null ? target.getX() : targetPos.getX() + 0.5;
            double z = target != null ? target.getZ() : targetPos.getZ() + 0.5;
            double y = target != null ? target.getY() + 4.0 : targetPos.getY() + 5.0;
            level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 8, 0.65, 0.08, 0.65, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, x, y - 0.55, z, 4, 0.35, 0.04, 0.35, 0.0);
        }

        if (ticksActive == WHISTLE_TICK) {
            level.playSound(null, targetPos, ModSounds.TOON_WHISTLE_FALL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if (ticksActive == IMPACT_TICK) {
            if (!level.isClientSide) {
                double x = target != null ? target.getX() : targetPos.getX() + 0.5;
                double z = target != null ? target.getZ() : targetPos.getZ() + 0.5;
                double y = target != null ? target.getY() + 5.0 : targetPos.getY() + 6.0;

                ToonAnvilEntity anvil = new ToonAnvilEntity(level, user);
                anvil.setPos(x, y, z);
                level.addFreshEntity(anvil);
                level.sendParticles(ParticleTypes.FLASH, x, y - 4.5, z, 1, 0, 0, 0, 0.0);
            }
        }
    }
}
