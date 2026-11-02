package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.networking.packet.PlayerMissionSyncS2CPacket;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives story missions: validates requests from the client, ticks the active mission, routes combat/death events
 * to it, removes stale scoped entities, and syncs the HUD tracker. All of it is server-side.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MissionManager {
    private MissionManager() {
    }

    static Optional<PlayerMissions> data(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).resolve();
    }

    /** Context for the player's active mission, or null. */
    @Nullable
    private static MissionContext activeContext(ServerPlayer player) {
        PlayerMissions data = data(player).orElse(null);
        if (data == null || !data.hasActive()) return null;
        StoryMission mission = MissionRegistry.get(data.getActiveId());
        if (mission == null || !mission.isPlayable()) {
            data.clearActive();       // script removed since the save was made
            return null;
        }
        return new MissionContext(player, data, mission);
    }

    // ================================================================== requests (validated)

    /** Starts a mission if every condition holds. Returns the refusal reason, or null on success. */
    /** Accept/forfeit spam guard (accepting can run an expensive world search). */
    private static final java.util.Map<UUID, Long> LAST_REQUEST = new java.util.HashMap<>();
    private static final long REQUEST_COOLDOWN = 100L;

    private static boolean onCooldown(ServerPlayer player) {
        long now = player.getLevel().getGameTime();
        Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < REQUEST_COOLDOWN) return true;
        LAST_REQUEST.put(player.getUUID(), now);
        return false;
    }

    @Nullable
    public static Component tryAccept(ServerPlayer player, ResourceLocation id, boolean force) {
        return tryAccept(player, id, force, false);
    }

    /**
     * @param fromGiver the request came from the mission's giver in the world (an NPC dialogue), not the crow list;
     *                  missions that {@link StoryMission#requiresGiver() require a giver} only start that way.
     */
    @Nullable
    public static Component tryAccept(ServerPlayer player, ResourceLocation id, boolean force, boolean fromGiver) {
        StoryMission mission = MissionRegistry.get(id);
        if (!force && mission != null && mission.requiresGiver() && !fromGiver) return mission.giverHint();
        if (!force && !fromGiver && onCooldown(player)) return Component.translatable("mission.demonslayer.refuse.cooldown");
        PlayerMissions data = data(player).orElse(null);
        if (mission == null || data == null) return Component.translatable("mission.demonslayer.refuse.unknown");
        if (!mission.isPlayable()) return Component.translatable("mission.demonslayer.refuse.sealed");
        if (data.isActive(id)) return Component.translatable("mission.demonslayer.refuse.already_active");
        if (!force) {
            if (data.hasActive()) return Component.translatable("mission.demonslayer.refuse.other_active");
            if (data.isCompleted(id)) return Component.translatable("mission.demonslayer.refuse.completed");
            if (!mission.isUnlocked(player, data)) return mission.lockedReason(player, data);
            Component blocker = mission.acceptBlocker(player, data);
            if (blocker != null) return blocker;
        } else if (data.hasActive()) {
            abandon(player, data.getActiveId(), true);
        }

        CompoundTag state = new CompoundTag();
        state.putInt("run", player.getRandom().nextInt(Integer.MAX_VALUE - 1) + 1);
        data.setActive(id, state);
        MissionContext ctx = new MissionContext(player, data, mission);
        mission.onAccept(ctx);
        sync(player);
        return null;
    }

    /** A story notice on the player's HUD (never chat); {@code warning} draws it in red. */
    public static void notify(ServerPlayer player, Component message, boolean warning) {
        ModMessages.sendToPlayer(new com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket(
                warning ? com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket.WARNING
                        : com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket.NOTICE,
                message, Component.empty(), warning ? 0xFF7070 : 0xEDE3E3, warning ? 160 : 120), player);
    }

    /** Forfeits the active mission (counts as a failure, no story consequences). */
    public static boolean abandon(ServerPlayer player, ResourceLocation id, boolean silent) {
        MissionContext ctx = activeContext(player);
        if (ctx == null || !ctx.mission().id().equals(id)) return false;
        if (!silent) ctx.notice(Component.translatable("mission.demonslayer.forfeit", ctx.mission().title()));
        finish(ctx, false, null, null);
        return true;
    }

    /**
     * Runs {@code action} against the player's active mission if it is {@code id} (used by NPC dialogue answers that
     * move a story along), then syncs. Returns false when that mission isn't active.
     */
    public static boolean withActive(ServerPlayer player, ResourceLocation id, java.util.function.Consumer<MissionContext> action) {
        MissionContext ctx = activeContext(player);
        if (ctx == null || !ctx.mission().id().equals(id)) return false;
        action.accept(ctx);
        if (!ctx.hasEnded()) sync(player);
        return true;
    }

    /** Admin: advance the active mission to its next step (testing). */
    public static boolean debugAdvance(ServerPlayer player) {
        MissionContext ctx = activeContext(player);
        if (ctx == null) return false;
        ctx.mission().onDebugAdvance(ctx);
        if (!ctx.hasEnded()) sync(player);
        return true;
    }

    /** Admin: complete the active mission on a branch. */
    public static boolean forceComplete(ServerPlayer player, String outcome) {
        MissionContext ctx = activeContext(player);
        if (ctx == null) return false;
        ctx.complete(outcome);
        return true;
    }

    /** Admin: reset one mission (or all). */
    public static void reset(ServerPlayer player, @Nullable ResourceLocation id) {
        MissionContext ctx = activeContext(player);
        if (ctx != null && (id == null || ctx.mission().id().equals(id))) {
            ctx.mission().onEnd(ctx, false);
            ctx.discardAllScoped();
        }
        data(player).ifPresent(d -> d.reset(id));
        sync(player);
    }

    static void finish(MissionContext ctx, boolean success, @Nullable String outcome, @Nullable Component reason) {
        if (ctx.hasEnded()) return;
        ctx.setEnded();
        StoryMission mission = ctx.mission();
        if (success) mission.onComplete(ctx, outcome == null ? "default" : outcome);
        mission.onEnd(ctx, success);
        ctx.discardAllScoped();
        PlayerMissions data = ctx.data();
        if (success) {
            data.markCompleted(mission.id(), outcome == null ? "default" : outcome);
        } else {
            data.addFailure(mission.id());
            if (reason != null) {
                ctx.banner(Component.translatable("mission.demonslayer.failed", mission.title()), reason, 0xD8263A, 120);
                ctx.sound(ModSounds.MISSION_FAILED.get(), 0.9F, 1.0F);
            }
        }
        data.clearActive();
        sync(ctx.player());
    }

    private static void afterHook(MissionContext ctx) {
        if (!ctx.hasEnded() && ctx.isDirty()) sync(ctx.player());
    }

    // ================================================================== views & sync

    public static void sync(ServerPlayer player) {
        PlayerMissions data = data(player).orElse(null);
        if (data == null) return;
        CompoundTag nbt = new CompoundTag();
        data.saveNBTData(nbt);
        nbt.remove("activeState");   // scripts' internals stay on the server
        MissionTracker tracker = null;
        MissionContext ctx = activeContext(player);
        if (ctx != null) tracker = ctx.mission().tracker(ctx);
        ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(nbt, tracker), player);
    }

    /** Every mission as the crow's list shows it for this player. */
    public static List<MissionView> buildViews(ServerPlayer player) {
        PlayerMissions data = data(player).orElse(new PlayerMissions());
        List<MissionView> views = new ArrayList<>();
        for (StoryMission m : MissionRegistry.getAllMissions()) {
            MissionView.Status status;
            Component note;
            if (data.isActive(m.id())) {
                status = MissionView.Status.ACTIVE;
                note = Component.translatable("mission.demonslayer.status.active");
            } else if (data.isCompleted(m.id())) {
                status = MissionView.Status.COMPLETED;
                note = Component.translatable("mission.demonslayer.status.completed",
                        Component.translatable("mission.demonslayer.outcome." + data.getOutcome(m.id())));
            } else if (!m.isPlayable()) {
                status = MissionView.Status.LOCKED;
                note = Component.translatable("mission.demonslayer.status.sealed");
            } else if (!m.isUnlocked(player, data)) {
                status = MissionView.Status.LOCKED;
                note = m.lockedReason(player, data);
            } else {
                Component blocker = m.acceptBlocker(player, data);
                if (blocker == null && m.requiresGiver()) blocker = m.giverHint();   // started in person, not through the crow
                status = blocker == null ? MissionView.Status.AVAILABLE : MissionView.Status.LOCKED;
                note = blocker == null ? Component.translatable("mission.demonslayer.status.available") : blocker;
            }
            views.add(new MissionView(m.id(), m.number(), m.arc(), m.title(), m.location(), m.summary(), status, note, m.briefing()));
        }
        return views;
    }

    // ================================================================== events

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        data(player).ifPresent(d -> com.wassuples.demonslayer.missions.story.FinalSelectionMission.tickBladeDelivery(player, d));
        MissionContext ctx = activeContext(player);
        if (ctx == null) return;
        ctx.mission().tick(ctx);
        if (!ctx.hasEnded()) sync(player);   // the tracker shows live timers
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerMissions data = data(player).orElse(null);
        if (data == null) return;
        // Players who earned a rank under the old system already passed the Final Selection.
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(p -> {
            if (p.getRank().ordinal() >= Rank.MIZUNOTO.ordinal() && !data.isCompleted(MissionRegistry.FINAL_SELECTION_ID)) {
                data.markCompleted(MissionRegistry.FINAL_SELECTION_ID, "legacy");
            }
        });
        giveLetterIfNew(player, data);
        MissionContext ctx = activeContext(player);
        if (ctx != null) ctx.mission().onLogin(ctx);
        sync(player);
    }

    /** New civilians start with their teacher's letter of introduction, which points the way to Mt. Fujikasane. */
    private static void giveLetterIfNew(ServerPlayer player, PlayerMissions data) {
        if (data.hasFlag("letter_of_introduction.given") || data.isCompleted(MissionRegistry.FINAL_SELECTION_ID)) return;
        boolean demon = player.getCapability(com.wassuples.demonslayer.demon.PlayerDemonProvider.PLAYER_DEMON).map(d -> d.isDemon()).orElse(false);
        if (demon) return;
        data.setFlag("letter_of_introduction.given");
        net.minecraft.world.item.ItemStack letter = com.wassuples.demonslayer.item.custom.LetterOfIntroductionItem.create(player,
                com.wassuples.demonslayer.item.ModItems.LETTER_OF_INTRODUCTION.get());
        if (!player.getInventory().add(letter)) player.drop(letter, false);
        notify(player, Component.translatable("item.demonslayer.letter_of_introduction.received"), false);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    /** Runs after Unyielding (HIGHEST) and before the death-persistence save in ModEvents (NORMAL). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level.isClientSide) return;

        if (victim instanceof ServerPlayer player) {
            MissionContext ctx = activeContext(player);
            if (ctx != null) {
                ctx.mission().onPlayerDeath(ctx, event.getSource());
                afterHook(ctx);
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != victim) {
            MissionContext ctx = activeContext(killer);
            if (ctx != null) {
                ctx.mission().onKill(ctx, victim, event.getSource());
                afterHook(ctx);
            }
        }

        ScopedOwner scoped = scopedOwner(victim);
        if (scoped != null) {
            scoped.ctx.mission().onScopedDeath(scoped.ctx, scoped.key, victim, event.getSource());
            afterHook(scoped.ctx);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level.isClientSide || event.getAmount() <= 0) return;
        ScopedOwner scoped = scopedOwner(victim);
        if (scoped != null) {
            scoped.ctx.mission().onScopedHurt(scoped.ctx, scoped.key, victim, event.getSource(), Math.min(event.getAmount(), victim.getHealth()));
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MissionContext ctx = activeContext(player);
        if (ctx != null && !ctx.mission().allowsSleep(ctx)) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            ctx.warn(Component.translatable("mission.demonslayer.no_sleep", ctx.mission().title()));
        }
    }

    /** Entities spawned by a mission run that has since ended (or been replaced) are removed as they load. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getPersistentData().contains(MissionContext.SCOPE_TAG)) return;
        CompoundTag tag = entity.getPersistentData().getCompound(MissionContext.SCOPE_TAG);
        if (!tag.hasUUID("owner") || !(event.getLevel() instanceof ServerLevel level)) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tag.getUUID("owner"));
        if (owner == null) return;   // can't tell while the owner is offline; checked again later
        if (!isCurrentScoped(owner, tag, entity.getUUID())) event.setCanceled(true);
    }

    /** Scoped entities accepted while their owner was offline are re-checked every few seconds once the owner is back. */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level.isClientSide || entity.tickCount % 100 != 7 || !entity.getPersistentData().contains(MissionContext.SCOPE_TAG)) return;
        if (!(entity.level instanceof ServerLevel level)) return;
        CompoundTag tag = entity.getPersistentData().getCompound(MissionContext.SCOPE_TAG);
        if (!tag.hasUUID("owner")) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tag.getUUID("owner"));
        if (owner != null && !isCurrentScoped(owner, tag, entity.getUUID())) entity.discard();
    }

    private static boolean isCurrentScoped(ServerPlayer owner, CompoundTag tag, UUID entityId) {
        PlayerMissions data = data(owner).orElse(null);
        if (data == null || !data.hasActive()) return false;
        if (!data.getActiveId().toString().equals(tag.getString("mission"))) return false;
        CompoundTag state = data.getActiveState();
        if (state.getInt("run") != tag.getInt("run")) return false;
        CompoundTag entities = state.getCompound("entities");
        String key = tag.getString("key");
        return entities.hasUUID(key) && entities.getUUID(key).equals(entityId);
    }

    private record ScopedOwner(MissionContext ctx, String key) {
    }

    @Nullable
    private static ScopedOwner scopedOwner(Entity entity) {
        if (!entity.getPersistentData().contains(MissionContext.SCOPE_TAG) || !(entity.level instanceof ServerLevel level)) return null;
        CompoundTag tag = entity.getPersistentData().getCompound(MissionContext.SCOPE_TAG);
        if (!tag.hasUUID("owner")) return null;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tag.getUUID("owner"));
        if (owner == null || !isCurrentScoped(owner, tag, entity.getUUID())) return null;
        MissionContext ctx = activeContext(owner);
        return ctx == null ? null : new ScopedOwner(ctx, tag.getString("key"));
    }
}
