package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.infinitycastle.InfinityCastleManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "demonslayer")
public class InfinityCastleEvents {

    // Unique dimension key mapped to datapack
    public static final ResourceKey<Level> INFINITY_CASTLE_DIMENSION = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("demonslayer", "infinity_castle"));

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            // Only tick the schedule manager in the Infinity Castle dimension
            if (serverLevel.dimension().equals(INFINITY_CASTLE_DIMENSION)) {
                InfinityCastleManager.get(serverLevel).tick(serverLevel);
            }
        }
    }
}

