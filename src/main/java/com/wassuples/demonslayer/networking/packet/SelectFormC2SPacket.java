package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet that sets the player's currently selected form.
 *
 * <p>This is used by GUIs to select a specific form by index.
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>{@code isBda=true} selects a {@link DemonForms} value.</li>
 *   <li>{@code isBda=false} selects a {@link BreathingForms} value and updates the {@code PlayerStyle}
 *       capability.</li>
 * </ul>
 *
 * <p>This packet updates selection only; executing a form is handled elsewhere.
 */
public class SelectFormC2SPacket {
    private final int formIndex;
    private final boolean isBda;

    public SelectFormC2SPacket(int formIndex, boolean isBda) {
        this.formIndex = formIndex;
        this.isBda = isBda;
    }

    public SelectFormC2SPacket(FriendlyByteBuf buf) {
        this.formIndex = buf.readInt();
        this.isBda = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.formIndex);
        buf.writeBoolean(this.isBda);
    }

    /** Handles the packet on the logical server thread. */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (this.isBda) {
                // --- HANDLE DEMON FORM SELECTION ---
                DemonForms selectedDemonForm = DemonForms.values()[this.formIndex];

                player.getCapability(com.wassuples.demonslayer.demon.PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    demon.setCurrentDemonForm(selectedDemonForm);
                    player.sendSystemMessage(Component.literal("Switched to " + selectedDemonForm.getDisplayName()), true);
                });

            } else {
                // --- HANDLE BREATHING FORM SELECTION ---
                BreathingForms selectedBreathingForm = BreathingForms.values()[this.formIndex];

                player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    style.changeForm(this.formIndex);
                    player.sendSystemMessage(Component.literal("Switched to " + selectedBreathingForm.getDisplayName()), true);
                });
            }
        });
        return true;
    }
}
