package com.wassuples.demonslayer.client.weapon;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Remembers which living entity is being rendered, so an item renderer drawing a held weapon in third person knows
 * whose swing and timing to follow (item renderers aren't told who holds the item).
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponHolderTracker {
    private static LivingEntity current;

    private WeaponHolderTracker() {
    }

    public static LivingEntity current() {
        return current;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        current = event.getEntity();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        current = null;
    }
}
