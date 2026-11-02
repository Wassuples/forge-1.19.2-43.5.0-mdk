package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.CooldownSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each player's client informed of their form cooldowns (for the Demon HUD and the form wheel). Every few
 * ticks the server snapshots the cooldowns as remaining ticks and sends them only if the set of cooldowns changed
 * or one was restarted, so idle players cost nothing.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CooldownSyncEvents {
    private static final String NBT_KEY = "player_cooldowns";
    /** Last expiry (game time) sent per player and form key. */
    private static final Map<UUID, Map<String, Long>> LAST_SENT = new ConcurrentHashMap<>();

    private CooldownSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 4 != 0) return;
        player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS).ifPresent(cooldowns -> {
            CompoundTag wrapper = new CompoundTag();
            cooldowns.saveNBTData(player.level, wrapper);
            CompoundTag remaining = wrapper.getCompound(NBT_KEY);
            long now = player.level.getGameTime();
            Map<String, Long> last = LAST_SENT.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
            boolean changed = last.size() != remaining.getAllKeys().size();
            if (!changed) {
                for (String key : remaining.getAllKeys()) {
                    Long sent = last.get(key);
                    long expiry = now + remaining.getInt(key);
                    if (sent == null || Math.abs(sent - expiry) > 2) {
                        changed = true;
                        break;
                    }
                }
            }
            // Expired entries drop out of the snapshot on their own; tell the client once when the last one ends.
            if (!changed) return;
            last.clear();
            for (String key : remaining.getAllKeys()) last.put(key, now + remaining.getInt(key));
            ModMessages.sendToPlayer(new CooldownSyncS2CPacket(remaining), player);
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID()); // forces a fresh sync
    }
}
