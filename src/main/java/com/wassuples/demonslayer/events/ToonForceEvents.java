package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@Mod.EventBusSubscriber(modid = "demonslayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToonForceEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.getEquippedBda() == BloodDemonArts.TOON_FORCE) {
                    if (player.getRandom().nextFloat() < 0.25f) {
                        event.getEntity().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!player.level.isClientSide()) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.getEquippedBda() == BloodDemonArts.TOON_FORCE) {
                    if (player.fallDistance > 0 && player.getXRot() < 30) {
                        player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
                        player.fallDistance = 0;
                    }
                }
            });
        }
    }
}

