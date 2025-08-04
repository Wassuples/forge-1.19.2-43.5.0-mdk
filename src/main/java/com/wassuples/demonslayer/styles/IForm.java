package com.wassuples.demonslayer.styles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface IForm {
    enum InputType { PRESS, HOLD}
    String getDisplayName();
    InputType getInputType();
    int getChargeTicks();
    int getBreathCost();
    boolean IsBDA();

    int getCooldownTicks(LivingEntity user);
    default boolean isBuff() {return false;}
    void performAction(LivingEntity user, ServerLevel level);
}
