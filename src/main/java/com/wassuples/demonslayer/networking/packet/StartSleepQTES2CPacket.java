package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.SleepQTEHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class StartSleepQTES2CPacket {
    private final IntList keySequence;

    public StartSleepQTES2CPacket(List<Integer> keySequence) {
        this.keySequence = new IntArrayList(keySequence);
    }

    public StartSleepQTES2CPacket(FriendlyByteBuf buf) {
        this.keySequence = buf.readIntIdList();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeIntIdList(this.keySequence);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                SleepQTEHandler.startQTE(this.keySequence);
            });
        });
        return true;
    }
}