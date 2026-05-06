package com.wassuples.demonslayer.reputation.witness;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class WitnessModifierTable {

    public static float calculateDetectionMultiplier(WitnessType witnessType, LivingEntity target, Level level, BlockPos pos) {
        float multiplier = 1.0f;

        multiplier *= getBreathingStyleModifier(witnessType, target);
        multiplier *= getTimeOfDayModifier(witnessType, level);
        multiplier *= getMoonPhaseModifier(witnessType, level);
        multiplier *= getLightingModifier(witnessType, level, pos);
        multiplier *= getPlayerStateModifier(witnessType, target);

        return multiplier;
    }

    private static float getBreathingStyleModifier(WitnessType witnessType, LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return 1.0f;
        }

        final float[] mod = {1.0f};

        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            BreathingStyles activeStyle = style.getCurrentStyles();
            if (activeStyle != null && witnessType != WitnessType.CROW) {
                switch (activeStyle) {
                    case MIST:
                        mod[0] *= 0.70f;
                        break;
                    case THUNDER:
                        mod[0] *= 1.25f;
                        break;
                    case INSECT:
                        mod[0] *= 0.80f;
                        break;
                    case SERPENT:
                        if (witnessType == WitnessType.DEMON_SLAYER) {
                            mod[0] *= 0.85f;
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        return mod[0];
    }

    private static float getTimeOfDayModifier(WitnessType witnessType, Level level) {
        long time = level.getDayTime() % 24000;

        boolean isDay = time < 12000;
        boolean isDuskDawn = (time >= 12000 && time < 13000) || (time >= 23000 && time < 24000);
        boolean isDeepNight = time >= 18000 && time < 23000;

        if (isDay) return 1.0f;

        if (isDuskDawn && witnessType == WitnessType.VILLAGER) {
            return 0.85f;
        }

        if (isDeepNight && (witnessType == WitnessType.DEMON_ALIVE || witnessType == WitnessType.DEMON_DYING)) {
            return 1.35f;
        }

        if (witnessType == WitnessType.VILLAGER) {
            return 0.60f; // Standard night for villager
        }

        return 1.0f;
    }

    private static float getMoonPhaseModifier(WitnessType witnessType, Level level) {
        int phase = level.getMoonPhase();

        // 0 = Full Moon, 4 = New Moon
        if (phase == 4) { // New Moon
            if (witnessType == WitnessType.VILLAGER || witnessType == WitnessType.DEMON_SLAYER) {
                return 0.75f;
            } else if (witnessType == WitnessType.DEMON_ALIVE || witnessType == WitnessType.DEMON_DYING) {
                return 0.60f;
            }
        } else if (phase == 0) { // Full Moon
            if (witnessType == WitnessType.VILLAGER || witnessType == WitnessType.DEMON_SLAYER) {
                return 0.80f;
            } else if (witnessType == WitnessType.DEMON_ALIVE || witnessType == WitnessType.DEMON_DYING) {
                return 1.50f;
            }
        }

        return 1.0f;
    }

    private static float getLightingModifier(WitnessType witnessType, Level level, BlockPos pos) {
        if (witnessType != WitnessType.VILLAGER && witnessType != WitnessType.DEMON_SLAYER) {
            return 1.0f;
        }

        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);

        // Approximating night light level taking moon into account.
        // This could be improved depending on the game's actual light scale at the moment, but
        // using Max(blockLight, effectiveSkyLight) is generally accurate.
        int effectiveSkyLight = skyLight - level.getSkyDarken();
        int lightLevel = Math.max(blockLight, effectiveSkyLight);

        if (level.isRaining() && level.isThundering()) {
            return 0.60f; // Snowstorm/blizzard approximation
        }

        if (lightLevel <= 4) {
            return 0.50f;
        } else if (lightLevel <= 8) {
            return 0.75f;
        }

        return 1.0f;
    }

    private static float getPlayerStateModifier(WitnessType witnessType, LivingEntity target) {
        if (witnessType != WitnessType.VILLAGER && witnessType != WitnessType.DEMON_SLAYER) {
            return 1.0f;
        }

        if (target.hasEffect(MobEffects.INVISIBILITY)) {
            return 0.0f;
        }

        if (target.isCrouching()) {
            return 0.70f;
        }

        return 1.0f;
    }
}


