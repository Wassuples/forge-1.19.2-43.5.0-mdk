package com.wassuples.demonslayer.networking;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.networking.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(DemonSlayer.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ExampleC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ExampleC2SPacket::new)
                .encoder(ExampleC2SPacket::toBytes)
                .consumerMainThread(ExampleC2SPacket::handle)
                .add();

        net.messageBuilder(UseFormC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UseFormC2SPacket::new)
                .encoder(UseFormC2SPacket::toBytes)
                .consumerMainThread(UseFormC2SPacket::handle)
                .add();

        net.messageBuilder(ChangeFormC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChangeFormC2SPacket::new)
                .encoder(ChangeFormC2SPacket::toBytes)
                .consumerMainThread(ChangeFormC2SPacket::handle)
                .add();

        net.messageBuilder(ChargeFormC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChargeFormC2SPacket::new)
                .encoder(ChargeFormC2SPacket::toBytes)
                .consumerMainThread(ChargeFormC2SPacket::handle)
                .add();

        net.messageBuilder(PlayerStyleSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerStyleSyncS2CPacket::new)
                .encoder(PlayerStyleSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerStyleSyncS2CPacket::handle)
                .add();

        net.messageBuilder(PlayerBreathSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerBreathSyncS2CPacket::new)
                .encoder(PlayerBreathSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerBreathSyncS2CPacket::handle)
                .add();

        net.messageBuilder(VignetteS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(VignetteS2CPacket::new)
                .encoder(VignetteS2CPacket::toBytes)
                .consumerMainThread(VignetteS2CPacket::handle)
                .add();

        net.messageBuilder(SwingInAirC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwingInAirC2SPacket::new)
                .encoder(SwingInAirC2SPacket::toBytes)
                .consumerMainThread(SwingInAirC2SPacket::handle)
                .add();

        net.messageBuilder(PlayerProgressionSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerProgressionSyncS2CPacket::new)
                .encoder(PlayerProgressionSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerProgressionSyncS2CPacket::handle)
                .add();

        net.messageBuilder(SwitchStyleC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwitchStyleC2SPacket::new)
                .encoder(SwitchStyleC2SPacket::toBytes)
                .consumerMainThread(SwitchStyleC2SPacket::handle)
                .add();

        net.messageBuilder(PlayerMissionSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerMissionSyncS2CPacket::new)
                .encoder(PlayerMissionSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerMissionSyncS2CPacket::handle)
                .add();

        net.messageBuilder(AcceptMissionC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AcceptMissionC2SPacket::new)
                .encoder(AcceptMissionC2SPacket::toBytes)
                .consumerMainThread(AcceptMissionC2SPacket::handle)
                .add();

        net.messageBuilder(AbandonMissionC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AbandonMissionC2SPacket::new)
                .encoder(AbandonMissionC2SPacket::toBytes)
                .consumerMainThread(AbandonMissionC2SPacket::handle)
                .add();

        net.messageBuilder(PlayerDemonSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerDemonSyncS2CPacket::new)
                .encoder(PlayerDemonSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerDemonSyncS2CPacket::handle)
                .add();

        net.messageBuilder(DemonMeleeAttackC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DemonMeleeAttackC2SPacket::new)
                .encoder(DemonMeleeAttackC2SPacket::toBytes)
                .consumerMainThread(DemonMeleeAttackC2SPacket::handle)
                .add();

        net.messageBuilder(StartSleepQTES2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StartSleepQTES2CPacket::new)
                .encoder(StartSleepQTES2CPacket::toBytes)
                .consumerMainThread(StartSleepQTES2CPacket::handle)
                .add();

        net.messageBuilder(BrokeSleepC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BrokeSleepC2SPacket::new)
                .encoder(BrokeSleepC2SPacket::toBytes)
                .consumerMainThread(BrokeSleepC2SPacket::handle)
                .add();

        net.messageBuilder(SyncLesserEyesS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncLesserEyesS2CPacket::new)
                .encoder(SyncLesserEyesS2CPacket::toBytes)
                .consumerMainThread(SyncLesserEyesS2CPacket::handle)
                .add();

        net.messageBuilder(ClearLesserEyesS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClearLesserEyesS2CPacket::new)
                .encoder(ClearLesserEyesS2CPacket::toBytes)
                .consumerMainThread(ClearLesserEyesS2CPacket::handle)
                .add();

        net.messageBuilder(SpawnParticleStreamS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SpawnParticleStreamS2CPacket::new)
                .encoder(SpawnParticleStreamS2CPacket::toBytes)
                .consumerMainThread(SpawnParticleStreamS2CPacket::handle)
                .add();

        net.messageBuilder(SpawnSonicBeamS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SpawnSonicBeamS2CPacket::new)
                .encoder(SpawnSonicBeamS2CPacket::toBytes)
                .consumerMainThread(SpawnSonicBeamS2CPacket::handle)
                .add();

        net.messageBuilder(UpdateGlowDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UpdateGlowDataPacket::encode)
                .decoder(UpdateGlowDataPacket::new)
                .consumerMainThread(UpdateGlowDataPacket::handle)
                .add();

        net.messageBuilder(BodhisattvaInputC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BodhisattvaInputC2SPacket::new)
                .encoder(BodhisattvaInputC2SPacket::toBytes)
                .consumerMainThread(BodhisattvaInputC2SPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
