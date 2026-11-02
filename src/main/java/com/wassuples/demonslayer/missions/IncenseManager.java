package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Burning wisteria incense: a 6-block cloud for 30 seconds. Lesser demons inside (captive and Nemesis demons below
 * the Kizuki) are seared and driven out. Clouds are short-lived and not saved.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncenseManager {
    private static final double RADIUS = 6.0D;
    private static final int DURATION = 600;
    private static final List<Cloud> CLOUDS = new ArrayList<>();

    private IncenseManager() {
    }

    private record Cloud(ServerLevel level, Vec3 pos, long until) {
    }

    public static void light(ServerLevel level, Vec3 pos) {
        CLOUDS.add(new Cloud(level, pos, level.getGameTime() + DURATION));
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.DIALOGUE_OPEN.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    /** Whether {@code pos} lies inside a burning cloud (spawners avoid them too). */
    public static boolean inCloud(ServerLevel level, Vec3 pos) {
        for (Cloud c : CLOUDS) if (c.level == level && c.pos.distanceToSqr(pos) < RADIUS * RADIUS) return true;
        return false;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CLOUDS.isEmpty()) return;
        for (Iterator<Cloud> it = CLOUDS.iterator(); it.hasNext(); ) {
            Cloud c = it.next();
            long now = c.level.getGameTime();
            if (now > c.until || now < c.until - DURATION - 20) {
                it.remove();
                continue;
            }
            if (now % 4 == 0) {
                c.level.sendParticles(ModParticles.INCENSE_SMOKE.get(), c.pos.x, c.pos.y + 0.6, c.pos.z, 6, RADIUS * 0.45, 0.6, RADIUS * 0.45, 0.01);
                c.level.sendParticles(ModParticles.WISTERIA_PETAL.get(), c.pos.x, c.pos.y + 2.5, c.pos.z, 2, RADIUS * 0.5, 0.5, RADIUS * 0.5, 0.0);
            }
            if (now % 10 != 0) continue;
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, new AABB(c.pos, c.pos).inflate(RADIUS), IncenseManager::lesserDemon)) {
                if (e.position().distanceToSqr(c.pos) > RADIUS * RADIUS) continue;
                e.hurt(DamageSource.MAGIC, 3.0F);
                Vec3 out = e.position().subtract(c.pos).multiply(1, 0, 1).normalize().scale(0.7);
                e.setDeltaMovement(out.x, 0.2, out.z);
                if (e instanceof Mob mob) {
                    mob.setTarget(null);
                    Vec3 flee = c.pos.add(out.scale(RADIUS * 2.5 / 0.7));
                    mob.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.3D);
                }
            }
        }
    }

    private static boolean lesserDemon(LivingEntity e) {
        return e.isAlive() && (e instanceof CaptiveDemonEntity || e instanceof NemesisDemonEntity);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        CLOUDS.clear();
    }
}
