package com.wassuples.demonslayer.mixin.toon;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Toon Force airwalk: lets the server reset vanilla's "Flying is not enabled" counter while airwalking. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
    @Accessor("aboveGroundTickCount")
    void setAboveGroundTickCount(int ticks);
}
