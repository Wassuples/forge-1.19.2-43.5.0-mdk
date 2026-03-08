package com.wassuples.demonslayer.client;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side component: Lesser Eye Renderer Manager.
 */
public class LesserEyeRendererManager {

    private static List<BlockPos> activeEyePositions = new ArrayList<>();
    private static boolean needsSound = false;

    public static void updateEyePositions(List<BlockPos> positions) {
        activeEyePositions = positions;
        if (!positions.isEmpty()) {
            needsSound = true;
        }
    }

    public static void clearEyePositions() {
        activeEyePositions.clear();
    }

    public static void tick() {
        Level level = Minecraft.getInstance().level;
        if (level == null || activeEyePositions.isEmpty()) {
            return;
        }

        if (needsSound) {
            level.playSound(Minecraft.getInstance().player, activeEyePositions.get(0),
                    SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.7f, 1.5f);
            needsSound = false;
        }

        final DustParticleOptions fleshParticle = new DustParticleOptions(new Vector3f(0.8f, 0.2f, 0.2f), 1.5f); // Larger red dust

        for (BlockPos pos : activeEyePositions) {
            if (level.random.nextFloat() < 0.8f) {
                level.addParticle(fleshParticle,
                        pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        0, 0.01, 0);
                level.addParticle(ParticleTypes.ASH,
                        pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5,
                        pos.getY() + 0.3,
                        pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.01, 0);
            }
        }
    }
}