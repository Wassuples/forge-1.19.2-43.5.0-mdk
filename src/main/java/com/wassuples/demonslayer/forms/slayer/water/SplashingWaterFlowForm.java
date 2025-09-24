package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SplashingWaterFlowForm extends ActiveForm {
    private static final int BREATH_COST_PER_TICK = 1;

    public SplashingWaterFlowForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    public void onTick() {
        if (!user.isSprinting()) {
            ActiveFormManager.stop(user);
            return;
        }

        if (ticksActive > 0 && ticksActive % 20 == 0) {
            user.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                if (breath.getBreath() >= BREATH_COST_PER_TICK) {
                    breath.subBreath(BREATH_COST_PER_TICK);
                } else {
                    ActiveFormManager.stop(user);
                }
            });
        }

        if (!level.isClientSide && user.isSprinting()) {
            BlockPos userPos = user.blockPosition();
            BlockState iceState = Blocks.FROSTED_ICE.defaultBlockState();
            int radius = 2;

            for (BlockPos blockPos : BlockPos.betweenClosed(userPos.offset(-radius, -1, -radius), userPos.offset(radius, -1, radius))) {
                if (level.getBlockState(blockPos).is(Blocks.WATER) && level.isEmptyBlock(blockPos.above())) {
                    level.setBlockAndUpdate(blockPos, iceState);
                }
            }
        }
    }

    @Override
    public void onStop() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}