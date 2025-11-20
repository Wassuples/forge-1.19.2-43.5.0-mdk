package com.wassuples.demonslayer.styles;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface IForm {
    ResourceLocation DEFAULT_ICON = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/icons/default_icon.png");

    enum InputType { PRESS, HOLD}
    record CombatContext(LivingEntity user,
                         LivingEntity target,
                         double distanceSqToTarget,
                         float userHealthPercent,
                         float targetHealthPercent,
                         Behavior behavior
                         ) {}

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
    default int getUsagePriority(CombatContext context) {
        // Default to a low priority for any form that doesn't override this.
        // This ensures basic melee attacks are still used.
        return 1;
    }
    void performAction(LivingEntity user, ServerLevel level);
}
