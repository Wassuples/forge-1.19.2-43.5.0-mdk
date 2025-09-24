package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.MistCloneEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class FakeRainbowForm extends TimedForm {
    private static final int DURATION = 10;
    private static final double DASH_SPEED = 2.0;
    private static final int AFTERIMAGE_COUNT = 3;

    public FakeRainbowForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 2.0f);

        AABB searchBox = user.getBoundingBox().inflate(32.0);
        List<Mob> targetingMobs = level.getEntitiesOfClass(Mob.class, searchBox, mob -> mob.getTarget() == user);
        for (Mob mob : targetingMobs) {
            mob.setTarget(null);
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        Vec3 moveDir = new Vec3(user.getDeltaMovement().x, 0, user.getDeltaMovement().z);
        if (moveDir.lengthSqr() < 0.01) { // If standing still, dash backwards
            moveDir = user.getLookAngle().scale(-1);
        }
        Vec3 dashVec = moveDir.normalize().scale(DASH_SPEED);
        user.setDeltaMovement(dashVec.x, 0.2, dashVec.z);
        user.hurtMarked = true;

        if (ticksActive % 2 == 0) {
            // Leave a trail of afterimages
            if (ticksActive < DURATION - 2) {
                spawnAfterimage();
            }

            for (int i = 0; i < 7; i++) {
                float hue = (float)i / 7.0f;
                Color color = Color.getHSBColor(hue, 0.9f, 1.0f);
                DustParticleOptions particle = new DustParticleOptions(new Vector3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f), 1.0f);
                level.sendParticles(particle, user.getRandomX(0.5), user.getRandomY(), user.getRandomZ(0.5), 1, 0,0,0,0);
            }
        }
    }

    private void spawnAfterimage() {
        MistCloneEntity decoy = ModEntityTypes.MIST_CLONE.get().create(level);
        if (decoy != null) {
            if(user instanceof Player player) {
                decoy.setOwnerProfile(player.getGameProfile());
            } else {
                decoy.setIsNpcClone(true);
            }

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                decoy.setItemSlot(slot, user.getItemBySlot(slot).copy());
            }

            decoy.setPos(user.position());
            decoy.setYRot(user.getYRot());
            decoy.setPersistenceRequired();
            level.addFreshEntity(decoy);
        }
    }
}
