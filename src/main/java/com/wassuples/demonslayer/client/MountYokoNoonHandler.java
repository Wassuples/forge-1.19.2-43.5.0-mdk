package com.wassuples.demonslayer.client;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only, visual time override to make Mount Yoko look like perpetual noon.
 *
 * Important:
 * - Does NOT change server time.
 * - Only affects the client's rendering while the player is inside the biome.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT)
public final class MountYokoNoonHandler {
    /**
     * Minecraft uses dayTime modulo 24000.
     * 6000 is noon.
     */
    private static final long NOON_TIME = 6000L;

    private MountYokoNoonHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientLevel level = mc.level;

        ResourceKey<Biome> biomeKey = level.getBiome(mc.player.blockPosition()).unwrapKey().orElse(null);
        if (biomeKey == null || biomeKey != ModBiomes.MOUNT_YOKO) return;

        // Visual-only override: set the client world's day time each tick.
        // This does not get sent to the server.
        level.setDayTime(NOON_TIME);
    }
}

