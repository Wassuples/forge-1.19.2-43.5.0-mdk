package com.wassuples.demonslayer.styles;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface IForm {
    ResourceLocation DEFAULT_ICON = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/icons/default_icon.png");

    enum InputType { PRESS, HOLD}

    String getDisplayName();
    String getDescription();
    InputType getInputType();
    int getChargeTicks();
    int getBreathCost();
    int getStyleMasteryToUnlock();
    int getCooldownTicks(LivingEntity user);
    int getMasteryGoal();

    Object getStyle();
    default ResourceLocation getIcon() {
        return DEFAULT_ICON;
    }
    default boolean isBuff() {
        return false;
    }
    void performAction(LivingEntity user, ServerLevel level);
}
