package com.wassuples.demonslayer.missions.story;

import com.wassuples.demonslayer.dialogue.DialogueNode;
import com.wassuples.demonslayer.dialogue.DialogueSession;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.missions.MissionContext;
import com.wassuples.demonslayer.missions.MissionManager;
import com.wassuples.demonslayer.missions.PlayerMissions;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneResidents;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Everything on the mountain besides the main thread of the Final Selection:
 *
 * <h3>The other candidates</h3>
 * Twelve more hopefuls (the selection takes about twenty) climb the stairs to the shrine when the player accepts. They
 * listen to the briefing, spread across the summit during the trial and can be killed there; the survivors gather at
 * the shrine at the end. Each is a mission-scoped {@link StoryNpcEntity} ({@link StoryCharacter#CANDIDATE}) under the
 * key {@code cand<N>}.
 *
 * <h3>Side quests</h3> (all optional, shown as optional lines on the tracker)
 * <ul>
 *   <li><b>Cornered</b> (nights 2-6): one of the other candidates is surrounded by demons; reach them and clear them
 *       out before they are killed.</li>
 *   <li><b>Inosuke's challenge</b>: kill 12 demons before dawn.</li>
 *   <li><b>Zenitsu's plea</b>: clear 8 demons out of the trees around his hiding place.</li>
 *   <li><b>The horned demon</b> (Tanjiro, from night 3): hunt a demon that has eaten more than the others, near the
 *       ruined hut.</li>
 *   <li><b>The lost sword</b>: a candidate dropped their sword near the pit where the Hand Demon dens; bring it back.</li>
 *   <li><b>Kanao's coin</b>: once a day she tosses her coin; heads, she shares a rice ball.</li>
 * </ul>
 * Rewards are rations (onigiri), wisteria incense and a little mastery.
 */
final class FinalSelectionExtras {
    static final int CANDIDATES = 12;
    private static final String[] NAMES = {"Kenta Ishida", "Yui Hoshino", "Hayato Mori", "Sayo Kanda", "Daisuke Ono", "Haru Imai",
            "Shinji Arai", "Mitsu Ogawa", "Ryota Kudo", "Chiyo Nakai", "Takumi Sano", "Fumi Sakata"};
    /** The common styles taught by cultivators across the country (never a Hashira's own, nor Sun or Moon). */
    private static final com.wassuples.demonslayer.styles.BreathingStyles[] STYLES = {
            com.wassuples.demonslayer.styles.BreathingStyles.WATER, com.wassuples.demonslayer.styles.BreathingStyles.FLAME,
            com.wassuples.demonslayer.styles.BreathingStyles.THUNDER, com.wassuples.demonslayer.styles.BreathingStyles.WIND,
            com.wassuples.demonslayer.styles.BreathingStyles.STONE};
    // state keys
    private static final String CANDS = "cands", QUESTS = "quests";
    private static final String CORNER = "corner", RACE = "race", ZEN = "zenitsu", HORN = "horned", SWORD = "sword", COIN = "coinDay";
    /** Quest status values. */
    private static final int OPEN = 0, ACTIVE = 1, DONE = 2, FAILED = 3;
    private static final String CORNER_TAG = "demonslayer.fs_corner", SWORD_TAG = "demonslayer.fs_sword";
    private static final int RACE_GOAL = 12, ZEN_GOAL = 8;

    private FinalSelectionExtras() {
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("mission.demonslayer.final_selection." + key, args);
    }

    private static CompoundTag quests(MissionContext ctx) {
        CompoundTag q = ctx.state().getCompound(QUESTS);
        ctx.state().put(QUESTS, q);
        return q;
    }

    private static CompoundTag quest(MissionContext ctx, String id) {
        CompoundTag q = quests(ctx);
        CompoundTag one = q.getCompound(id);
        q.put(id, one);
        return one;
    }

    private static boolean status(CompoundTag quest, int status) {
        return quest.contains("status") && quest.getInt("status") == status;
    }

    // ================================================================== the other candidates

    /** They come up the stairs from the foot of the mountain to wait at the shrine. */
    static void spawnCandidates(MissionContext ctx, FujikasaneLayout layout) {
        ServerLevel level = ctx.level();
        CompoundTag cands = new CompoundTag();
        for (int i = 0; i < CANDIDATES; i++) {
            StoryNpcEntity npc = ModEntityTypes.STORY_NPC.get().create(level);
            if (npc == null) continue;
            npc.setCandidate(NAMES[i], i % StoryCharacter.CANDIDATE_VARIANTS, STYLES[i % STYLES.length]);
            int dx = -2 + i % 5, dz = FujikasaneLayout.STAIR_BOTTOM + 2 + i / 5 * 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, layout.centerX + dx, layout.centerZ + dz);
            npc.moveTo(layout.centerX + dx + 0.5, y, layout.centerZ + dz + 0.5, 180F, 0F);
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.EVENT, null, null);
            npc.setAnchor(layout.plazaRingSpot(i), 3);
            ctx.spawnScoped(npc, "cand" + i);
            CompoundTag c = new CompoundTag();
            c.putString("name", NAMES[i]);
            cands.put("cand" + i, c);
        }
        ctx.state().put(CANDS, cands);
    }

    /** Every second: direct the candidates and run the side quests. */
    static void tick(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        String phase = s.getString(FinalSelectionMission.PHASE);
        if (ctx.player().tickCount % 100 < 20) directCandidates(ctx, layout, phase);
        if (!FinalSelectionMission.TRIAL.equals(phase)) return;
        if (status(quest(ctx, SWORD), ACTIVE)) {
            // the dropped sword glints in the dark by the den
            for (ItemEntity item : ctx.level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(layout.den()).inflate(12.0D),
                    e -> e.getItem().hasTag() && e.getItem().getTag().getInt(SWORD_TAG) == ctx.runId())) {
                ctx.level().sendParticles(com.wassuples.demonslayer.particle.ModParticles.RESOLVE_SPARK.get(), item.getX(), item.getY() + 0.4, item.getZ(),
                        2, 0.1, 0.1, 0.1, 0.01);
            }
        }
        tickCornered(ctx, layout);
        tickHorned(ctx, layout);
    }

    private static void directCandidates(MissionContext ctx, FujikasaneLayout layout, String phase) {
        CompoundTag cands = ctx.state().getCompound(CANDS);
        int night = FinalSelectionMission.activeNight(ctx.player());
        CompoundTag corner = ctx.state().getCompound(QUESTS).getCompound(CORNER);
        String cornered = status(corner, ACTIVE) ? corner.getString("who") : "";
        for (int i = 0; i < CANDIDATES; i++) {
            String key = "cand" + i;
            if (!cands.contains(key) || cands.getCompound(key).getBoolean("dead")) continue;
            if (!(ctx.scoped(key) instanceof StoryNpcEntity npc)) continue;
            if (key.equals(cornered)) continue;   // pinned where the demons have them
            if (FinalSelectionMission.TRIAL.equals(phase)) npc.setAnchor(layout.summitPost(5 + i, night), 16);
            else npc.setAnchor(layout.plazaRingSpot(i), 3);
        }
    }

    /** Named survivors + living anonymous candidates + the player. */
    static int survivors(DialogueSession session) {
        int alive = 0;
        PlayerMissions data = session.player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).resolve().orElse(null);
        if (data != null && data.isActive(FinalSelectionMission.ID)) {
            CompoundTag cands = data.getActiveState().getCompound(CANDS);
            for (String k : cands.getAllKeys()) if (!cands.getCompound(k).getBoolean("dead")) alive++;
        }
        return StoryCharacter.CANDIDATES.length + alive + 1;
    }

    static void onScopedDeath(MissionContext ctx, String key, LivingEntity victim, DamageSource source) {
        if (key.startsWith("cand")) {
            CompoundTag cands = ctx.state().getCompound(CANDS);
            CompoundTag c = cands.getCompound(key);
            c.putBoolean("dead", true);
            cands.put(key, c);
            ctx.state().put(CANDS, cands);
            ctx.forgetScoped(key);
            CompoundTag corner = quest(ctx, CORNER);
            if (status(corner, ACTIVE) && key.equals(corner.getString("who"))) {
                corner.putInt("status", FAILED);
            }
            CompoundTag sword = quest(ctx, SWORD);
            if (key.equals(sword.getString("who")) && !status(sword, DONE)) sword.putInt("status", FAILED);
        } else if ("horned".equals(key)) {
            ctx.forgetScoped(key);
            CompoundTag horn = quest(ctx, HORN);
            if (status(horn, ACTIVE) && victim instanceof CaptiveDemonEntity cd && cd.diedToSun()) {
                horn.putInt("status", FAILED);   // the sun took it, not you
            } else if (status(horn, ACTIVE)) {
                horn.putInt("status", DONE);
                give(ctx.player(), new ItemStack(ModItems.WISTERIA_INCENSE.get(), 2));
                mastery(ctx.player(), 40);
                StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
                if (tanjiro != null) tanjiro.say(t("quest.horned.tanjiro_thanks"));
            }
        }
        ctx.markDirty();
    }

    // ================================================================== quests: hooks

    static void onTrialBegin(MissionContext ctx, FujikasaneLayout layout) {
        // the first anonymous candidate still alive lost their sword running from the demons near the den
        CompoundTag cands = ctx.state().getCompound(CANDS);
        for (int i = 0; i < CANDIDATES; i++) {
            String key = "cand" + i;
            if (cands.contains(key) && !cands.getCompound(key).getBoolean("dead")) {
                CompoundTag sword = quest(ctx, SWORD);
                sword.putString("who", key);
                sword.putInt("status", OPEN);
                break;
            }
        }
    }

    static void onKill(MissionContext ctx, LivingEntity victim) {
        CompoundTag race = quest(ctx, RACE);
        if (status(race, ACTIVE)) {
            int n = race.getInt("kills") + 1;
            race.putInt("kills", n);
            StoryNpcEntity shouter = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.INOSUKE);
            if (n < RACE_GOAL && n % 3 == 0 && shouter != null) {
                shouter.say(t("quest.race.progress", n, Math.min(RACE_GOAL - 1, n + 1 + ctx.level().getRandom().nextInt(3))));
            }
            if (n >= RACE_GOAL) {
                race.putInt("status", DONE);
                ctx.data().setFlag("final_selection.beat_inosuke");
                mastery(ctx.player(), 60);
                give(ctx.player(), new ItemStack(ModItems.WISTERIA_INCENSE.get()));
                StoryNpcEntity inosuke = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.INOSUKE);
                if (inosuke != null) inosuke.say(t("quest.race.inosuke_lost"));
            }
        }
        CompoundTag zen = quest(ctx, ZEN);
        if (status(zen, ACTIVE)) {
            StoryNpcEntity zenitsu = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.ZENITSU);
            if (zenitsu != null && zenitsu.distanceToSqr(victim) < 32 * 32) {
                int n = zen.getInt("kills") + 1;
                zen.putInt("kills", n);
                if (n >= ZEN_GOAL) {
                    zen.putInt("status", DONE);
                    give(ctx.player(), new ItemStack(ModItems.ONIGIRI.get(), 3));
                    zenitsu.say(t("quest.zenitsu.thanks"));
                }
            }
        }
    }

    static void onDawn(MissionContext ctx) {
        CompoundTag race = quest(ctx, RACE);
        if (status(race, ACTIVE)) {
            race.putInt("status", FAILED);
            StoryNpcEntity inosuke = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.INOSUKE);
            if (inosuke != null) inosuke.say(t("quest.race.inosuke_won"));
        }
        CompoundTag corner = quest(ctx, CORNER);
        if (status(corner, ACTIVE)) {
            // the sun saved them before you did
            corner.putInt("status", DONE);
            clearCornerDemons(ctx);
        }
    }

    // ---------------------------------------------------------------- cornered

    private static void tickCornered(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        CompoundTag corner = quest(ctx, CORNER);
        int night = s.getInt(FinalSelectionMission.SURVIVED) + 1;
        long tod = ctx.level().getDayTime() % FinalSelectionMission.DAY;
        boolean inNight = s.getBoolean(FinalSelectionMission.IN_NIGHT);
        if (status(corner, ACTIVE)) {
            String who = corner.getString("who");
            if (!(ctx.scoped(who) instanceof StoryNpcEntity npc)) return;   // unloaded; check again later
            npc.setAnchor(npc.blockPosition(), 0);
            // a column of red distress smoke over them, visible from across the summit
            for (ServerPlayer viewer : ctx.level().players()) {
                if (viewer.distanceToSqr(npc) > 160 * 160) continue;
                ctx.level().sendParticles(viewer, com.wassuples.demonslayer.particle.ModParticles.DISTRESS_SMOKE.get(), true,
                        npc.getX(), npc.getY() + 2.2, npc.getZ(), 6, 0.25, 0.4, 0.25, 0.0);
            }
            if (ctx.player().tickCount % 200 < 20) npc.say(t("quest.corner.scream." + ctx.level().getRandom().nextInt(3)), 80);
            int left = 0;
            for (CaptiveDemonEntity d : ctx.level().getEntitiesOfClass(CaptiveDemonEntity.class, npc.getBoundingBox().inflate(64.0D), LivingEntity::isAlive)) {
                if (d.getPersistentData().getInt(CORNER_TAG) == ctx.runId()) left++;
            }
            corner.putInt("left", left);
            if (left == 0 && ctx.player().distanceToSqr(npc) < 32 * 32) {
                corner.putInt("status", DONE);
                corner.putInt("rescued", corner.getInt("rescued") + 1);
                npc.say(t("quest.corner.thanks"));
                give(ctx.player(), new ItemStack(ModItems.ONIGIRI.get(), 2));
                ctx.markDirty();
            }
            return;
        }
        // one candidate is cornered each night from the second to the sixth, once full dark has fallen
        if (!inNight || night < 2 || night > 6 || corner.getInt("night") >= night || tod < FinalSelectionMission.DUSK + 1500) return;
        corner.putInt("night", night);
        CompoundTag cands = s.getCompound(CANDS);
        for (int attempt = 0; attempt < CANDIDATES; attempt++) {
            String key = "cand" + ctx.level().getRandom().nextInt(CANDIDATES);
            if (!cands.contains(key) || cands.getCompound(key).getBoolean("dead")) continue;
            if (!(ctx.scoped(key) instanceof StoryNpcEntity npc)) continue;
            corner.putString("who", key);
            corner.putInt("status", ACTIVE);
            ServerLevel level = ctx.level();
            int count = 3 + night / 2;
            for (int i = 0; i < count; i++) {
                double ang = i * (Math.PI * 2 / count);
                BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        npc.blockPosition().offset(Math.cos(ang) * 5, 0, Math.sin(ang) * 5));
                CaptiveDemonEntity demon = ModEntityTypes.CAPTIVE_DEMON.get().create(level);
                if (demon == null) continue;
                demon.setVariant(CaptiveDemonEntity.randomSkin(level.getRandom()));
                demon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                demon.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
                demon.setTier(night);
                demon.getPersistentData().putInt(CORNER_TAG, ctx.runId());
                demon.setTarget(npc);
                level.addFreshEntity(demon);
            }
            npc.say(t("quest.corner.scream"), 160);
            // a scream that carries across the summit
            level.playSound(null, npc.blockPosition(), ModSounds.MISSION_FAILED.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 6.0F, 1.7F);
            ctx.markDirty();
            return;
        }
    }

    private static void clearCornerDemons(MissionContext ctx) {
        // at dawn they burn anyway; drop the persistence flag so leftovers can despawn
        for (CaptiveDemonEntity d : ctx.level().getEntitiesOfClass(CaptiveDemonEntity.class, ctx.player().getBoundingBox().inflate(160.0D),
                e -> e.getPersistentData().getInt(CORNER_TAG) == ctx.runId())) {
            d.getPersistentData().remove(CORNER_TAG);
        }
    }

    // ---------------------------------------------------------------- the horned demon

    private static void tickHorned(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag horn = quest(ctx, HORN);
        if (!status(horn, ACTIVE) || horn.getBoolean("spawned")) return;
        if (!ctx.state().getBoolean(FinalSelectionMission.IN_NIGHT)) return;
        BlockPos hut = layout.hut();
        if (ctx.player().blockPosition().distSqr(hut) > 40 * 40) return;
        ServerLevel level = ctx.level();
        CaptiveDemonEntity demon = ModEntityTypes.CAPTIVE_DEMON.get().create(level);
        if (demon == null) return;
        demon.setVariant(CaptiveDemonEntity.HORNED);
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hut);
        demon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        demon.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        demon.setTier(Math.min(7, FinalSelectionMission.activeNight(ctx.player()) + 2));
        demon.setCustomName(t("quest.horned.name"));
        ctx.spawnScoped(demon, "horned");
        demon.setTarget(ctx.player());
        horn.putBoolean("spawned", true);
        level.playSound(null, at, ModSounds.DEMON_GROWL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.6F);
        com.wassuples.demonslayer.networking.ModMessages.sendToTrackingAndSelf(new com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket(
                demon.getId(), t("quest.horned.roar"), 120), demon);
        ctx.markDirty();
    }

    // ================================================================== dialogue

    /**
     * Side-quest conversations, or null to fall back to the candidate's ordinary small talk.
     * {@code stage}: before / trial / after.
     */
    @Nullable
    static DialogueNode dialogue(DialogueSession session, StoryCharacter who, String stage) {
        ServerPlayer player = session.player;
        PlayerMissions data = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).resolve().orElse(null);
        boolean trial = "trial".equals(stage) && data != null && data.isActive(FinalSelectionMission.ID);
        CompoundTag state = trial ? data.getActiveState() : new CompoundTag();
        CompoundTag quests = state.getCompound(QUESTS);
        String base = "npc.demonslayer." + who.key() + ".";
        boolean night = state.getBoolean(FinalSelectionMission.IN_NIGHT);
        int nightNo = state.getInt(FinalSelectionMission.SURVIVED) + 1;

        if (who == StoryCharacter.CANDIDATE) return anonymous(session, stage, trial, quests);
        if (!trial) return null;

        DialogueNode first = DialogueNode.of(who, Component.translatable(base + "trial"));
        switch (who) {
            case INOSUKE -> {
                CompoundTag race = quests.getCompound(RACE);
                if (!race.contains("status") && night) {
                    first.primary(t("quest.race.ask"), x -> {
                        MissionManager.withActive(x.player, FinalSelectionMission.ID, c -> {
                            CompoundTag q = quest(c, RACE);
                            q.putInt("status", ACTIVE);
                            q.putInt("kills", 0);
                            c.markDirty();
                        });
                        return DialogueNode.of(StoryCharacter.INOSUKE, t("quest.race.offer")).end(t("quest.race.accept"));
                    });
                }
            }
            case ZENITSU -> {
                CompoundTag zen = quests.getCompound(ZEN);
                if (!zen.contains("status")) {
                    first.primary(t("quest.zenitsu.ask"), x -> DialogueNode.of(StoryCharacter.ZENITSU, t("quest.zenitsu.plea"))
                            .primary(t("quest.zenitsu.accept"), y -> {
                                MissionManager.withActive(y.player, FinalSelectionMission.ID, c -> {
                                    CompoundTag q = quest(c, ZEN);
                                    q.putInt("status", ACTIVE);
                                    q.putInt("kills", 0);
                                    c.markDirty();
                                });
                                return DialogueNode.of(StoryCharacter.ZENITSU, t("quest.zenitsu.relief")).end(Component.translatable("npc.demonslayer.ask.bye"));
                            })
                            .end(t("quest.zenitsu.decline")));
                }
            }
            case TANJIRO -> {
                CompoundTag horn = quests.getCompound(HORN);
                if (!horn.contains("status") && nightNo >= 3) {
                    first.primary(t("quest.horned.ask"), x -> DialogueNode.of(StoryCharacter.TANJIRO, t("quest.horned.offer"))
                            .primary(t("quest.horned.accept"), y -> {
                                MissionManager.withActive(y.player, FinalSelectionMission.ID, c -> {
                                    quest(c, HORN).putInt("status", ACTIVE);
                                    c.markDirty();
                                });
                                return null;
                            })
                            .end(t("quest.horned.decline")));
                }
            }
            case KANAO -> {
                long day = player.getLevel().getDayTime() / FinalSelectionMission.DAY;
                if (state.getLong(COIN) != day + 1) {
                    first.option(t("quest.coin.ask"), x -> {
                        boolean heads = x.player.getRandom().nextBoolean();
                        MissionManager.withActive(x.player, FinalSelectionMission.ID, c -> {
                            c.state().putLong(COIN, day + 1);
                            if (heads) give(c.player(), new ItemStack(ModItems.ONIGIRI.get()));
                            c.markDirty();
                        });
                        return DialogueNode.of(StoryCharacter.KANAO, t(heads ? "quest.coin.heads" : "quest.coin.tails")).end(Component.translatable("npc.demonslayer.ask.bye"));
                    });
                }
            }
            default -> {
            }
        }
        return first.end(Component.translatable("npc.demonslayer.ask.bye"));
    }

    /** An anonymous candidate: nerves before, the lost-sword errand or a plea during, thanks after. */
    private static DialogueNode anonymous(DialogueSession session, String stage, boolean trial, CompoundTag quests) {
        StoryNpcEntity npc = session.npc;
        int v = npc.variant();
        if (!trial) {
            return DialogueNode.of(null, Component.translatable("npc.demonslayer.candidate." + stage + "." + (v % 3)))
                    .end(Component.translatable("npc.demonslayer.ask.bye"));
        }
        CompoundTag sword = quests.getCompound(SWORD);
        String key = scopedKey(npc);
        if (key != null && key.equals(sword.getString("who"))) {
            if (status(sword, OPEN)) {
                return DialogueNode.of(null, t("quest.sword.plea"))
                        .primary(t("quest.sword.accept"), x -> {
                            MissionManager.withActive(x.player, FinalSelectionMission.ID, c -> {
                                CompoundTag q = quest(c, SWORD);
                                q.putInt("status", ACTIVE);
                                dropSword(c);
                                c.markDirty();
                            });
                            return DialogueNode.of(null, t("quest.sword.relief")).end(Component.translatable("npc.demonslayer.ask.bye"));
                        })
                        .end(t("quest.sword.decline"));
            }
            if (status(sword, ACTIVE)) {
                DialogueNode node = DialogueNode.of(null, t("quest.sword.waiting"));
                if (findSword(session.player) >= 0) {
                    node.primary(t("quest.sword.return"), x -> {
                        MissionManager.withActive(x.player, FinalSelectionMission.ID, c -> {
                            int slot = findSword(c.player());
                            if (slot < 0) return;
                            c.player().getInventory().removeItem(slot, 1);
                            quest(c, SWORD).putInt("status", DONE);
                            give(c.player(), new ItemStack(ModItems.ONIGIRI.get(), 2));
                            give(c.player(), new ItemStack(ModItems.WISTERIA_INCENSE.get()));
                            c.markDirty();
                        });
                        return DialogueNode.of(null, t("quest.sword.thanks")).end(Component.translatable("npc.demonslayer.ask.bye"));
                    });
                }
                return node.end(Component.translatable("npc.demonslayer.ask.bye"));
            }
        }
        return DialogueNode.of(null, Component.translatable("npc.demonslayer.candidate.trial." + (v % 3))).end(Component.translatable("npc.demonslayer.ask.bye"));
    }

    @Nullable
    private static String scopedKey(StoryNpcEntity npc) {
        CompoundTag tag = npc.getPersistentData().getCompound(MissionContext.SCOPE_TAG);
        return tag.contains("key") ? tag.getString("key") : null;
    }

    private static void dropSword(MissionContext ctx) {
        FujikasaneLayout layout = FujikasaneSites.known(ctx.player().server);
        if (layout == null) return;
        BlockPos den = layout.den();
        ServerLevel level = ctx.level();
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, den);
        ItemStack sword = new ItemStack(ModItems.NICHIRIN_BLADE.get());
        sword.setHoverName(t("quest.sword.item"));
        sword.getOrCreateTag().putInt(SWORD_TAG, ctx.runId());
        ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, sword);
        item.setUnlimitedLifetime();
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
    }

    private static int findSword(ServerPlayer player) {
        int run = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> d.getActiveState().getInt("run")).orElse(-1);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.hasTag() && st.getTag().getInt(SWORD_TAG) == run && run != 0) return i;
        }
        return -1;
    }

    // ================================================================== rewards

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void mastery(ServerPlayer player, int amount) {
        BreathingStyles style = player.getCapability(PlayerStyleProvider.PLAYER_STYLE).map(st -> st.getCurrentStyles()).orElse(null);
        if (style == null || style == BreathingStyles.NONE) return;
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(p -> p.addStyleMastery(style, amount));
        com.wassuples.demonslayer.util.ModUtils.syncProgression(player);
    }
}
