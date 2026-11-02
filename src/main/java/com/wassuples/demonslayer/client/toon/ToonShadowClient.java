package com.wassuples.demonslayer.client.toon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which entities currently have their shadow tossed. Shadow anchors refresh their owner every
 * client tick; an entry that isn't refreshed for a couple of ticks (anchor gone/out of range) expires,
 * so the vanilla shadow comes back automatically.
 */
@OnlyIn(Dist.CLIENT)
public final class ToonShadowClient {
    private static final Map<Integer, Long> TOSSED_UNTIL = new HashMap<>();
    private static long clientTicks;

    private ToonShadowClient() {
    }

    public static void markTossed(int ownerEntityId) {
        TOSSED_UNTIL.put(ownerEntityId, clientTicks + 2);
    }

    public static boolean isShadowTossed(Entity entity) {
        if (TOSSED_UNTIL.isEmpty() || !entity.level.isClientSide) return false;
        Long until = TOSSED_UNTIL.get(entity.getId());
        return until != null && clientTicks <= until;
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            TOSSED_UNTIL.clear();
            return;
        }
        clientTicks++;
        TOSSED_UNTIL.values().removeIf(until -> until < clientTicks);
    }
}
