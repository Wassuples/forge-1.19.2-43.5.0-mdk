package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws players resting on a bedroll lying down: the player is rendered in the sleeping pose for that frame only (the
 * vanilla renderer lays a sleeping entity flat along its facing), then their real pose is put back.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RestPoseHandler {
    private static final Set<Integer> RESTING = new HashSet<>();
    private static final Map<Integer, Pose> SAVED = new HashMap<>();

    private RestPoseHandler() {
    }

    public static void set(int entityId, boolean resting) {
        if (resting) RESTING.add(entityId);
        else RESTING.remove(entityId);
    }

    /** Runs last, and not at all if another mod cancelled the render (then Post wouldn't put the pose back). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        if (!RESTING.contains(p.getId()) || p.getPose() == Pose.SLEEPING) return;
        SAVED.put(p.getId(), p.getPose());
        p.setPose(Pose.SLEEPING);
    }

    @SubscribeEvent
    public static void onPost(RenderPlayerEvent.Post event) {
        Player p = event.getEntity();
        Pose saved = SAVED.remove(p.getId());
        if (saved != null) p.setPose(saved);
    }

    /** Out of sight, out of mind: the server says again when they come back into view. */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) RESTING.remove(event.getEntity().getId());
    }

    /** A dimension change replaces the client level without removing its entities one by one. */
    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) RESTING.clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RESTING.clear();
        SAVED.clear();
    }
}
