package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class RimeBodhisattvaForm extends ActiveForm {
    public RimeBodhisattvaForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 1.0f);

        BodhisattvaEntity bodhisattva = ModEntityTypes.BODHISATTVA.get().create(level);
        if (bodhisattva != null) {
            Vec3 spawnPos = user.position().add(user.getLookAngle().scale(8));
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(spawnPos));
            bodhisattva.setPos(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);

            bodhisattva.setPilot(this.user);
            level.addFreshEntity(bodhisattva);

            user.startRiding(bodhisattva, true);
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1205, 5, false, false, true));
        }
    }

    @Override
    public void onStop() {
        if (user.getVehicle() instanceof BodhisattvaEntity bodhisattva) {
            bodhisattva.shatter();
        }

        user.removeEffect(MobEffects.INVISIBILITY);
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    @Override
    public void onTick() {
    }
}
