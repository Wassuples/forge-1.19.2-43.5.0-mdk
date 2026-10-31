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

        net.messageBuilder(EquipBdaC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(EquipBdaC2SPacket::new)
                .encoder(EquipBdaC2SPacket::toBytes)
                .consumerMainThread(EquipBdaC2SPacket::handle)
                .add();

        net.messageBuilder(VignetteS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(VignetteS2CPacket::new)
                .encoder(VignetteS2CPacket::toBytes)
                .consumerMainThread(VignetteS2CPacket::handle)
                .add();

        net.messageBuilder(PieSplatS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PieSplatS2CPacket::new)
                .encoder(PieSplatS2CPacket::toBytes)
                .consumerMainThread(PieSplatS2CPacket::handle)
                .add();

        net.messageBuilder(ToonPocketS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ToonPocketS2CPacket::new)
                .encoder(ToonPocketS2CPacket::toBytes)
                .consumerMainThread(ToonPocketS2CPacket::handle)
                .add();

        net.messageBuilder(PlayPlayerAnimationS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayPlayerAnimationS2CPacket::new)
                .encoder(PlayPlayerAnimationS2CPacket::toBytes)
                .consumerMainThread(PlayPlayerAnimationS2CPacket::handle)
                .add();

        net.messageBuilder(SwingInAirC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwingInAirC2SPacket::new)
                .encoder(SwingInAirC2SPacket::toBytes)
                .consumerMainThread(SwingInAirC2SPacket::handle)
                .add();

        net.messageBuilder(SelectFormC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectFormC2SPacket::new)
                .encoder(SelectFormC2SPacket::toBytes)
                .consumerMainThread(SelectFormC2SPacket::handle)
                .add();

        net.messageBuilder(UpdateCompassInfoPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(UpdateCompassInfoPacket::new)
                .encoder(UpdateCompassInfoPacket::encode)
                .consumerMainThread(UpdateCompassInfoPacket::handle)
                .add();

        net.messageBuilder(PlayerProgressionSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerProgressionSyncS2CPacket::new)
                .encoder(PlayerProgressionSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerProgressionSyncS2CPacket::handle)
                .add();

        net.messageBuilder(PurchasePassiveC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PurchasePassiveC2SPacket::new)
                .encoder(PurchasePassiveC2SPacket::toBytes)
                .consumerMainThread(PurchasePassiveC2SPacket::handle)
                .add();

        net.messageBuilder(RankUpC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RankUpC2SPacket::new)
                .encoder(RankUpC2SPacket::toBytes)
                .consumerMainThread(RankUpC2SPacket::handle)
                .add();

        net.messageBuilder(SwitchStyleC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwitchStyleC2SPacket::new)
                .encoder(SwitchStyleC2SPacket::toBytes)
                .consumerMainThread(SwitchStyleC2SPacket::handle)
                .add();

        net.messageBuilder(UpgradeBdaC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UpgradeBdaC2SPacket::new)
                .encoder(UpgradeBdaC2SPacket::toBytes)
                .consumerMainThread(UpgradeBdaC2SPacket::handle)
                .add();

        net.messageBuilder(PurchaseBdaC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PurchaseBdaC2SPacket::new)
                .encoder(PurchaseBdaC2SPacket::toBytes)
                .consumerMainThread(PurchaseBdaC2SPacket::handle)
                .add();

        net.messageBuilder(TogglePassiveC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TogglePassiveC2SPacket::new)
                .encoder(TogglePassiveC2SPacket::toBytes)
                .consumerMainThread(TogglePassiveC2SPacket::handle)
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

        net.messageBuilder(PlayerGravitySyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerGravitySyncS2CPacket::new)
                .encoder(PlayerGravitySyncS2CPacket::toBytes)
                .consumerMainThread(PlayerGravitySyncS2CPacket::handle)
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

        net.messageBuilder(ToggleBdaModeC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleBdaModeC2SPacket::toBytes)
                .decoder(ToggleBdaModeC2SPacket::new)
                .consumerMainThread(ToggleBdaModeC2SPacket::handle)
                .add();

        net.messageBuilder(StopFormS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StopFormS2CPacket::new)
                .encoder(StopFormS2CPacket::toBytes)
                .consumerMainThread(StopFormS2CPacket::handle)
                .add();

        net.messageBuilder(AbilityKeyC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AbilityKeyC2SPacket::new)
                .encoder(AbilityKeyC2SPacket::toBytes)
                .consumerMainThread(AbilityKeyC2SPacket::handle)
                .add();

        net.messageBuilder(OpenCrowDispatchS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenCrowDispatchS2CPacket::new)
                .encoder(OpenCrowDispatchS2CPacket::toBytes)
                .consumerMainThread(OpenCrowDispatchS2CPacket::handle)
                .add();

        net.messageBuilder(DemonThreatS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(DemonThreatS2CPacket::new)
                .encoder(DemonThreatS2CPacket::toBytes)
                .consumerMainThread(DemonThreatS2CPacket::handle)
                .add();

        net.messageBuilder(OpenTamayoOfferS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenTamayoOfferS2CPacket::new)
                .encoder(OpenTamayoOfferS2CPacket::toBytes)
                .consumerMainThread(OpenTamayoOfferS2CPacket::handle)
                .add();

        net.messageBuilder(TamayoOfferResponseC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TamayoOfferResponseC2SPacket::new)
                .encoder(TamayoOfferResponseC2SPacket::toBytes)
                .consumerMainThread(TamayoOfferResponseC2SPacket::handle)
                .add();

        net.messageBuilder(HeatLightningCracksS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeatLightningCracksS2CPacket::encode)
                .decoder(HeatLightningCracksS2CPacket::new)
                .consumerMainThread(HeatLightningCracksS2CPacket::handle)
                .add();

        // ── Challenge system ──────────────────────────────────────────────
        net.messageBuilder(RequestChallengeListC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestChallengeListC2SPacket::new)
                .encoder(RequestChallengeListC2SPacket::toBytes)
                .consumerMainThread(RequestChallengeListC2SPacket::handle)
                .add();

        net.messageBuilder(ChallengeListS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ChallengeListS2CPacket::new)
                .encoder(ChallengeListS2CPacket::toBytes)
                .consumerMainThread(ChallengeListS2CPacket::handle)
                .add();

        net.messageBuilder(InitiateChallengeC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(InitiateChallengeC2SPacket::new)
                .encoder(InitiateChallengeC2SPacket::toBytes)
                .consumerMainThread(InitiateChallengeC2SPacket::handle)
                .add();

        net.messageBuilder(StartStyleMinigameS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(StartStyleMinigameS2CPacket::new)
            .encoder(StartStyleMinigameS2CPacket::toBytes)
            .consumerMainThread(StartStyleMinigameS2CPacket::handle)
            .add();

        net.messageBuilder(StyleMinigameResultC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(StyleMinigameResultC2SPacket::new)
            .encoder(StyleMinigameResultC2SPacket::toBytes)
            .consumerMainThread(StyleMinigameResultC2SPacket::handle)
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
