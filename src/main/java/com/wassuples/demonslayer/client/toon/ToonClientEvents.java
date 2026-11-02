package com.wassuples.demonslayer.client.toon;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Client-side Toon Force glue: airwalk, knock-down camera, shadow/tunnel registries. */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ToonClientEvents {
    private ToonClientEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
            ToonAirwalkClient.tick(player);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ToonShadowClient.clientTick();
        if (mc.level == null || mc.player == null) {
            ToonTunnelClient.clear();
            ToonCameraAnimator.reset();
            ToonAirwalkClient.reset();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ToonTunnelClient.clear();
            ToonAirwalkClient.reset();
        }
    }

    /** Runs after the gravity camera handler so the knock-down tilt stacks on top of it. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (event.getCamera().isDetached()) return;
        float[] offsets = ToonCameraAnimator.angleOffsets((float) event.getPartialTick());
        if (offsets == null) return;
        event.setPitch(Mth.clamp(event.getPitch() + offsets[0], -90.0f, 90.0f));
        event.setRoll(event.getRoll() + offsets[1]);
    }

    /** No floating first-person arm while lying on your back. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ToonCameraAnimator.flatness(event.getPartialTick()) > 0.25f) {
            event.setCanceled(true);
        }
    }
}
