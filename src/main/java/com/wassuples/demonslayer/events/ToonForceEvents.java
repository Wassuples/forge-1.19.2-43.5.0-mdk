package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import com.wassuples.demonslayer.util.ToonSfx;
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

    /** Pocket's Spring Shoes: the landing boings instead of hurting. */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().level.isClientSide || !ToonForceHelper.consumeSafeLanding(event.getEntity())) return;
        event.setDamageMultiplier(0.0f);
        if (event.getEntity().level instanceof ServerLevel level && event.getDistance() > 1.5f) {
            ToonSfx.spawn(level, event.getEntity().position().add(0, 0.4, 0), ToonSfx.Kind.BOING);
            level.playSound(null, event.getEntity().blockPosition(), com.wassuples.demonslayer.sound.ModSounds.TOON_BOING.get(),
                    SoundSource.PLAYERS, 1.0f, 0.8f);
            level.sendParticles(ParticleTypes.ITEM_SLIME, event.getEntity().getX(), event.getEntity().getY() + 0.1,
                    event.getEntity().getZ(), 16, 0.4, 0.05, 0.4, 0.1);
        }
    }

    /** Fake Tunnel: the caster walks through blocks that are still solid on the server, so never suffocate there. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource() == DamageSource.IN_WALL && ToonFakeTunnelEntity.isInsideOwnTunnel(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
