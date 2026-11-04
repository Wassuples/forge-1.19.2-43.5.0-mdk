package com.wassuples.demonslayer.client.mission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws players resting on a bedroll lying on their backs along the mat, for everyone who can see them (the server
 * sends {@link com.wassuples.demonslayer.networking.packet.RestStateS2CPacket} to all trackers).
 *
 * <p>The player's own render is cancelled and drawn again inside a transform that lays the standing figure down:
 * turned to face away from the pillow, tipped backwards 90 degrees about the feet, then slid onto the middle of the
 * mat. Only render-time copies of the rotations are touched (and put back right after), never the entity's pose, so
 * the hitbox, eye height and movement are unaffected.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RestPoseHandler {
    /** Resting players by entity id, with the direction their head points (2D value; -1: lie along their facing). */
    private static final Map<Integer, Integer> RESTING = new HashMap<>();
    /** Set while we re-render a resting player, so our own render doesn't come back through here. */
    private static boolean drawing;

    private RestPoseHandler() {
    }

    public static void set(int entityId, boolean resting, int headDir) {
        if (resting) RESTING.put(entityId, headDir);
        else RESTING.remove(entityId);
    }

    public static boolean isResting(int entityId) {
        return RESTING.containsKey(entityId);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderPlayerEvent.Pre event) {
        if (drawing || event.isCanceled()) return;
        Player p = event.getEntity();
        Integer dir = RESTING.get(p.getId());
        if (dir == null || !(p instanceof AbstractClientPlayer player)) return;
        event.setCanceled(true);

        Direction head = dir >= 0 ? Direction.from2DDataValue(dir) : p.getDirection();
        Direction facing = head.getOpposite();   // standing, they face away from the pillow, then lie back
        float yaw = facing.toYRot();
        float bodyO = p.yBodyRotO, body = p.yBodyRot, headO = p.yHeadRotO, headR = p.yHeadRot, xO = p.xRotO, x = p.getXRot();
        float swingO = p.animationSpeedOld, swing = p.animationSpeed, attack = p.attackAnim, attackO = p.oAttackAnim;
        p.yBodyRotO = p.yBodyRot = yaw;
        p.yHeadRotO = p.yHeadRot = yaw;
        p.xRotO = 0;
        p.setXRot(0);
        p.animationSpeedOld = p.animationSpeed = 0;
        p.attackAnim = p.oAttackAnim = 0;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // the body is 1.8 long from the feet: slide it so its middle lies on the middle of the mat, a little raised
        pose.translate(facing.getStepX() * 0.9D, 0.27D, facing.getStepZ() * 0.9D);
        Vector3f axis = new Vector3f(-facing.getStepZ(), 0, facing.getStepX());
        pose.mulPose(new Quaternion(axis, 90.0F, true));
        drawing = true;
        try {
            event.getRenderer().render(player, yaw, event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            drawing = false;
            pose.popPose();
            p.yBodyRotO = bodyO;
            p.yBodyRot = body;
            p.yHeadRotO = headO;
            p.yHeadRot = headR;
            p.xRotO = xO;
            p.setXRot(x);
            p.animationSpeedOld = swingO;
            p.animationSpeed = swing;
            p.attackAnim = attack;
            p.oAttackAnim = attackO;
        }
    }

    /** No name plate floating sideways over someone lying down. */
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (drawing && event.getEntity() instanceof Player) event.setResult(Event.Result.DENY);
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
    }
}
