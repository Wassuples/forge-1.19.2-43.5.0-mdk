package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Passives granted by story missions.
 *
 * <ul>
 *   <li><b>Unyielding</b> (Final Selection, Hand Demon slain): the first lethal hit each in-game day is survived at
 *       1 HP.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MissionPerks {
    public static final String UNYIELDING = "unyielding";

    private MissionPerks() {
    }

    /** HIGHEST so a saved player never reaches the mission-failure or death-persistence handlers. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLethal(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level.isClientSide) return;
        if (event.getSource().isBypassInvul()) return;   // /kill and the void are final
        MissionManager.data(player).ifPresent(data -> {
            if (!data.hasPerk(UNYIELDING)) return;
            long day = player.getLevel().getDayTime() / 24000L;
            if (data.getUnyieldingDay() == day) return;
            data.setUnyieldingDay(day);
            event.setCanceled(true);
            player.setHealth(1.0F);
            player.invulnerableTime = 20;
            player.getLevel().sendParticles(ModParticles.RESOLVE_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(), 36, 0.35, 0.6, 0.35, 0.25);
            player.getLevel().playSound(null, player.blockPosition(), ModSounds.RESOLVE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            MissionManager.notify(player, Component.translatable("perk.demonslayer.unyielding.triggered"), false);
        });
    }
}
