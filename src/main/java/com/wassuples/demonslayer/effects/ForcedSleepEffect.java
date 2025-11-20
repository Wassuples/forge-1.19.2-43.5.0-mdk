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

public class ForcedSleepEffect extends MobEffect {
    private static final Map<UUID, UUID> SLEEPING_ENTITIES_TO_CASTERS = Maps.newHashMap();

    public ForcedSleepEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        applyImmobility(target, 5);
        if (!target.hasEffect(MobEffects.BLINDNESS)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, target.getEffect(this).getDuration(), 0, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, target.getEffect(this).getDuration(), 255, false, false, true));
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
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