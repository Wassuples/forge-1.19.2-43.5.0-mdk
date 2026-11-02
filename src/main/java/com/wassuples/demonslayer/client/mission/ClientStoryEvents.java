package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.dialogue.ClientDialogueHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drops client-side story state (speech bubbles, notices, banner, tracker) when leaving a world. */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientStoryEvents {
    private ClientStoryEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDialogueHandler.clear();
        MissionNoticeOverlay.clear();
        RestFadeOverlay.clear();
        ClientMissionHandler.tracker = null;
    }
}
