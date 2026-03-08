package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.tracking.CrystallineDivineChildTracker;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.forms.demon.doma.CrystallineDivineChildForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cancels Crystalline Divine Child (CRYO_8) and despawns its summons if the caster is poisoned.
 */
@Mod.EventBusSubscriber(modid = "demonslayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrystallineDivineChildCancelHandler {
    private CrystallineDivineChildCancelHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity caster = event.getEntity();
        if (caster.level.isClientSide) return;

        // Keep it cheap: run twice per second.
        if (caster.tickCount % 10 != 0) return;

        boolean isPoisoned = caster.hasEffect(MobEffects.POISON) || caster.hasEffect(ModEffects.WISTERIA_POISON.get());
        if (!isPoisoned) return;

        // Always remove clones when the owner is poisoned.
        if (caster.level instanceof ServerLevel serverLevel) {
            CrystallineDivineChildTracker.removeAllForOwner(serverLevel, caster.getUUID());
        }

        // If the caster is currently using the CDC form, also stop it.
        ActiveForm form = ActiveFormManager.getActiveForm(caster);
        if (form instanceof CrystallineDivineChildForm) {
            ActiveFormManager.stop(caster);
        }
    }
}
