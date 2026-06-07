package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.minigame.*;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.training.StyleMinigameType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: instructs the client to open the style minigame screen for the given style.
 *
 * <p>Payload:
 * <ul>
 *   <li>{@code sessionId} - matches the server-side session for result correlation</li>
 *   <li>{@code style}     - which style's minigame to launch</li>
 *   <li>{@code tier}      - difficulty tier (1 = unlock, 2+ = form mastery training)</li>
 *   <li>{@code lives}     - remaining lives (for FLAME and SERPENT)</li>
 * </ul>
 */
public class StartStyleMinigameS2CPacket {

    private final UUID sessionId;
    private final BreathingStyles style;
    private final int tier;
    private final int lives;

    public StartStyleMinigameS2CPacket(UUID sessionId, BreathingStyles style, int tier, int lives) {
        this.sessionId = sessionId;
        this.style     = style;
        this.tier      = tier;
        this.lives     = lives;
    }

    public StartStyleMinigameS2CPacket(FriendlyByteBuf buf) {
        this.sessionId = buf.readUUID();
        this.style     = BreathingStyles.values()[buf.readVarInt()];
        this.tier      = buf.readVarInt();
        this.lives     = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeVarInt(style.ordinal());
        buf.writeVarInt(tier);
        buf.writeVarInt(lives);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            StyleMinigameType type = StyleMinigameType.forStyle(style);
            StyleMinigameScreen screen = switch (type) {
                case WATER   -> new WaterMinigameScreen(sessionId, tier);
                case FLAME   -> new FlameMinigameScreen(sessionId, tier, lives);
                case THUNDER -> new ThunderMinigameScreen(sessionId, tier);
                case MIST    -> new MistMinigameScreen(sessionId, tier);
                case WIND    -> new WindMinigameScreen(sessionId, tier);
                case STONE   -> new StoneMinigameScreen(sessionId, tier);
                case INSECT  -> new InsectMinigameScreen(sessionId, tier);
                case SOUND   -> new SoundMinigameScreen(sessionId, tier);
                case LOVE    -> new LoveMinigameScreen(sessionId, tier);
                case SERPENT -> new SerpentMinigameScreen(sessionId, tier, lives);
            };
            Minecraft mc = Minecraft.getInstance();
            // Dismiss any open screen (including the pause menu) before opening
            // the minigame, so a playermaster-forced session always works.
            if (mc.screen != null) {
                mc.setScreen(null);
            }
            mc.setScreen(screen);
        }));
        return true;
    }
}
