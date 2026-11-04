package com.wassuples.demonslayer.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The Hand Demon's theme, as the server sees it. Sent to everyone on the mountain twice a second while it plays, and
 * at once whenever something changes. {@link #HINT} is the one-off foreshadowing on nights 3-6.
 */
public class HandDemonMusicS2CPacket {
    public static final byte IDLE = 0, FIGHT = 1, DEATH = 2, HINT = 3, PRELUDE = 4;

    public final byte mode;
    public final int entityId;
    /** FIGHT: 1 crawl (100-75%), 2 grudge (75-50%), 3 thirteen (50-20%), 4 fear (&lt;20%). */
    public final byte phase;
    /** Bumped every time the demon regrows back into an earlier phase (the client plays the rewind). */
    public final int rewinds;
    /** 1 while it sees its quarry; falls toward 0 as it loses track of everyone. */
    public final float intensity;
    /** Tanjiro is in the fight: the count ends on fourteen. */
    public final boolean fourteen;
    /** Ticks since it first spotted someone (the client only plays the intro for a fresh fight). */
    public final int fightAge;
    /** DEATH: ticks since the killing blow, the outcome (0 waiting, 1 hand taken, 2 not taken) and when it came. */
    public final int deathTime;
    public final byte outcome;
    public final int outcomeAt;
    /** HINT: the night (3-6) and where the den is. */
    public final byte night;
    public final BlockPos den;

    private HandDemonMusicS2CPacket(byte mode, int entityId, byte phase, int rewinds, float intensity, boolean fourteen,
                                    int fightAge, int deathTime, byte outcome, int outcomeAt, byte night, BlockPos den) {
        this.mode = mode;
        this.entityId = entityId;
        this.phase = phase;
        this.rewinds = rewinds;
        this.intensity = intensity;
        this.fourteen = fourteen;
        this.fightAge = fightAge;
        this.deathTime = deathTime;
        this.outcome = outcome;
        this.outcomeAt = outcomeAt;
        this.night = night;
        this.den = den;
    }

    public static HandDemonMusicS2CPacket fight(int entityId, int phase, int rewinds, float intensity, boolean fourteen, int fightAge) {
        return new HandDemonMusicS2CPacket(FIGHT, entityId, (byte) phase, rewinds, intensity, fourteen, fightAge, 0, (byte) 0, 0, (byte) 0, BlockPos.ZERO);
    }

    public static HandDemonMusicS2CPacket idle(int entityId) {
        return new HandDemonMusicS2CPacket(IDLE, entityId, (byte) 0, 0, 0F, false, 0, 0, (byte) 0, 0, (byte) 0, BlockPos.ZERO);
    }

    public static HandDemonMusicS2CPacket death(int entityId, int deathTime, int outcome, int outcomeAt) {
        return new HandDemonMusicS2CPacket(DEATH, entityId, (byte) 0, 0, 1F, false, 0, deathTime, (byte) outcome, outcomeAt, (byte) 0, BlockPos.ZERO);
    }

    /** Night 7: the theme's intro, before the Hand Demon shows itself. {@code age} is ticks since it began. */
    public static HandDemonMusicS2CPacket prelude(int age) {
        return new HandDemonMusicS2CPacket(PRELUDE, -1, (byte) 0, 0, 1F, false, age, 0, (byte) 0, 0, (byte) 0, BlockPos.ZERO);
    }

    public static HandDemonMusicS2CPacket hint(int night, BlockPos den) {
        return new HandDemonMusicS2CPacket(HINT, -1, (byte) 0, 0, 1F, false, 0, 0, (byte) 0, 0, (byte) night, den);
    }

    public HandDemonMusicS2CPacket(FriendlyByteBuf buf) {
        this(buf.readByte(), buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readFloat(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readByte(), buf.readBlockPos());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(mode);
        buf.writeVarInt(entityId);
        buf.writeByte(phase);
        buf.writeVarInt(rewinds);
        buf.writeFloat(intensity);
        buf.writeBoolean(fourteen);
        buf.writeVarInt(fightAge);
        buf.writeVarInt(deathTime);
        buf.writeByte(outcome);
        buf.writeVarInt(outcomeAt);
        buf.writeByte(night);
        buf.writeBlockPos(den);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.wassuples.demonslayer.client.music.HandDemonConductor.receive(this)));
        context.setPacketHandled(true);
        return true;
    }
}
