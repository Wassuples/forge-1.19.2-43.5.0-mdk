package com.wassuples.demonslayer.styles;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Common contract for all player "forms" (abilities).
 *
 * <p>A form is something a living entity can execute as an action on the server. In this mod, forms are
 * primarily implemented as enum constants (e.g. {@code BreathingForms}, {@code DemonForms}) and are
 * triggered by keybinds/network packets.
 *
 * <h2>Execution model</h2>
 * <ul>
 *   <li>Forms are executed on the logical server via {@link #performAction(LivingEntity, ServerLevel)}.</li>
 *   <li>Costs (breath/hunger/etc) and cooldowns are enforced by the packet/manager that calls the form, not
 *       by the form itself.</li>
 * </ul>
 *
 * <h2>Costs</h2>
 * {@link #getBreathCost()} is named historically for slayer breathing, but callers may interpret it as a
 * generic resource cost.
 *
 * <ul>
 *   <li>Breathing forms: interpreted as breath cost (custom {@code PlayerBreath}).</li>
 *   <li>Demon/BDA forms: interpreted as hunger cost (vanilla {@code FoodData}).</li>
 * </ul>
 */
public interface IForm {
    /** Fallback icon used if a form does not provide a specific icon. */
    ResourceLocation DEFAULT_ICON =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/icons/default_icon.png");

    /**
     * How the form is triggered by input.
     *
     * <ul>
     *   <li>{@link #PRESS}: triggers once per key press.</li>
     *   <li>{@link #HOLD}: requires charging/holding and is usually managed by an active-form tick system.</li>
     * </ul>
     */
    enum InputType {
        PRESS,
        HOLD
    }

    /**
     * Context passed to AI decision-making to determine which form to use.
     *
     * <p>This is primarily used by nemesis/AI combat routines.
     */
    record CombatContext(
            LivingEntity user,
            LivingEntity target,
            double distanceSqToTarget,
            float userHealthPercent,
            float targetHealthPercent,
            Behavior behavior) {}

    /** @return localized/player-facing display name */
    String getDisplayName();

    /** @return player-facing description for tooltips/menus */
    String getDescription();

    /** @return whether this is a press-to-use or hold-to-charge form */
    InputType getInputType();

    /**
     * @return charge/hold duration in ticks. For {@link InputType#PRESS} forms, this is typically 0.
     */
    int getChargeTicks();

    /**
     * Returns the resource cost for the form.
     *
     * <p>Interpretation depends on the caller (see class JavaDoc). This should be a non-negative value.
     */
    int getBreathCost();

    /** @return minimum style mastery required to unlock this form */
    int getStyleMasteryToUnlock();

    /**
     * Returns the cooldown duration (in ticks) for the given user.
     *
     * <p>Callers may apply additional multipliers (e.g. gamerules, equipment).
     */
    int getCooldownTicks(LivingEntity user);

    /** @return how much mastery is required to "master" this form */
    int getMasteryGoal();

    /**
     * @return the style/category this form belongs to.
     *     <p>Historically this returns different types depending on the form family (breathing vs BDA).
     */
    Object getStyle();

    /** @return icon for UI render */
    default ResourceLocation getIcon() {
        return DEFAULT_ICON;
    }

    /** @return true if the form is primarily a buff/stance rather than an attack */
    default boolean isBuff() {
        return false;
    }

    /**
     * Returns how desirable this form is for AI usage.
     *
     * <p>Higher values mean the AI is more likely to pick this form.
     */
    default int getUsagePriority(CombatContext context) {
        // Default to a low priority for any form that doesn't override this.
        // This ensures basic melee attacks are still used.
        return 1;
    }

    /**
     * Executes the form action.
     *
     * <p>This must be called server-side.
     */
    void performAction(LivingEntity user, ServerLevel level);
}
