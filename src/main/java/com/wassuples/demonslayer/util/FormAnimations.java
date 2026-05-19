package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayPlayerAnimationS2CPacket;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-facing helper for form animation playback.
 *
 * <p>PlayerAnimator runs on clients, so forms call this class on the logical server and the packet
 * asks each client to play the named animation for the synced entity id.
 */
public final class FormAnimations {
    public static final String TOON_EYE_POP = "toon_eye_pop";
    public static final String TOON_ANVIL = "toon_anvil";
    public static final String TOON_BANANA = "toon_banana";
    public static final String TOON_SHADOW = "toon_shadow";
    public static final String TOON_TUNNEL = "toon_tunnel";
    public static final String TOON_POCKET = "toon_pocket";
    public static final String TOON_BONKED = "toon_bonked";
    public static final String TOON_SLIP = "toon_slip";
    public static final String TOON_PIE_SPLAT = "toon_pie_splat";

    private FormAnimations() {
    }

    public static void play(LivingEntity entity, String animationId) {
        if (entity.level.isClientSide) {
            return;
        }

        ModMessages.sendToClients(new PlayPlayerAnimationS2CPacket(entity.getId(), animationId));
    }
}
