package com.wassuples.demonslayer.client.effect;

import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ParryIndicatorHandler {
    private static final Map<IForm, BiFunction<Level, Vec3, Void>> INDICATOR_ACTIONS = new ImmutableMap.Builder<IForm, BiFunction<Level, Vec3, Void>>()
            .put(BreathingForms.WATER_11, ParryIndicatorHandler::showWaterIndicator)
            .put(BreathingForms.FLAME_4, ParryIndicatorHandler::showFlameIndicator)
            .put(BreathingForms.WIND_3, ParryIndicatorHandler::showWindIndicator)
            .put(BreathingForms.MIST_3, ParryIndicatorHandler::showMistIndicator)
            .put(BreathingForms.STONE_3, ParryIndicatorHandler::showStoneIndicator)
            .put(BreathingForms.FLOWER_2, ParryIndicatorHandler::showFlowerIndicator)
            // BDA
            .put(DemonForms.HAEMO_2, ParryIndicatorHandler::showHaemoIndicator)
            .build();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        mc.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            BreathingForms currentForm = style.getCurrentForms();
            if (!INDICATOR_ACTIONS.containsKey(currentForm)) return;

            AABB scanBox = mc.player.getBoundingBox().inflate(10.0);
            List<Entity> nearbyEntities = mc.level.getEntities(mc.player, scanBox,
                    entity -> !entity.isAlliedTo(mc.player) && (entity instanceof Projectile || entity instanceof LivingEntity));

            for (Entity threat : nearbyEntities) {
                checkThreatAndShowIndicator(mc.player, threat, currentForm);
            }
        });
    }

    private static void checkThreatAndShowIndicator(Player player, Entity threat, IForm form) {
        Vec3 indicatorPos = null;

        if (threat instanceof Projectile projectile) {
            Vec3 pPos = projectile.position();
            Vec3 playerPos = player.getEyePosition();

            double distanceToImpact = pPos.distanceTo(playerPos);
            if (distanceToImpact > 1.0 && distanceToImpact < 4.0) {
                indicatorPos = projectile.position();
            }
        } else if (threat instanceof LivingEntity living) {
            boolean isHostile = false;
            if (living instanceof Mob mob) {
                if (mob.getTarget() == player) {
                    isHostile = true;
                }
            } else if (!(living instanceof Animal)) {
                isHostile = true;
            }
            if (isHostile && living.swinging && living.distanceTo(player) < 5.0) {
                indicatorPos = living.getEyePosition().add(living.getLookAngle().scale(1.2));
            }
        }

        if (indicatorPos != null) {
            INDICATOR_ACTIONS.get(form).apply(player.level, indicatorPos);
        }
    }

    private static Void showWaterIndicator(Level level, Vec3 position) {
        level.addParticle(ParticleTypes.END_ROD, position.x, position.y, position.z, 0, 0, 0);
        playSound(level, position, SoundEvents.NOTE_BLOCK_BELL, 1.0f, 2.0f);
        return null;
    }

    private static Void showFlameIndicator(Level level, Vec3 position) {
        level.addParticle(ParticleTypes.FLAME, position.x, position.y, position.z, 0, 0, 0);
        playSound(level, position, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.8f);
        return null;
    }

    private static Void showWindIndicator(Level level, Vec3 position) {
        level.addParticle(ParticleTypes.CLOUD, position.x, position.y, position.z, 0, 0, 0);
        playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 2.0f);
        return null;
    }

    private static Void showMistIndicator(Level level, Vec3 position) {
        level.addParticle(ParticleTypes.CLOUD, position.x, position.y, position.z, 0.1, 0.1, 0.1);
        playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
        return null;
    }

    private static Void showStoneIndicator(Level level, Vec3 position) {
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), position.x, position.y, position.z, 0.1, 0.1, 0.1);
        playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
        return null;
    }

    private static Void showFlowerIndicator(Level level, Vec3 position) {
        level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, position.x, position.y, position.z, 0.1, 0.1, 0.1);
        playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
        return null;
    }

    private static Void showHaemoIndicator(Level level, Vec3 position) {
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), position.x, position.y, position.z, 0.1, 0.1, 0.1);
        playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
        return null;
    }

    private static void playSound(Level level, Vec3 pos, SoundEvent sound, float vol, float pitch) {
        level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, vol, pitch, false);
    }
}
