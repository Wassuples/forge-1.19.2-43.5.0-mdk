package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Static movement utility methods for form-related player/entity movement.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #pushPlayer} — launches an entity along their look vector</li>
 *   <li>{@link #applyImmobility} — roots the entity in place with knockback immunity</li>
 *   <li>Other movement helpers for dashes, teleports, and gravitational effects</li>
 * </ul>
 *
 * @see CombatHelper
 * @see VFXHelper
 */
public class MovementHelper {
    /**
     * Pushes the player forward based on their look vector.
     */
    public static void pushPlayer(LivingEntity user, double speed, boolean maintainVertical) {
        Vec3 lookVec = user.getLookAngle();
        double yVel = maintainVertical ? user.getDeltaMovement().y : lookVec.y * speed;
        Vec3 velocity = new Vec3(lookVec.x * speed, yVel, lookVec.z * speed);
        user.setDeltaMovement(velocity);
        user.hurtMarked = true;
    }

    /**
     * Temporarily makes the player immobile and immune to knockback.
     */
    public static void applyImmobility(LivingEntity user, int durationTicks) {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            if (attribute.getModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID) == null) {
                AttributeModifier modifier = new AttributeModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID, "Form knockback immunity", 1.0D, AttributeModifier.Operation.ADDITION);
                attribute.addTransientModifier(modifier);
            }
        }
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 255, false, false, false));
        user.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, -128, false, false, false));
    }

    /**
     * Removes the immobility effect.
     */
    public static void removeImmobility(LivingEntity user) {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            AttributeModifier modifier = new AttributeModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID, "Form knockback immunity", 1.0D, AttributeModifier.Operation.ADDITION);
            if (!attribute.hasModifier(modifier)) {
                attribute.addTransientModifier(modifier);
            }
        }
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        user.removeEffect(MobEffects.JUMP);
    }
}
