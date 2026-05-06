package com.wassuples.demonslayer.reputation.witness;

import com.wassuples.demonslayer.entity.custom.CrowEntity;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class WitnessScanner {
    private static final double DEFAULT_RADIUS = 32.0D;

    private WitnessScanner() {
    }

    public static Set<WitnessType> scan(Level level, BlockPos pos) {
        return scan(level, pos, DEFAULT_RADIUS);
    }

    public static Set<WitnessType> scan(Level level, BlockPos pos, double radius) {
        return scan(level, pos, radius, null);
    }

    public static Set<WitnessType> scan(Level level, BlockPos pos, LivingEntity target) {
        return scan(level, pos, DEFAULT_RADIUS, target);
    }

    public static Set<WitnessType> scan(Level level, BlockPos pos, double radius, LivingEntity target) {
        EnumSet<WitnessType> witnesses = EnumSet.noneOf(WitnessType.class);
        AABB box = new AABB(pos).inflate(radius);
        List<Entity> nearby = level.getEntities((Entity) null, box);

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (isSuppressedByBlindness(living)) {
                continue;
            }

            WitnessType type = null;
            if (entity instanceof CrowEntity) {
                type = WitnessType.CROW;
            } else if (entity instanceof Villager) {
                type = WitnessType.VILLAGER;
            } else if (entity instanceof HashiraEntity) {
                type = WitnessType.DEMON_SLAYER;
            } else if (entity instanceof NemesisDemonEntity || entity instanceof DemonEntity) {
                type = WitnessType.DEMON_ALIVE;
            }

            if (type != null) {
                float detectionChance = WitnessModifierTable.calculateDetectionMultiplier(type, target, level, pos);
                if (level.random.nextFloat() < detectionChance) {
                    witnesses.add(type);
                }
            }
        }

        return witnesses;
    }

    public static boolean isSuppressedByBlindness(LivingEntity entity) {
        return entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS);
    }
}
