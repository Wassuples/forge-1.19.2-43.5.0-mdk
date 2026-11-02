package com.wassuples.demonslayer.effects;

import com.google.common.collect.Maps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

/**
 * Custom mob effect: Forced Sleep (Enmu's Blood Demon Art).
 *
 * <p>Immobilizes the target and triggers a client-side Sleep QTE. If the target
 * fails the QTE (or is an NPC), they take damage. Tracks the caster UUID so
 * damage is attributed correctly.
 *
 * @see ModEffects
 * @see com.wassuples.demonslayer.client.gui.SleepQTEOverlay
 */
public class ForcedSleepEffect extends MobEffect {
    private static final Map<UUID, UUID> SLEEPING_ENTITIES_TO_CASTERS = Maps.newHashMap();

    public ForcedSleepEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    private static final String DREAM_AURA_TAG = "ds_dream_aura_id";

    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        ensureDreamVisuals(target);
        applyImmobility(target, 5);
        if (!target.hasEffect(MobEffects.BLINDNESS)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, target.getEffect(this).getDuration(), 0, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, target.getEffect(this).getDuration(), 255, false, false, true));
        }
    }

    /**
     * Visual layer only: makes sure every sleeper has a {@link com.wassuples.demonslayer.entity.custom.DreamAuraEntity}
     * (Z z z, dream halo; visible to everyone) and plays the slumped-over animation on players. Checked every
     * 10 ticks so any source of Forced Sleep gets it, without touching the effect's gameplay.
     */
    private static void ensureDreamVisuals(LivingEntity target) {
        if (!(target.level instanceof net.minecraft.server.level.ServerLevel level) || target.tickCount % 10 != 0
                && target.getPersistentData().contains(DREAM_AURA_TAG)) {
            return;
        }
        int auraId = target.getPersistentData().getInt(DREAM_AURA_TAG);
        boolean hasAura = target.getPersistentData().contains(DREAM_AURA_TAG)
                && level.getEntity(auraId) instanceof com.wassuples.demonslayer.entity.custom.DreamAuraEntity aura && aura.isAlive();
        if (!hasAura) {
            com.wassuples.demonslayer.entity.custom.DreamAuraEntity aura = new com.wassuples.demonslayer.entity.custom.DreamAuraEntity(level, target);
            level.addFreshEntity(aura);
            target.getPersistentData().putInt(DREAM_AURA_TAG, aura.getId());
            com.wassuples.demonslayer.util.FormAnimations.play(target, com.wassuples.demonslayer.util.FormAnimations.ENMU_SLUMBER);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        if (!pLivingEntity.level.isClientSide && pLivingEntity.getPersistentData().contains(DREAM_AURA_TAG)) {
            pLivingEntity.getPersistentData().remove(DREAM_AURA_TAG);
            com.wassuples.demonslayer.util.FormAnimations.play(pLivingEntity, com.wassuples.demonslayer.util.FormAnimations.ENMU_WAKE);
        }
        removeImmobility(pLivingEntity);
        pLivingEntity.removeEffect(MobEffects.BLINDNESS);
        pLivingEntity.removeEffect(MobEffects.WEAKNESS);

        SLEEPING_ENTITIES_TO_CASTERS.remove(pLivingEntity.getUUID());
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }


    public static void setCaster(LivingEntity target, @Nullable LivingEntity caster) {
        if (caster != null) {
            SLEEPING_ENTITIES_TO_CASTERS.put(target.getUUID(), caster.getUUID());
        }
    }

    @Nullable
    public static UUID getCasterUUID(LivingEntity target) {
        return SLEEPING_ENTITIES_TO_CASTERS.get(target.getUUID());
    }
}
