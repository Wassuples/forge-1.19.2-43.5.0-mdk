package com.wassuples.demonslayer.entity.custom.story;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Story slayers' breathing forms sweep everything in front of them: their hits never land on players, other story
 * NPCs, or anyone's pets (a player's Kasugai crow).
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StoryNpcEvents {
    private StoryNpcEvents() {
    }

    /** True when {@code user} is a story NPC and {@code victim} is someone its forms must spare. */
    public static boolean spares(Entity user, LivingEntity victim) {
        return user instanceof StoryNpcEntity && (victim instanceof Player || victim instanceof StoryNpcEntity
                || (victim instanceof OwnableEntity owned && owned.getOwnerUUID() != null));
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().isBypassInvul()) return;
        if (spares(event.getSource().getEntity(), event.getEntity())) event.setCanceled(true);
    }
}
