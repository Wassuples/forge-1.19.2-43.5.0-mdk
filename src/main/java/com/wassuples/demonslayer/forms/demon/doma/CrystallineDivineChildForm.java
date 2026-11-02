package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.custom.DomaArtEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Doma Blood Demon Art form: Crystalline Divine Child.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CrystallineDivineChildForm extends ChargeableForm {
    private static final int CLONE_CHARGE_TIME = 20; // 1s/clone
    private static final int MAX_CLONES = 6;

    private DomaArtEntity art;

    public CrystallineDivineChildForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    protected int getReleaseDuration() {
        return 0;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        this.art = new DomaArtEntity(level, user, DomaArtEntity.Mode.CRYSTAL_CHARGE, getMaxChargeTicks() + 40);
        level.addFreshEntity(this.art);
        FormAnimations.play(user, FormAnimations.DOMA_CHARGE);
    }

    @Override
    protected void onChargingTick() {
        // The orbiting crystal lotuses (one per clone) are drawn by DomaArtEntity.
    }

    @Override
    protected void onChargeRelease(float chargeRatio) {
        if (art != null) {
            art.discard();
            art = null;
        }
        FormAnimations.play(user, FormAnimations.DOMA_SUMMON);
        int clonesToSpawn = Math.min(chargeTicks / CLONE_CHARGE_TIME, MAX_CLONES);
        if (clonesToSpawn > 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.5f, 1.5f);

            for (int i = 0; i < clonesToSpawn; i++) {
                double angle = (level.random.nextDouble() * 2 * Math.PI);
                double dist = 2.0 + level.random.nextDouble() * 3.0;
                Vec3 spawnPos = user.position().add(new Vec3(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
                spawnCloneAt(spawnPos);
            }
        }
    }

    private void spawnCloneAt(Vec3 position) {
        CrystallineDivineChildEntity clone = ModEntityTypes.CRYSTALLINE_DIVINE_CHILD.get().create(level);
        if (clone != null) {
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(position));
            clone.setPos(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            clone.setOwner(this.user);

            if (this.user instanceof Player playerOwner) {
                clone.setOwnerProfile(playerOwner.getGameProfile());
            } else {
                clone.setIsNpcClone(true);
            }

            level.addFreshEntity(clone);
        }
    }

    private void spawnIceLotusVFX(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 3; i++) {
            double angle = (2 * Math.PI / 3) * i + (user.tickCount * 0.1);
            Vec3 petalPos = center.add(new Vec3(Math.cos(angle) * 0.5, 0.2, Math.sin(angle) * 0.5));
            level.sendParticles(ParticleTypes.END_ROD, petalPos.x, petalPos.y, petalPos.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onReleaseTick() {
    }

    @Override
    protected void onChargeEnd() {
        if (art != null) {
            art.discard();
            art = null;
            FormAnimations.play(user, FormAnimations.DOMA_END);
        }
    }
}
