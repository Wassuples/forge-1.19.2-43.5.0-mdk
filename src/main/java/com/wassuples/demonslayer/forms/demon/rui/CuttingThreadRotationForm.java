package com.wassuples.demonslayer.forms.demon.rui;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.ThreadWheelEntity;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Rui Blood Demon Art form: Cutting Thread Rotation. Launches a rolling wheel of threads along the ground in
 * the direction the user is facing. All behaviour lives in {@link ThreadWheelEntity}.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 */
public class CuttingThreadRotationForm extends TimedForm {
    public CuttingThreadRotationForm(LivingEntity user, IForm form) {
        super(user, form, 5);
    }

    @Override
    public void onStart() {
        ThreadWheelEntity wheel = ModEntityTypes.THREAD_WHEEL.get().create(level);
        if (wheel == null) return;

        Vec3 look = user.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, user.getYRot()) : flat.normalize();
        Vec3 spawn = user.position().add(flat.scale(1.6));

        wheel.setPos(spawn.x, user.getY() + 0.5, spawn.z);
        wheel.setOwner(user);
        wheel.setDirection(flat, 0);
        wheel.snapToGround();
        level.addFreshEntity(wheel);

        level.playSound(null, user.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0f, 1.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}
