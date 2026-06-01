package com.wassuples.demonslayer.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

// This packet is sent from SERVER to CLIENT

/**
 * Server-to-client packet: stop form.
 *
 * <p>Previously this packet called {@code ClientEvents.resetAbilityKeyState()}, which set
 * {@code abilityKeyIsDown = false}. That caused a race condition:
 *
 * <ol>
 *   <li>Client presses key → sends PRESS, sets {@code abilityKeyIsDown = true}.</li>
 *   <li>Server starts Form A, then stops it (e.g. interrupted) → sends {@code StopFormS2CPacket}.</li>
 *   <li>Before that packet arrives, client presses key again → sends PRESS for Form B,
 *       {@code abilityKeyIsDown} still {@code true}.</li>
 *   <li>Server starts Form B.</li>
 *   <li>Client receives the stale {@code StopFormS2CPacket} from step 2 → resets
 *       {@code abilityKeyIsDown = false}.</li>
 *   <li>Player releases key → {@code !isDown() && abilityKeyIsDown} → {@code false} → RELEASE
 *       never sent → {@code ChargeableForm} stuck charging, blocking all new activations.</li>
 * </ol>
 *
 * <p>The fix: do not touch client key-tracking state from this packet at all. The client's
 * {@code abilityKeyIsDown} flag is managed purely by physical key events. When the key is
 * eventually released, RELEASE is sent naturally, and the server ignores it gracefully if no
 * {@code ChargeableForm} is active.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class StopFormS2CPacket {

    public StopFormS2CPacket() {
    }

    public StopFormS2CPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Intentionally empty: do NOT reset abilityKeyIsDown here.
            // See class JavaDoc for explanation of the race condition this previously caused.
        });
        return true;
    }
}
