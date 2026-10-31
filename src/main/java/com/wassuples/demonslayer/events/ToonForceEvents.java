package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid = "demonslayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToonForceEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.getEquippedBda() == BloodDemonArts.TOON_FORCE) {
                    if (!event.getEntity().level.isClientSide && event.getAmount() > 0.0f
                            && event.getEntity().level instanceof ServerLevel level) {
                        ToonForceHelper.tryApplyDizzy(event.getEntity(), player, 0.25f);
                        ToonForceHelper.spawnBonkText(level, event.getEntity().position());
                        FormAnimations.play(event.getEntity(), FormAnimations.TOON_BONKED);
                        level.sendParticles(ParticleTypes.CLOUD, event.getEntity().getX(), event.getEntity().getEyeY(),
                                event.getEntity().getZ(), 10, 0.25, 0.18, 0.25, 0.0);
                        level.playSound(null, event.getEntity().blockPosition(), SoundEvents.WOODEN_BUTTON_CLICK_ON,
                                SoundSource.PLAYERS, 0.75f, 1.8f);
                    }
                }
            });
        }
    }

}
