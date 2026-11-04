package com.wassuples.demonslayer.missions.story;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.crow.CrowManager;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.dialogue.Dialogue;
import com.wassuples.demonslayer.dialogue.DialogueManager;
import com.wassuples.demonslayer.dialogue.DialogueNode;
import com.wassuples.demonslayer.dialogue.DialogueSession;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.missions.Arc;
import com.wassuples.demonslayer.missions.MissionContext;
import com.wassuples.demonslayer.missions.MissionManager;
import com.wassuples.demonslayer.missions.MissionPerks;
import com.wassuples.demonslayer.missions.MissionTracker;
import com.wassuples.demonslayer.missions.PlayerMissions;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.missions.StoryMission;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.reputation.RepEventType;
import com.wassuples.demonslayer.reputation.ReputationService;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.ModUtils;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneResidents;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission 0 — Final Selection (Arc I), on this world's one Mt. Fujikasane ({@link FujikasaneLayout}).
 *
 * <ol>
 *   <li><b>Gather</b>: the selection is only offered by the Ubuyashiki twins, Kiriya and Kanata, at the shrine half way
 *       up the mountain (new civilians carry a letter of introduction that points the way). The other candidates —
 *       Tanjiro, Zenitsu, Inosuke, Kanao and Genya — wait there too. The selection begins at nightfall.</li>
 *   <li><b>Briefing</b>: at nightfall, with the player at the shrine, the twins explain the rules out loud: the
 *       wisteria that blooms from the foot of the mountain to its slope keeps the captured demons penned on the
 *       mountaintop; survive seven days up there.</li>
 *   <li><b>Trial</b>: up the north stairs, through the last torii, onto the summit. Only nights actually <i>lived</i>
 *       there count (beds are refused). Sheltering in the wisteria around the summit is allowed but costs you (see
 *       {@code reactToHiding} / {@code endNight}); leaving the mountain for 30 seconds, or dying, fails the selection.
 *       On Night 7 the Hand Demon emerges; Tanjiro runs over and offers to fight beside the player.</li>
 *   <li><b>Return</b>: at the eighth dawn the survivors gather at the shrine. The twins measure them for their
 *       uniforms, engrave the rank Mizunoto, have them choose the ore for their Nichirin sword (delivered about two
 *       weeks later) and assign their Kasugai crow. That conversation completes the mission.</li>
 * </ol>
 *
 * <p>Branches: {@code survived} and {@code hand_demon_slain} (the player landed the killing blow or dealt at least half
 * the damage). Rewards: rank Mizunoto; style mastery pulse; +20 kill record and the Unyielding passive for slaying the
 * Hand Demon; reputation via {@link RepEventType#FINAL_SELECTION_PASSED} / {@link RepEventType#FINAL_SELECTION_HAND_DEMON_SLAIN};
 * a Kasugai crow now, the Nichirin blade later.
 */
public final class FinalSelectionMission extends StoryMission {
    public static final ResourceLocation ID = new ResourceLocation(DemonSlayer.MOD_ID, "final_selection");
    public static final FinalSelectionMission INSTANCE = new FinalSelectionMission();

    static final int NIGHTS = 7;
    static final long DAY = 24000L, DUSK = 13000L, DAWN = 23000L;
    /** The briefing starts when the player is at the shrine between these times of day ("nightfall"). */
    static final long NIGHTFALL_FROM = 12000L, NIGHTFALL_TO = 17000L;
    /** Hand Demon emerges this long after dusk on Night 7 (full dark). */
    static final long HAND_DELAY = 1500L;
    static final int LEAVE_GRACE_TICKS = 600, FIRST_CLIMB_TICKS = 1800;
    static final int BRIEF_LINES = 6, BRIEF_INTERVAL = 110;
    /** The swordsmith arrives this many days after the ceremony. */
    public static final long BLADE_DAYS = 2;
    public static final String SMITH_AT = "final_selection.smith_at", CROW_INTRO = "final_selection.crow_intro";
    public static final String BLADE_DUE = "final_selection.blade_due", BLADE_SEEN = "final_selection.blade_seen";
    static final int ORES = 5;
    /** NBT key of a Nichirin blade's colour (RGB), set from the ore chosen at the ceremony. */
    public static final String BLADE_COLOR = "BladeColor";
    /** Blade colour per ore: black iron, ringing (white) steel, scarlet-veined, cold (blue) iron, sunlit (yellow). */
    public static final int[] ORE_COLORS = {0x2A2A32, 0xF2F4FA, 0xC8323C, 0x3C6CD8, 0xE6B830};

    static final String PHASE = "phase", SEEN = "seen", IN_NIGHT = "inNight", SURVIVED = "survived", KILLS = "kills",
            HAND = "hand", HAND_PLAYER_DMG = "handPlayerDmg", HAND_TOTAL_DMG = "handTotalDmg", HAND_BY_PLAYER = "handByPlayer",
            OFFER = "offer", OFFER_AT = "offerAt", OUTSIDE = "outside", ENTERED = "entered",
            BRIEF_STEP = "briefStep", BRIEF_NEXT = "briefNext", ORE = "ore", WELCOMED = "welcomed",
            BUDGET = "spawnBudget", LINGER_X = "lingerX", LINGER_Z = "lingerZ", LINGER_T = "lingerT", LINGERING = "lingering",
            HIDE_T = "hideT", HIDE_FRENZY = "hideFrenzy", HID_NIGHTS = "hidNights", NIGHT_HIDDEN = "nightHidden", NIGHT_TICKS = "nightTicks",
            HAND_HINT = "handHint", HAND_AT = "handAt",
            ORE_LAID = "oreLaid", ORE_CHOSEN = "oreChosen", PACK_NIGHT = "packNight", SCENE = "scene", SCENE_NEXT = "sceneNext";
    /**
     * Demons that may appear on the summit on one candidate's account in a night: 15 on the first, 33 on the seventh.
     * (The summit holds fewer at once, see FujikasaneDemons; this caps the night's total.)
     */
    static int nightBudget(int night) {
        return 12 + 3 * night;
    }
    /** Staying within this many blocks of one spot for {@link #LINGER_LIMIT} ticks at night draws a pack. */
    private static final int LINGER_RADIUS = 12, LINGER_LIMIT = 1200;
    private static final long MAX_NATURAL_STEP = 100L;
    static final String GATHER = "gather", BRIEFING = "briefing", TRIAL = "trial", RETURN = "return";
    private static final int HAND_NONE = 0, HAND_ALIVE = 1, HAND_DEAD = 2, HAND_COMING = 3;
    /** The theme's intro (the children's taunt, its slams coming closer) plays this long before the Hand Demon appears. */
    private static final int PRELUDE_TICKS = 256;
    private static final int OFFER_NONE = 0, OFFER_PENDING = 1, OFFER_ACCEPTED = 2, OFFER_DECLINED = 3;

    private static final int GOLD = 0xF0D080, WISTERIA = 0xB9A2EC, BLOOD = 0xD8263A, PURPLE = 0x9A7AE0;

    private FinalSelectionMission() {
        super("final_selection", 0, Arc.I);
    }

    // ================================================================== availability

    @Override
    public List<Component> briefing() {
        return List.of(
                Component.translatable(key("brief.travel")),
                Component.translatable(key("brief.survive")),
                Component.translatable(key("brief.hand")),
                Component.translatable(key("brief.fail")));
    }

    @Override
    public boolean isUnlocked(ServerPlayer player, PlayerMissions missions) {
        return true;
    }

    @Override
    public boolean requiresGiver() {
        return true;
    }

    @Override
    public Component giverHint() {
        return Component.translatable(key("refuse.giver"));
    }

    @Override
    public Component acceptBlocker(ServerPlayer player, PlayerMissions missions) {
        if (isDemon(player)) return Component.translatable(key("refuse.demon"));
        Rank rank = player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).map(p -> p.getRank()).orElse(Rank.CIVILIAN);
        if (rank.ordinal() >= Rank.MIZUNOTO.ordinal()) return Component.translatable(key("refuse.slayer"));
        if (FujikasaneSites.find(player.server) == null) return Component.translatable(key("refuse.no_mountain"));
        return null;
    }

    /** Beds would skip nights of the selection; fine before it starts and after it ends. */
    @Override
    public boolean allowsSleep(MissionContext ctx) {
        String phase = ctx.state().getString(PHASE);
        return !TRIAL.equals(phase) && !BRIEFING.equals(phase);
    }

    // ================================================================== lifecycle

    @Override
    public void onAccept(MissionContext ctx) {
        CompoundTag s = ctx.state();
        s.putString(PHASE, GATHER);
        ctx.banner(title(), Component.translatable(key("accept.sub")), WISTERIA, 90);
        ctx.sound(ModSounds.MISSION_BELL.get(), 0.9F, 1.0F);
        ctx.notice(Component.translatable(key("accept.wait")));
        // the twins hand every candidate a bedroll to rest on until nightfall
        if (!ctx.player().getInventory().contains(new ItemStack(ModItems.BEDROLL.get()))) {
            ItemStack roll = new ItemStack(ModItems.BEDROLL.get());
            if (!ctx.player().getInventory().add(roll)) ctx.player().drop(roll, false);
            ctx.notice(Component.translatable(key("accept.bedroll")));
        }
        FujikasaneLayout layout = FujikasaneSites.find(ctx.player().server);
        if (layout != null) FinalSelectionExtras.spawnCandidates(ctx, layout);
        ctx.markDirty();
    }

    @Override
    public void tick(MissionContext ctx) {
        ServerPlayer player = ctx.player();
        CompoundTag s = ctx.state();
        if (isDemon(player)) {
            ctx.fail(Component.translatable(key("failed.demon")));
            return;
        }
        FujikasaneLayout layout = FujikasaneSites.find(player.server);
        if (layout == null) return;
        String phase = s.getString(PHASE);
        switch (phase) {
            case GATHER -> tickGather(ctx, layout);
            case BRIEFING -> tickBriefing(ctx, layout);
            case TRIAL -> tickTrial(ctx, layout);
            case RETURN -> tickReturn(ctx, layout);
            default -> {
                // runs saved before the shrine existed (travel/trial on the old biome): start again at the shrine
                for (String k : new ArrayList<>(s.getAllKeys())) {
                    if (!"run".equals(k) && !"entities".equals(k) && !"cands".equals(k) && !"quests".equals(k)) s.remove(k);
                }
                s.putString(PHASE, GATHER);
                ctx.notice(Component.translatable(key("migrated")));
                ctx.markDirty();
            }
        }
        if (!ctx.hasEnded()) FinalSelectionExtras.tick(ctx, layout);
    }

    // ---------------------------------------------------------------- gather

    private void tickGather(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        long tod = ctx.level().getDayTime() % DAY;
        boolean nightfall = tod >= NIGHTFALL_FROM && tod <= NIGHTFALL_TO;
        if (nightfall && atShrine(layout, ctx.player())) {
            s.putString(PHASE, BRIEFING);
            s.putInt(BRIEF_STEP, 0);
            s.putLong(BRIEF_NEXT, ctx.level().getGameTime() + 40);
            ctx.banner(Component.translatable(key("briefing.title")), Component.translatable(key("briefing.sub")), WISTERIA, 80);
            ctx.sound(ModSounds.MISSION_BELL.get(), 1.0F, 0.8F);
            ctx.markDirty();
        }
    }

    // ---------------------------------------------------------------- briefing

    private void tickBriefing(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        ServerPlayer player = ctx.player();
        if (!nearShrine(layout, player, 28)) {
            if (player.tickCount % 100 < 20) ctx.warn(Component.translatable(key("briefing.come_back")));
            return;   // the twins wait for you
        }
        long now = ctx.level().getGameTime();
        if (now < s.getLong(BRIEF_NEXT)) return;
        int step = s.getInt(BRIEF_STEP);
        if (step >= BRIEF_LINES) {
            beginTrial(ctx, layout);
            return;
        }
        boolean kanata = step % 2 == 0;
        int read = twinSay(player.server, kanata, key("briefing." + (step + 1)));
        s.putInt(BRIEF_STEP, step + 1);
        s.putLong(BRIEF_NEXT, now + Math.max(BRIEF_INTERVAL, read + 15));   // the next line once this one has been read
        ctx.markDirty();
    }

    private void beginTrial(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        s.putString(PHASE, TRIAL);
        s.putLong(SEEN, ctx.level().getDayTime());
        // the briefing happens at nightfall, so the selection opens on its first night — unless the briefing was held
        // up until late in the night, in which case Night 1 is the next one
        long tod = ctx.level().getDayTime() % DAY;
        boolean counts = tod >= DUSK && tod <= NIGHTFALL_TO + 1000;
        s.putBoolean(IN_NIGHT, counts);
        s.putInt(SURVIVED, 0);
        s.putBoolean(ENTERED, false);
        s.putLong(OUTSIDE, ctx.level().getGameTime());
        ctx.banner(Component.translatable(key("begin.title")), Component.translatable(key("begin.sub")), BLOOD, 100);
        ctx.sound(ModSounds.NIGHT_DRUM.get(), 1.0F, 1.0F);
        ctx.notice(Component.translatable(key("begin.climb")));
        FinalSelectionExtras.onTrialBegin(ctx, layout);
        if (counts) {
            ctx.notice(Component.translatable(key("night.first"), NIGHTS));
            s.putInt(BUDGET, nightBudget(1));
        }
        ctx.markDirty();
    }

    // ---------------------------------------------------------------- trial

    private void tickTrial(MissionContext ctx, FujikasaneLayout layout) {
        ServerPlayer player = ctx.player();
        CompoundTag s = ctx.state();
        long gameTime = ctx.level().getGameTime();

        // ---- staying on the mountain (the summit, or sheltering in the wisteria above the shrine; not below it)
        boolean inSummit = inSummit(layout, player);
        boolean onMountain = onMountain(layout, player);
        if (inSummit && !s.getBoolean(ENTERED)) {
            s.putBoolean(ENTERED, true);
            ctx.notice(Component.translatable(key("entered")));
            ctx.sound(ModSounds.NIGHT_DRUM.get(), 0.6F, 1.3F);
            s.remove(OUTSIDE);
        }
        if (s.getBoolean(ENTERED) ? onMountain : inSummit) {
            if (s.contains(OUTSIDE)) ctx.notice(Component.translatable(key("leave.returned")));
            s.remove(OUTSIDE);
        } else {
            int grace = s.getBoolean(ENTERED) ? LEAVE_GRACE_TICKS : FIRST_CLIMB_TICKS;
            if (!s.contains(OUTSIDE)) {
                s.putLong(OUTSIDE, gameTime);
                ctx.warn(Component.translatable(key("leave.warn")));
            }
            if (gameTime - s.getLong(OUTSIDE) >= grace) {
                ctx.fail(Component.translatable(s.getBoolean(ENTERED) ? key("failed.fled") : key("failed.never_climbed")));
                return;
            }
        }

        // ---- the night clock
        long now = ctx.level().getDayTime();
        long prev = s.contains(SEEN) ? s.getLong(SEEN) : now;
        s.putLong(SEEN, now);
        long step = now - prev;
        long tod = now % DAY, prevTod = prev % DAY;
        if (step > 0) {
            if (crossed(prevTod, tod, DUSK) && !s.getBoolean(IN_NIGHT)) startNight(ctx);
            if (crossed(prevTod, tod, DAWN) && s.getBoolean(IN_NIGHT) && endNight(ctx)) return;
        }
        boolean inNight = s.getBoolean(IN_NIGHT);
        int night = s.getInt(SURVIVED) + 1;

        // ---- keep moving: lingering in one spot draws the mountain's demons to you
        if (inNight && inSummit) {
            BlockPos at = player.blockPosition();
            int lx = s.getInt(LINGER_X), lz = s.getInt(LINGER_Z);
            if (!s.contains(LINGER_X) || (at.getX() - lx) * (at.getX() - lx) + (at.getZ() - lz) * (at.getZ() - lz) > LINGER_RADIUS * LINGER_RADIUS) {
                s.putInt(LINGER_X, at.getX());
                s.putInt(LINGER_Z, at.getZ());
                s.putInt(LINGER_T, 0);
                if (s.getBoolean(LINGERING)) s.putBoolean(LINGERING, false);
            } else {
                int t = s.getInt(LINGER_T) + 20;
                s.putInt(LINGER_T, t);
                if (t >= LINGER_LIMIT && !s.getBoolean(LINGERING)) s.putBoolean(LINGERING, true);
            }
        }

        // ---- sheltering in the wisteria is allowed (the demons can't follow). It has a price: a night spent mostly
        // hiding is noticed (the Corps hears of it, the twins remark on it, and the breathing training it would have
        // given is lost), and on the last night the Hand Demon finds someone else to fight.
        if (inNight) {
            s.putInt(NIGHT_TICKS, s.getInt(NIGHT_TICKS) + 20);
            if (inWisteria(layout, player)) {
                // by Zenitsu's hiding place (his errand takes you there) it counts half
                int t = s.getInt(NIGHT_HIDDEN) + (player.blockPosition().distSqr(layout.zenitsuHideout()) < 8 * 8 ? 10 : 20);
                s.putInt(NIGHT_HIDDEN, t);
                if (t >= 200 && !s.getBoolean(HIDE_FRENZY)) {   // a few seconds in, once a night: someone notices
                    s.putBoolean(HIDE_FRENZY, true);
                    reactToHiding(ctx, layout);
                }
            }
        }

        // ---- night 6: the starving ones band together into a pack
        if (inNight && night == 6 && inSummit && tod >= DUSK + 2500 && s.getInt(PACK_NIGHT) != night) {
            s.putInt(PACK_NIGHT, night);
            com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneDemons.sendPack(ctx.level(), player, 4);
            StoryNpcEntity tanjiro = FujikasaneResidents.loaded(player.server, StoryCharacter.TANJIRO);
            if (tanjiro != null && tanjiro.distanceToSqr(player) < 40 * 40) tanjiro.say(Component.translatable(key("tanjiro.pack")), 100);
            ctx.markDirty();
        }

        // ---- nights 3-6: something on the mountain is waiting. Once a night, its theme drifts up from the den.
        if (inNight && night >= 3 && night < NIGHTS && inSummit && tod >= DUSK + 3000 + 400L * night && s.getInt(HAND_HINT) != night) {
            s.putInt(HAND_HINT, night);
            com.wassuples.demonslayer.networking.ModMessages.sendToPlayer(
                    com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket.hint(night, layout.den()), player);
            ctx.markDirty();
        }

        // ---- the Hand Demon emerges in full dark on Night 7
        boolean hidingTonight = s.getInt(NIGHT_TICKS) > 0 && s.getInt(NIGHT_HIDDEN) * 2 > s.getInt(NIGHT_TICKS);
        if (inNight && night >= NIGHTS && s.getInt(HAND) == HAND_NONE && tod >= DUSK + HAND_DELAY && inSummit && !hidingTonight) {
            // first its theme: the mountain hears the children's taunt and something huge coming closer
            s.putInt(HAND, HAND_COMING);
            s.putLong(HAND_AT, gameTime);
            com.wassuples.demonslayer.sound.HandDemonMusic.broadcast(ctx.level(), player.blockPosition(),
                    com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket.prelude(0));
            ctx.markDirty();
        }
        if (s.getInt(HAND) == HAND_COMING) {
            long age = gameTime - s.getLong(HAND_AT);
            com.wassuples.demonslayer.sound.HandDemonMusic.broadcast(ctx.level(), player.blockPosition(),
                    com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket.prelude((int) Math.min(age, 100000)));
            // it appears as the intro lands (this ticks once a second, so up to a second early; the music waits for the bar)
            if (age >= PRELUDE_TICKS - 20) spawnHandDemon(ctx, layout);
        }
        // ...unless you have spent the night in the wisteria: then Tanjiro meets it alone, and cuts its neck
        if (inNight && night >= NIGHTS && s.getInt(HAND) == HAND_NONE && tod >= DUSK + HAND_DELAY + 2400 && hidingTonight) {
            s.putInt(HAND, HAND_DEAD);
            s.putBoolean(HAND_BY_PLAYER, false);
            ctx.notice(Component.translatable(key("hand.tanjiro_alone")));
            ctx.level().playSound(null, layout.den(), ModSounds.DEMON_GROWL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.4F);
            ctx.markDirty();
        }
        if (s.getInt(OFFER) == OFFER_PENDING && gameTime - s.getLong(OFFER_AT) > 600) answerOffer(ctx, false);

        // ---- something enormous stirs under the den (nights 3 to 6)
        if (inNight && night >= 3 && night < NIGHTS && player.blockPosition().distSqr(layout.den()) < 30 * 30
                && player.getRandom().nextInt(45) == 0) {
            BlockPos den = layout.den();
            ctx.level().playSound(null, den, ModSounds.DEMON_GROWL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.45F);
            ctx.level().sendParticles(ModParticles.DEMON_ASH.get(), den.getX() + 0.5, den.getY() + 1, den.getZ() + 0.5, 40, 3.0, 0.4, 3.0, 0.05);
            StoryNpcEntity tanjiro = FujikasaneResidents.loaded(player.server, StoryCharacter.TANJIRO);
            if (tanjiro != null && tanjiro.distanceToSqr(player) < 32 * 32) tanjiro.say(Component.translatable(key("tanjiro.den_smell")), 100);
        }

        // the demons themselves come from FujikasaneDemons, more each night
    }

    private void startNight(MissionContext ctx) {
        CompoundTag s = ctx.state();
        s.putBoolean(IN_NIGHT, true);
        int night = s.getInt(SURVIVED) + 1;
        s.putInt(BUDGET, nightBudget(night));
        s.putInt(HIDE_T, 0);
        s.remove(HIDE_FRENZY);
        s.putInt(NIGHT_HIDDEN, 0);
        s.putInt(NIGHT_TICKS, 0);
        s.remove(LINGER_X);
        s.remove(LINGER_Z);
        s.putInt(LINGER_T, 0);
        s.putBoolean(LINGERING, false);
        boolean last = night >= NIGHTS;
        ctx.banner(Component.translatable(key("night.title"), night),
                Component.translatable(last ? key("night.sub.last") : key("night.sub"), night, NIGHTS), last ? BLOOD : PURPLE, 110);
        ctx.sound(ModSounds.NIGHT_DRUM.get(), 1.0F, last ? 0.8F : 1.0F);
        ctx.markDirty();
    }

    /** A counted night is survived. Returns true if the mission changed phase. */
    private boolean endNight(MissionContext ctx) {
        CompoundTag s = ctx.state();
        s.putBoolean(IN_NIGHT, false);
        if (s.getInt(NIGHT_TICKS) > 0 && s.getInt(NIGHT_HIDDEN) * 2 > s.getInt(NIGHT_TICKS)) {
            s.putInt(HID_NIGHTS, s.getInt(HID_NIGHTS) + 1);   // a night spent mostly in the wisteria
            ReputationService.applyAutoEvent(ctx.level(), ctx.player().getUUID(), RepEventType.PLAYER_FLED_FROM_DEMON);
        }
        int survived = s.getInt(SURVIVED) + 1;
        s.putInt(SURVIVED, survived);
        if (survived >= NIGHTS) {
            if (s.getInt(HAND) == HAND_ALIVE || s.getInt(HAND) == HAND_COMING) withdrawHandDemon(ctx, true);
            FinalSelectionExtras.onDawn(ctx);
            s.putString(PHASE, RETURN);
            s.remove(OUTSIDE);
            ctx.banner(Component.translatable(key("survived.title")), Component.translatable(key("survived.sub")), GOLD, 120);
            ctx.sound(ModSounds.MISSION_BELL.get(), 1.0F, 1.0F);
            ctx.markDirty();
            return true;
        }
        FinalSelectionExtras.onDawn(ctx);
        ctx.notice(Component.translatable(key("dawn"), survived + 1));
        ctx.sound(ModSounds.MISSION_BELL.get(), 0.5F, 1.4F);
        ctx.markDirty();
        return false;
    }

    // ---------------------------------------------------------------- return

    private void tickReturn(MissionContext ctx, FujikasaneLayout layout) {
        CompoundTag s = ctx.state();
        if (!s.getBoolean(WELCOMED) && atShrine(layout, ctx.player())) {
            s.putBoolean(WELCOMED, true);
            twinSay(ctx.player().server, true, key("return.welcome.1"));
            twinSay(ctx.player().server, false, key("return.welcome.2"));
            ctx.notice(Component.translatable(key("return.talk")));
            s.putInt(SCENE, 0);
            s.putLong(SCENE_NEXT, ctx.level().getGameTime() + 100);
            ctx.markDirty();
        }
        // Genya loses his patience with the twins, and Tanjiro won't stand for it
        if (s.getBoolean(WELCOMED) && !s.contains(SCENE_NEXT)) s.putInt(SCENE, RETURN_SCENE.length);   // saved before this scene existed
        if (s.getBoolean(WELCOMED) && s.getInt(SCENE) < RETURN_SCENE.length && ctx.level().getGameTime() >= s.getLong(SCENE_NEXT)
                && nearShrine(layout, ctx.player(), 24)) {
            int step = s.getInt(SCENE);
            StoryNpcEntity who = FujikasaneResidents.loaded(ctx.player().server, RETURN_SCENE[step]);
            if (who != null) {
                if (step == 0) who.teleportNear(net.minecraft.world.phys.Vec3.atBottomCenterOf(layout.twinPos(true)), 2);
                if (step == 2) who.teleportNear(net.minecraft.world.phys.Vec3.atBottomCenterOf(layout.twinPos(true)), 2);
                who.say(Component.translatable(key("return.scene." + (step + 1))), 90);
            }
            s.putInt(SCENE, step + 1);
            s.putLong(SCENE_NEXT, ctx.level().getGameTime() + 80);
        }
    }

    private static final StoryCharacter[] RETURN_SCENE = {StoryCharacter.GENYA, StoryCharacter.KANATA, StoryCharacter.TANJIRO,
            StoryCharacter.GENYA, StoryCharacter.TANJIRO, StoryCharacter.KIRIYA};

    /** Testing: gather → briefing → trial; trial: day → a counted night begins, night → it is survived; return → done. */
    @Override
    public void onDebugAdvance(MissionContext ctx) {
        CompoundTag s = ctx.state();
        FujikasaneLayout layout = FujikasaneSites.find(ctx.player().server);
        switch (s.getString(PHASE)) {
            case GATHER -> {
                s.putString(PHASE, BRIEFING);
                s.putInt(BRIEF_STEP, BRIEF_LINES);
                s.putLong(BRIEF_NEXT, 0);
            }
            case BRIEFING -> {
                if (layout != null) beginTrial(ctx, layout);
            }
            case TRIAL -> {
                s.putLong(SEEN, ctx.level().getDayTime());
                s.putBoolean(ENTERED, true);
                s.remove(OUTSIDE);
                if (s.getBoolean(IN_NIGHT)) endNight(ctx);
                else startNight(ctx);
            }
            case RETURN -> ctx.complete(s.getInt(HAND) == HAND_DEAD && s.getBoolean(HAND_BY_PLAYER) ? "hand_demon_slain" : "survived");
            default -> s.putString(PHASE, GATHER);
        }
        ctx.markDirty();
    }

    @Override
    public void onLogin(MissionContext ctx) {
        if (ctx.state().getInt(OFFER) == OFFER_PENDING) {
            ctx.state().putLong(OFFER_AT, ctx.level().getGameTime());
            offerHelp(ctx);
        }
    }

    /** Only the selection itself is deadly: dying before it starts or on the walk back costs nothing. */
    @Override
    public void onPlayerDeath(MissionContext ctx, DamageSource source) {
        if (TRIAL.equals(ctx.state().getString(PHASE))) ctx.fail(Component.translatable("mission.demonslayer.failed.death"));
    }

    @Override
    public void onKill(MissionContext ctx, LivingEntity victim, DamageSource source) {
        if (!TRIAL.equals(ctx.state().getString(PHASE))) return;
        if (victim instanceof com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity || victim instanceof HandDemonEntity) {
            ctx.state().putInt(KILLS, ctx.state().getInt(KILLS) + 1);
            FinalSelectionExtras.onKill(ctx, victim);
            ctx.markDirty();
        }
    }

    @Override
    public void onScopedHurt(MissionContext ctx, String key, LivingEntity victim, DamageSource source, float amount) {
        if (!"hand".equals(key)) return;
        CompoundTag s = ctx.state();
        s.putFloat(HAND_TOTAL_DMG, s.getFloat(HAND_TOTAL_DMG) + amount);
        if (source.getEntity() == ctx.player()) s.putFloat(HAND_PLAYER_DMG, s.getFloat(HAND_PLAYER_DMG) + amount);
    }

    @Override
    public void onScopedDeath(MissionContext ctx, String key, LivingEntity victim, DamageSource source) {
        CompoundTag s = ctx.state();
        if (!"hand".equals(key)) {
            FinalSelectionExtras.onScopedDeath(ctx, key, victim, source);
            return;
        }
        boolean byPlayer = source.getEntity() == ctx.player()
                || s.getFloat(HAND_PLAYER_DMG) >= 0.5F * Math.max(1.0F, s.getFloat(HAND_TOTAL_DMG));
        s.putInt(HAND, HAND_DEAD);
        s.putBoolean(HAND_BY_PLAYER, byPlayer);
        ctx.forgetScoped("hand");
        // the fifty it ate, Urokodaki's thirteen among them, are free: petals rise from where it fell, and a bell rings
        ctx.level().sendParticles(ModParticles.WISTERIA_PETAL.get(), victim.getX(), victim.getY() + 1.0, victim.getZ(), 90, 2.2, 0.6, 2.2, 0.0);
        ctx.level().sendParticles(ModParticles.RESOLVE_SPARK.get(), victim.getX(), victim.getY() + 2.0, victim.getZ(), 26, 1.6, 2.0, 1.6, 0.02);
        ctx.level().playSound(null, victim.blockPosition(), ModSounds.MISSION_BELL.get(), net.minecraft.sounds.SoundSource.AMBIENT, 1.2F, 0.7F);
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
        if (byPlayer) {
            ctx.banner(Component.translatable(key("hand.slain.title")), Component.translatable(key("hand.slain.sub")), GOLD, 100);
            ctx.sound(ModSounds.MISSION_BELL.get(), 1.0F, 1.2F);
            if (tanjiro != null) tanjiro.say(Component.translatable(key(s.getInt(OFFER) == OFFER_ACCEPTED ? "tanjiro.hand_slain_together" : "tanjiro.hand_slain")));
        } else {
            ctx.notice(Component.translatable(key("hand.slain_by_other")));
            if (tanjiro != null) tanjiro.say(Component.translatable(key("tanjiro.hand_rest")));
        }
        if (tanjiro != null) tanjiro.setFightsHandDemon(false);
        ctx.markDirty();
    }

    @Override
    public void onEnd(MissionContext ctx, boolean success) {
        Entity hand = ctx.scoped("hand");
        if (hand != null) {
            ctx.level().sendParticles(ModParticles.DEMON_ASH.get(), hand.getX(), hand.getY() + 1.5, hand.getZ(), 80, 1.0, 1.5, 1.0, 0.03);
        }
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
        if (tanjiro != null) tanjiro.setFightsHandDemon(false);
        // the bedroll belongs to the Corps: a candidate who fails or withdraws hands it back too
        if (!success) returnBedrolls(ctx.player());
        FinalSelectionExtras.clearMarkers(ctx.player());
    }

    // ================================================================== the Hand Demon

    private void spawnHandDemon(MissionContext ctx, FujikasaneLayout layout) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        CompoundTag s = ctx.state();
        HandDemonEntity hand = ModEntityTypes.HAND_DEMON.get().create(level);
        if (hand == null) return;
        BlockPos at = summitSpot(ctx, layout, 18, 28);
        if (at == null) at = summitSpot(ctx, layout, 8, 16);
        if (at == null) return;   // try again next second
        float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - at.getZ(), player.getX() - at.getX())) - 90F;
        hand.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0F);
        hand.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        ctx.spawnScoped(hand, "hand");
        hand.setTarget(player);
        s.putInt(HAND, HAND_ALIVE);
        ctx.banner(Component.translatable(key("hand.title")), Component.translatable(key("hand.sub")), BLOOD, 100);
        level.playSound(null, at, ModSounds.DEMON_GROWL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.8F);
        level.sendParticles(ModParticles.DEMON_ASH.get(), at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 60, 1.5, 0.5, 1.5, 0.02);
        com.wassuples.demonslayer.networking.ModMessages.sendToTrackingAndSelf(
            new com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket(hand.getId(), Component.translatable(key("hand.speech")), 160), hand);

        // Tanjiro hurries over and offers to fight alongside (not forced: the Hand Demon is not a Kizuki)
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(player.server, StoryCharacter.TANJIRO);
        if (tanjiro != null && tanjiro.distanceToSqr(player) > 12 * 12) tanjiro.comeTo(player);
        s.putInt(OFFER, OFFER_PENDING);
        s.putLong(OFFER_AT, ctx.level().getGameTime());
        offerHelp(ctx);
        ctx.markDirty();
    }

    private void offerHelp(MissionContext ctx) {
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
        if (tanjiro == null) return;
        tanjiro.say(Component.translatable(key("tanjiro.shout")), 80);
        DialogueManager.openRemote(ctx.player(), tanjiro, session -> DialogueNode.of(StoryCharacter.TANJIRO, Component.translatable(key("tanjiro.offer")))
                .primary(Component.translatable(key("tanjiro.offer.accept")), x -> {
                    MissionManager.withActive(x.player, ID, c -> answerOffer(c, true));
                    return null;
                })
                .option(Component.translatable(key("tanjiro.offer.decline")), x -> {
                    MissionManager.withActive(x.player, ID, c -> answerOffer(c, false));
                    return null;
                }));
    }

    private void answerOffer(MissionContext ctx, boolean accepted) {
        CompoundTag s = ctx.state();
        if (s.getInt(OFFER) != OFFER_PENDING) return;
        s.putInt(OFFER, accepted ? OFFER_ACCEPTED : OFFER_DECLINED);
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
        if (tanjiro != null) DialogueManager.closeIfTalkingTo(ctx.player(), tanjiro);   // the offer is settled (answered or timed out)
        if (tanjiro != null) {
            tanjiro.setFightsHandDemon(accepted);
            if (accepted && ctx.scoped("hand") instanceof HandDemonEntity hand) tanjiro.setTarget(hand);
            tanjiro.say(Component.translatable(key(accepted ? "tanjiro.accepted" : "tanjiro.declined")));
        }
        ctx.markDirty();
    }

    /** Night 7 cut short, or dawn: the Hand Demon sinks back into the earth (the encounter replays if the night does). */
    private void withdrawHandDemon(MissionContext ctx, boolean dawn) {
        CompoundTag s = ctx.state();
        Entity hand = ctx.scoped("hand");
        if (hand != null) {
            ctx.level().sendParticles(ModParticles.DEMON_ASH.get(), hand.getX(), hand.getY() + 1.5, hand.getZ(), 80, 1.0, 1.5, 1.0, 0.03);
        }
        ctx.discardScoped("hand");
        s.putInt(HAND, dawn ? HAND_DEAD : HAND_NONE);
        if (!dawn) {
            s.putInt(OFFER, OFFER_NONE);
            s.remove(HAND_PLAYER_DMG);
            s.remove(HAND_TOTAL_DMG);
        } else {
            s.putBoolean(HAND_BY_PLAYER, false);
            ctx.notice(Component.translatable(key("hand.dawn")));
        }
        StoryNpcEntity tanjiro = FujikasaneResidents.loaded(ctx.player().server, StoryCharacter.TANJIRO);
        if (tanjiro != null) {
            tanjiro.setFightsHandDemon(false);
            DialogueManager.closeIfTalkingTo(ctx.player(), tanjiro);   // a pending offer no longer applies
        }
    }

    // ================================================================== hunters

    @Nullable
    private BlockPos summitSpot(MissionContext ctx, FujikasaneLayout layout, int min, int max) {
        for (int i = 0; i < 6; i++) {
            BlockPos at = ctx.findSurfaceSpot(ctx.player().blockPosition(), min, max, 8);
            if (at != null && layout.inDemonZone(layout.dx(at), layout.dz(at))) return at;
        }
        return null;
    }

    // ================================================================== rewards

    @Override
    public void onComplete(MissionContext ctx, String outcome) {
        boolean slain = "hand_demon_slain".equals(outcome);
        ServerPlayer player = ctx.player();
        PlayerMissions data = ctx.data();
        CompoundTag s = ctx.state();
        int kills = s.getInt(KILLS);
        int minorPulse = Math.min(150, kills * 15);
        int hid = Math.min(NIGHTS, s.getInt(HID_NIGHTS));
        // the training of a night spent hiding is lost
        int pulse = (slain ? 300 + minorPulse : minorPulse) * (NIGHTS - hid) / NIGHTS;
        BreathingStyles style = player.getCapability(PlayerStyleProvider.PLAYER_STYLE).map(st -> st.getCurrentStyles()).orElse(null);

        boolean[] banked = {false};
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(prog -> {
            if (prog.getRank().ordinal() < Rank.MIZUNOTO.ordinal()) prog.setRank(Rank.MIZUNOTO);
            if (slain) prog.addKillRecordBonus(20);
            // breathing training goes to a style the player has learned; with none yet, it waits for the first one
            if (pulse > 0) {
                if (style != null && style != BreathingStyles.NONE && prog.isStyleUnlocked(style)) prog.addStyleMastery(style, pulse);
                else {
                    prog.addBankedMastery(pulse);
                    banked[0] = true;
                }
            }
            ModUtils.applyRankStatBonuses(player, prog.getRank());
        });
        ModUtils.syncProgression(player);

        data.setFlag("final_selection.passed");
        data.setFlag("final_selection.ore." + Math.max(0, Math.min(ORES - 1, s.getInt(ORE))));
        if (slain) {
            data.setFlag("final_selection.hand_demon_slain");
            data.addPerk(MissionPerks.UNYIELDING);
        }
        if (s.getInt(OFFER) == OFFER_ACCEPTED) data.setFlag("final_selection.fought_beside_tanjiro");
        if (hid >= 3) data.setFlag("final_selection.hid_in_wisteria");   // remembered (later missions may bring it up)

        ReputationService.applyAutoEvent(ctx.level(), player.getUUID(),
                slain ? RepEventType.FINAL_SELECTION_HAND_DEMON_SLAIN : RepEventType.FINAL_SELECTION_PASSED);
        com.wassuples.demonslayer.reputation.ReputationManager repManager = com.wassuples.demonslayer.reputation.ReputationManager.get(ctx.level());
        com.wassuples.demonslayer.reputation.PlayerRepRecord rep = repManager.getOrCreateRecord(player.getUUID());
        rep.setSlayerRank(com.wassuples.demonslayer.reputation.rank.SlayerRank.MIZUNOTO);
        rep.getKillRecord().resetForRankAdvance();
        if (slain) rep.getKillRecord().addBonusKills(20);
        repManager.setDirty();

        // the swordsmith brings the blade forged from the chosen ore in about two weeks
        long now = player.getLevel().getDayTime();
        data.setTimer(BLADE_DUE, now + BLADE_DAYS * DAY);
        data.setTimer(BLADE_SEEN, now);
        // the Kasugai crow is assigned on the spot, and has a few words for its new slayer
        CrowManager.ensureCrowFor(player);
        data.setTimer(CROW_INTRO, player.getLevel().getGameTime() + 60);
        returnBedrolls(player);

        ctx.banner(Component.translatable(key("passed.title")), Component.translatable(key("passed.sub"), Rank.MIZUNOTO.getDisplayName()), GOLD, 120);
        ctx.sound(ModSounds.MISSION_BELL.get(), 1.0F, 1.0F);
        if (pulse > 0) {
            ctx.notice(banked[0] ? Component.translatable(key("passed.rewards.banked"), pulse)
                    : Component.translatable(key("passed.rewards.mastery"), pulse, style.getDisplayName()));
        }
        if (slain) {
            ctx.notice(Component.translatable(key("passed.rewards.kills"), 20));
            ctx.notice(Component.translatable(key("passed.rewards.unyielding")));
        }
        ctx.notice(Component.translatable(key("passed.rewards.crow")));
        ctx.notice(Component.translatable(key("passed.rewards.blade"), BLADE_DAYS));
    }

    /** Called every second for every player (MissionManager): delivers the Nichirin blade once the swordsmith is due. */
    public static void tickBladeDelivery(ServerPlayer player, PlayerMissions data) {
        long game = player.getLevel().getGameTime();
        // the Kasugai crow's first words, a few seconds after it is assigned
        if (data.hasTimer(CROW_INTRO) && game >= data.getTimer(CROW_INTRO)) {
            data.removeTimer(CROW_INTRO);
            player.getCapability(com.wassuples.demonslayer.crow.PlayerCrowProvider.PLAYER_CROW).ifPresent(crow -> {
                if (crow.getCrowUuid() != null && player.getLevel().getEntity(crow.getCrowUuid()) instanceof net.minecraft.world.entity.LivingEntity bird) {
                    com.wassuples.demonslayer.networking.ModMessages.sendToTrackingAndSelf(new com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket(
                            bird.getId(), Component.translatable("mission.demonslayer.final_selection.crow.first_words"), 160), bird);
                }
            });
        }
        if (!data.hasTimer(BLADE_DUE)) return;
        long now = player.getLevel().getDayTime();
        long seen = data.getTimer(BLADE_SEEN);
        long due = data.getTimer(BLADE_DUE);
        if (now < seen) due -= (seen - now);   // time was set backwards: keep the same days remaining
        due = Math.min(due, now + BLADE_DAYS * DAY);   // a blade promised under the old two-week wait
        data.setTimer(BLADE_SEEN, now);
        data.setTimer(BLADE_DUE, due);
        if (now < due) return;
        // the swordsmith walks up with the blade; if he can't find the way within 30 s, it simply arrives
        if (!data.hasTimer(SMITH_AT)) {
            if (player.level.dimension() == Level.OVERWORLD && spawnSwordsmith(player)) data.setTimer(SMITH_AT, game);
            else deliverBlade(player, data);
        } else if (game - data.getTimer(SMITH_AT) > 600 || game < data.getTimer(SMITH_AT)) {
            deliverBlade(player, data);
        }
    }

    /** A swordsmith in a hyottoko mask comes to hand over the blade (see StoryNpcEntity's swordsmith behaviour). */
    private static boolean spawnSwordsmith(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        for (int i = 0; i < 16; i++) {
            double ang = player.getRandom().nextDouble() * Math.PI * 2;
            BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.blockPosition().offset(Math.cos(ang) * 11, 0, Math.sin(ang) * 11));
            if (Math.abs(p.getY() - player.getBlockY()) > 6 || !level.getFluidState(p.below()).isEmpty()) continue;
            StoryNpcEntity smith = ModEntityTypes.STORY_NPC.get().create(level);
            if (smith == null) return false;
            smith.setCharacter(StoryCharacter.SWORDSMITH);
            smith.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
            smith.setCompanion(player.getUUID());
            smith.getPersistentData().putUUID(StoryNpcEntity.SMITH_FOR, player.getUUID());
            level.addFreshEntity(smith);
            smith.say(Component.translatable("mission.demonslayer.final_selection.smith.arrive"), 100);
            return true;
        }
        return false;
    }

    /** Gives the blade if it is still owed; false when it was already delivered. */
    public static boolean deliverBlade(ServerPlayer player, PlayerMissions data) {
        if (!data.hasTimer(BLADE_DUE)) return false;
        data.removeTimer(BLADE_DUE);
        data.removeTimer(BLADE_SEEN);
        data.removeTimer(SMITH_AT);
        ItemStack blade = new ItemStack(ModItems.NICHIRIN_BLADE.get());
        // the ore the candidate chose at the ceremony decides the colour of the steel
        for (int i = 0; i < ORES; i++) {
            if (!data.hasFlag("final_selection.ore." + i)) continue;
            blade.getOrCreateTag().putInt(BLADE_COLOR, ORE_COLORS[i]);
            ListTag lore = new ListTag();
            lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.translatable("item.demonslayer.nichirin_blade.forged_from",
                    Component.translatable("item.demonslayer.selection_ore_" + i)).withStyle(net.minecraft.ChatFormatting.GRAY))));
            blade.getOrCreateTagElement("display").put("Lore", lore);
            break;
        }
        if (!player.getInventory().add(blade)) player.drop(blade, false);
        player.playNotifySound(ModSounds.MISSION_BELL.get(), net.minecraft.sounds.SoundSource.MASTER, 0.8F, 1.3F);
        return true;
    }

    /** Hands every bedroll back to the twins (it belongs to the Corps). */
    static void returnBedrolls(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.BEDROLL.get())) player.getInventory().setItem(i, ItemStack.EMPTY);
        }
    }

    // ================================================================== tracker

    @Override
    public MissionTracker tracker(MissionContext ctx) {
        CompoundTag s = ctx.state();
        ServerPlayer player = ctx.player();
        FujikasaneLayout layout = FujikasaneSites.known(player.server);
        List<MissionTracker.Line> lines = new ArrayList<>();
        Component footer = Component.empty();
        boolean urgent = false;
        long now = ctx.level().getDayTime();
        long tod = now % DAY;
        String phase = s.getString(PHASE);

        switch (phase) {
            case GATHER -> {
                boolean there = layout != null && atShrine(layout, player);
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.shrine")), there, false));
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.nightfall")), false, false));
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.survive.pending")), false, false));
                if (layout != null && !there) footer = toShrine(player, layout);
                else if (tod >= NIGHTFALL_FROM && tod <= NIGHTFALL_TO) footer = Component.translatable(key("footer.nightfall_now"));
                else footer = Component.translatable(key("footer.nightfall"), clock(untilTod(tod, NIGHTFALL_FROM)));
            }
            case BRIEFING -> {
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.shrine")), true, false));
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.listen")), false, false));
                footer = Component.translatable(key("footer.listen"));
            }
            case TRIAL -> {
                boolean inNight = s.getBoolean(IN_NIGHT);
                int survived = s.getInt(SURVIVED);
                int nights = inNight ? survived + 1 : survived;
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.summit")), s.getBoolean(ENTERED) && !s.contains(OUTSIDE), false));
                Component survive = inNight
                        ? Component.translatable(key("obj.survive"), Math.min(nights, NIGHTS), NIGHTS)
                        : Component.translatable(key("obj.survive.between"), survived, NIGHTS);
                lines.add(new MissionTracker.Line(survive, false, false));
                int hand = s.getInt(HAND);
                if (hand != HAND_NONE || nights >= NIGHTS) {
                    boolean slain = hand == HAND_DEAD && s.getBoolean(HAND_BY_PLAYER);
                    lines.add(new MissionTracker.Line(Component.translatable(hand == HAND_DEAD && !slain ? key("obj.hand.fallen") : key("obj.hand")), slain, true));
                }
                if (s.getInt(OFFER) == OFFER_ACCEPTED && hand == HAND_ALIVE) {
                    lines.add(new MissionTracker.Line(Component.translatable(key("obj.tanjiro_with_you")), false, true));
                }
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.kills"), s.getInt(KILLS)), false, true));
                if (s.contains(OUTSIDE)) {
                    int grace = s.getBoolean(ENTERED) ? LEAVE_GRACE_TICKS : FIRST_CLIMB_TICKS;
                    long left = grace - (ctx.level().getGameTime() - s.getLong(OUTSIDE));
                    footer = Component.translatable(s.getBoolean(ENTERED) ? key("footer.return") : key("footer.climb"), clock(Math.max(0, left)));
                    urgent = true;
                } else if (inNight) {
                    footer = Component.translatable(nights >= NIGHTS ? key("footer.final_dawn") : key("footer.dawn"), clock(DAWN - tod));
                } else if (isNight(now)) {
                    footer = Component.translatable(key("footer.uncounted"), clock(DAWN - tod));
                } else {
                    footer = Component.translatable(key("footer.dusk"), clock(untilTod(tod, DUSK)));
                }
            }
            case RETURN -> {
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.survive.done"), NIGHTS), true, false));
                lines.add(new MissionTracker.Line(Component.translatable(key("obj.return")), false, false));
                if (layout != null && !atShrine(layout, player)) footer = toShrine(player, layout);
                else footer = Component.translatable(key("footer.talk"));
            }
            default -> {
            }
        }
        return new MissionTracker(id(), arc().label(), title(), lines, footer, urgent);
    }

    private Component toShrine(ServerPlayer player, FujikasaneLayout layout) {
        if (player.level.dimension() != Level.OVERWORLD) return Component.translatable(key("footer.travel.dimension"));
        BlockPos shrine = layout.plazaCenter();
        return Component.translatable(key("footer.travel"), compass(player.blockPosition(), shrine), distance(player.blockPosition(), shrine));
    }

    // ================================================================== dialogue: the Ubuyashiki twins

    /** What the twins say to {@code player} right now. */
    public static Dialogue twinsDialogue() {
        return session -> INSTANCE.twins(session);
    }

    private DialogueNode twins(DialogueSession session) {
        ServerPlayer player = session.player;
        PlayerMissions data = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).resolve().orElse(null);
        if (data == null) return null;
        if (isDemon(player)) return DialogueNode.of(StoryCharacter.KIRIYA, t("twins.demon")).end(t("twins.leave"));

        if (data.isActive(ID)) {
            String phase = data.getActiveState().getString(PHASE);
            return switch (phase) {
                case GATHER -> DialogueNode.of(StoryCharacter.KANATA, t("twins.gather"))
                        .option(t("twins.ask.rules"), x -> rules(true))
                        .option(t("twins.ask.withdraw"), x -> withdrawConfirm())
                        .end(t("twins.ok"));
                case BRIEFING -> DialogueNode.of(StoryCharacter.KIRIYA, t("twins.briefing")).end(t("twins.ok"));
                case TRIAL -> DialogueNode.of(StoryCharacter.KANATA, t("twins.trial"))
                        .option(t("twins.ask.withdraw"), x -> withdrawConfirm())
                        .end(t("twins.go_back"));
                case RETURN -> ceremony(session);
                default -> DialogueNode.of(StoryCharacter.KANATA, t("twins.gather")).end(t("twins.ok"));
            };
        }
        if (data.isCompleted(ID)) {
            boolean waiting = data.hasTimer(BLADE_DUE);
            return DialogueNode.of(StoryCharacter.KANATA, t(waiting ? "twins.after.waiting" : "twins.after"))
                    .option(t("twins.ask.mountain"), x -> mountain(false))
                    .end(t("twins.farewell"));
        }
        if (data.hasActive()) return DialogueNode.of(StoryCharacter.KIRIYA, t("twins.busy")).end(t("twins.farewell"));
        Rank rank = player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).map(p -> p.getRank()).orElse(Rank.CIVILIAN);
        if (rank.ordinal() >= Rank.MIZUNOTO.ordinal()) return DialogueNode.of(StoryCharacter.KANATA, t("twins.after")).end(t("twins.farewell"));
        return welcome(data.getFailures(ID) > 0);
    }

    private DialogueNode welcome(boolean retry) {
        return DialogueNode.of(StoryCharacter.KIRIYA, t(retry ? "twins.welcome.retry" : "twins.welcome"))
                .primary(t("twins.ask.enter"), x -> enter(x))
                .option(t("twins.ask.what"), x -> rules(false))
                .option(t("twins.ask.mountain"), x -> mountain(true))
                .end(t("twins.not_yet"));
    }

    private DialogueNode rules(boolean active) {
        DialogueNode n2 = DialogueNode.of(StoryCharacter.KIRIYA, t("twins.rules.2"));
        if (active) n2.end(t("twins.ok"));
        else n2.option(t("twins.back"), x -> welcome(false));
        return DialogueNode.of(StoryCharacter.KANATA, t("twins.rules.1")).then(t("twins.continue"), n2);
    }

    private DialogueNode mountain(boolean offer) {
        DialogueNode n2 = DialogueNode.of(StoryCharacter.KANATA, t("twins.mountain.2"));
        if (offer) n2.option(t("twins.back"), x -> welcome(false));
        else n2.end(t("twins.farewell"));
        return DialogueNode.of(StoryCharacter.KIRIYA, t("twins.mountain.1")).then(t("twins.continue"), n2);
    }

    private DialogueNode enter(DialogueSession session) {
        Component refusal = MissionManager.tryAccept(session.player, ID, false, true);
        if (refusal != null) return DialogueNode.of(StoryCharacter.KANATA, refusal).end(t("twins.ok"));
        return DialogueNode.of(StoryCharacter.KANATA, t("twins.accepted")).end(t("twins.ok"));
    }

    private DialogueNode withdrawConfirm() {
        return DialogueNode.of(StoryCharacter.KIRIYA, t("twins.withdraw.confirm"))
                .option(t("twins.withdraw.yes"), x -> {
                    MissionManager.abandon(x.player, ID, false);
                    return DialogueNode.of(StoryCharacter.KANATA, t("twins.withdraw.done")).end(t("twins.farewell"));
                })
                .end(t("twins.withdraw.no"));
    }

    /**
     * The survivors' ceremony: bedroll back, uniform, rank, then the ore, chosen in the world (the twins lay five ores
     * out on lacquered trays in front of them; see {@link #layOres}), then blade and crow. Completing it completes the
     * mission. Talking to the twins again picks up where it stopped.
     */
    private DialogueNode ceremony(DialogueSession session) {
        CompoundTag s = session.player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(PlayerMissions::getActiveState).orElse(new CompoundTag());
        if (s.getBoolean(ORE_CHOSEN)) return afterOre();
        if (s.getBoolean(ORE_LAID)) {
            MissionManager.withActive(session.player, ID, this::layOres);   // the trays are put out again if they were cleared
            return DialogueNode.of(StoryCharacter.KIRIYA, t("twins.ceremony.ore.waiting")).end(t("twins.ok"));
        }
        DialogueNode ore = DialogueNode.of(StoryCharacter.KIRIYA, t("twins.ceremony.ore"))
                .primary(t("twins.ceremony.ore.look"), x -> {
                    MissionManager.withActive(x.player, ID, this::layOres);
                    return null;
                });
        DialogueNode rank = DialogueNode.of(StoryCharacter.KANATA, t("twins.ceremony.rank")).then(t("twins.continue"), ore);
        DialogueNode uniform = DialogueNode.of(StoryCharacter.KIRIYA, t("twins.ceremony.uniform")).then(t("twins.continue"), rank);
        // the bedroll belongs to the Corps: it goes back to headquarters with the twins
        DialogueNode bedroll = DialogueNode.of(StoryCharacter.KIRIYA, t("twins.ceremony.bedroll"))
                .primary(t("twins.ceremony.bedroll.give"), x -> {
                    returnBedrolls(x.player);
                    return uniform;
                });
        DialogueNode first = bedroll;
        int hid = s.getInt(HID_NIGHTS);
        if (hid > 0) first = DialogueNode.of(StoryCharacter.KANATA, Component.translatable(key("twins.ceremony.hid"), hid)).then(t("twins.continue"), bedroll);
        return DialogueNode.of(StoryCharacter.KANATA, Component.translatable("mission.demonslayer.final_selection.twins.ceremony.welcome",
                FinalSelectionExtras.survivors(session))).then(t("twins.continue"), first);
    }

    /** Blade and crow, after the ore is chosen. */
    private DialogueNode afterOre() {
        DialogueNode crow = DialogueNode.of(StoryCharacter.KIRIYA, t("twins.ceremony.crow"))
                .primary(t("twins.ceremony.thanks"), x -> {
                    MissionManager.withActive(x.player, ID, c -> {
                        CompoundTag st = c.state();
                        c.complete(st.getInt(HAND) == HAND_DEAD && st.getBoolean(HAND_BY_PLAYER) ? "hand_demon_slain" : "survived");
                    });
                    return DialogueNode.of(StoryCharacter.KANATA, t("twins.ceremony.farewell")).end(t("twins.farewell"));
                });
        return DialogueNode.of(StoryCharacter.KANATA, t("twins.ceremony.blade")).then(t("twins.continue"), crow);
    }

    /** Five ores on lacquered trays on the plaza, in a row in front of the twins, for this candidate to choose from. */
    private void layOres(MissionContext ctx) {
        FujikasaneLayout layout = FujikasaneSites.known(ctx.player().server);
        if (layout == null) return;
        ServerLevel level = ctx.level();
        CompoundTag s = ctx.state();
        s.putBoolean(ORE_LAID, true);
        ctx.markDirty();
        if (!offerings(ctx.player()).isEmpty()) return;
        BlockPos plaza = layout.plazaCenter();
        for (int i = 0; i < ORES; i++) {
            double x = layout.centerX + 0.5, z = plaza.getZ() + 0.5 + (i - 2) * 1.0;
            level.addFreshEntity(new com.wassuples.demonslayer.entity.custom.OreOfferingEntity(level, ctx.player(), i, x, plaza.getY(), z));
        }
        level.playSound(null, plaza, ModSounds.DIALOGUE_OPEN.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 0.8F);
        twinSay(ctx.player().server, false, key("twins.ceremony.ore.laid"));
    }

    private static List<com.wassuples.demonslayer.entity.custom.OreOfferingEntity> offerings(ServerPlayer player) {
        FujikasaneLayout layout = FujikasaneSites.known(player.server);
        if (layout == null) return List.of();
        return player.server.overworld().getEntitiesOfClass(com.wassuples.demonslayer.entity.custom.OreOfferingEntity.class,
                new net.minecraft.world.phys.AABB(layout.plazaCenter()).inflate(16.0D), o -> o.isFor(player));
    }

    /** Whether {@code player} is at the ore choice of the ceremony (the trays stay out while this holds). */
    public static boolean isChoosingOre(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> d.isActive(ID)
                && RETURN.equals(d.getActiveState().getString(PHASE)) && d.getActiveState().getBoolean(ORE_LAID)).orElse(false);
    }

    /** The candidate picked up an ore from its tray: that steel it is. The ceremony goes on. */
    public static void chooseOre(ServerPlayer player, int ore) {
        if (!isChoosingOre(player)) return;
        MissionManager.withActive(player, ID, c -> {
            CompoundTag s = c.state();
            s.putInt(ORE, Math.max(0, Math.min(ORES - 1, ore)));
            s.remove(ORE_LAID);
            s.putBoolean(ORE_CHOSEN, true);
            c.markDirty();
        });
        for (com.wassuples.demonslayer.entity.custom.OreOfferingEntity o : offerings(player)) o.discard();
        player.getLevel().playSound(null, player.blockPosition(), ModSounds.MISSION_BELL.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.7F, 1.3F);
        StoryNpcEntity kanata = FujikasaneResidents.loaded(player.server, StoryCharacter.KANATA);
        if (kanata != null) DialogueManager.openRemote(player, kanata, session -> INSTANCE.afterOre());
    }

    // ================================================================== dialogue: the other candidates

    /** Small talk with a fellow candidate; what they say depends on where the player is in the selection. */
    public static Dialogue candidateDialogue(StoryCharacter who) {
        return session -> INSTANCE.candidate(session, who);
    }

    private DialogueNode candidate(DialogueSession session, StoryCharacter who) {
        PlayerMissions data = session.player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).resolve().orElse(null);
        String stage = "before";
        if (data != null) {
            if (data.isActive(ID)) {
                String phase = data.getActiveState().getString(PHASE);
                stage = TRIAL.equals(phase) ? "trial" : RETURN.equals(phase) ? "after" : "before";
            } else if (data.isCompleted(ID)) {
                stage = "after";
            }
        }
        DialogueNode special = FinalSelectionExtras.dialogue(session, who, stage);
        if (special != null) return special;
        String base = "npc.demonslayer." + who.key() + "." + stage;
        DialogueNode first = DialogueNode.of(who, Component.translatable(base));
        if ("before".equals(stage)) {
            first.then(Component.translatable("npc.demonslayer.ask.why"), DialogueNode.of(who, Component.translatable(base + ".why")).end(t("twins.farewell")));
        }
        return first.end(Component.translatable("npc.demonslayer.ask.bye"));
    }

    // ================================================================== state for the residents

    /** Players whose selection is under way (Trial), for the shrine's residents. */
    public static List<ServerPlayer> trialPlayers(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(d -> {
                if (d.isActive(ID) && TRIAL.equals(d.getActiveState().getString(PHASE))) out.add(p);
            });
        }
        return out;
    }

    /** The night of {@code player}'s selection being lived now (1..7), 0 if their selection isn't under way. */
    public static int activeNight(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> {
            if (!d.isActive(ID) || !TRIAL.equals(d.getActiveState().getString(PHASE))) return 0;
            CompoundTag s = d.getActiveState();
            int survived = s.getInt(SURVIVED);
            return Math.min(NIGHTS, s.getBoolean(IN_NIGHT) ? survived + 1 : Math.max(1, survived));
        }).orElse(0);
    }

    /** Whether {@code player} has stayed in one spot too long tonight (the demons have their scent). */
    public static boolean isLingering(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> d.isActive(ID) && d.getActiveState().getBoolean(LINGERING)).orElse(false);
    }

    /** Takes one demon from tonight's budget for {@code player}; false when the night's demons are spent. */
    public static boolean takeSpawn(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> {
            if (!d.isActive(ID)) return false;
            CompoundTag s = d.getActiveState();
            if (!s.contains(BUDGET)) s.putInt(BUDGET, nightBudget(Math.max(1, s.getInt(SURVIVED) + 1)));
            int left = s.getInt(BUDGET);
            if (left <= 0) return false;
            s.putInt(BUDGET, left - 1);
            return true;
        }).orElse(false);
    }

    /** Whether {@code player}'s Hand Demon fight has Tanjiro at their side. */
    public static boolean tanjiroFightsWith(ServerPlayer player) {
        return player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).map(d -> d.isActive(ID)
                && d.getActiveState().getInt(OFFER) == OFFER_ACCEPTED && d.getActiveState().getInt(HAND) == HAND_ALIVE).orElse(false);
    }

    // ================================================================== helpers

    private static Component t(String suffix) {
        return Component.translatable("mission.demonslayer.final_selection." + suffix);
    }

    /** One twin says a line (in turn with the other); returns how long it stays up. */
    private static int twinSay(MinecraftServer server, boolean kanata, String translationKey) {
        StoryNpcEntity twin = FujikasaneResidents.loaded(server, kanata ? StoryCharacter.KANATA : StoryCharacter.KIRIYA);
        Component line = Component.translatable(translationKey);
        if (twin != null) twin.say(line);
        return StoryNpcEntity.readingTicks(line);
    }

    static boolean isDemon(ServerPlayer player) {
        return player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(d -> d.isDemon()).orElse(false);
    }

    static boolean atShrine(FujikasaneLayout layout, ServerPlayer player) {
        return nearShrine(layout, player, FujikasaneLayout.PLAZA_R + 4);
    }

    static boolean nearShrine(FujikasaneLayout layout, ServerPlayer player, int radius) {
        if (player.level.dimension() != Level.OVERWORLD) return false;
        BlockPos c = layout.plazaCenter();
        double dx = player.getX() - (c.getX() + 0.5), dz = player.getZ() - (c.getZ() + 0.5);
        return dx * dx + dz * dz <= (double) radius * radius && Math.abs(player.getY() - c.getY()) < 16;
    }

    /** On the wisteria-free mountaintop (a few blocks of slack at its edge). */
    static boolean inSummit(FujikasaneLayout layout, ServerPlayer player) {
        if (player.level.dimension() != Level.OVERWORLD) return false;
        int dx = layout.dx(player.blockPosition()), dz = layout.dz(player.blockPosition());
        return FujikasaneLayout.dist(dx, dz) < FujikasaneLayout.DEMON_R + 3 && !layout.inPlaza(dx, dz) && player.getY() > layout.baseY;
    }

    /**
     * Still taking the selection: on the summit or sheltering in the wisteria band around it, down to the level of the
     * shrine. Going down past that, or back to the shrine itself, is leaving.
     */
    static boolean onMountain(FujikasaneLayout layout, ServerPlayer player) {
        if (player.level.dimension() != Level.OVERWORLD || player.getY() <= layout.baseY) return false;
        if (inSummit(layout, player)) return true;
        int dx = layout.dx(player.blockPosition()), dz = layout.dz(player.blockPosition());
        if (layout.inPlaza(dx, dz)) return false;
        return FujikasaneLayout.dist(dx, dz) < FujikasaneLayout.PLAZA_DIST + FujikasaneLayout.PLAZA_R;
    }

    /** In the wisteria around the summit (where demons can't follow). */
    static boolean inWisteria(FujikasaneLayout layout, ServerPlayer player) {
        return onMountain(layout, player) && !inSummit(layout, player);
    }

    /** Someone sees you take shelter: Zenitsu is overjoyed to have company; anyone else, less so. */
    private void reactToHiding(MissionContext ctx, FujikasaneLayout layout) {
        ServerPlayer player = ctx.player();
        ctx.notice(Component.translatable(key("hide.notice")));
        StoryNpcEntity zenitsu = FujikasaneResidents.loaded(player.server, StoryCharacter.ZENITSU);
        if (zenitsu != null && zenitsu.distanceToSqr(player) < 24 * 24) {
            zenitsu.say(Component.translatable(key("hide.zenitsu")));
            return;
        }
        for (StoryCharacter c : new StoryCharacter[]{StoryCharacter.GENYA, StoryCharacter.KANAO, StoryCharacter.TANJIRO}) {
            StoryNpcEntity npc = FujikasaneResidents.loaded(player.server, c);
            if (npc != null && npc.distanceToSqr(player) < 28 * 28) {
                npc.say(Component.translatable(key("hide." + c.key())));
                return;
            }
        }
    }

    static boolean isNight(long dayTime) {
        long t = dayTime % DAY;
        return t >= DUSK && t < DAWN;
    }

    static long untilTod(long tod, long target) {
        return tod < target ? target - tod : target + DAY - tod;
    }

    /** Did time-of-day pass {@code boundary} going from {@code from} to {@code to} (with wrap-around at midnight)? */
    static boolean crossed(long from, long to, long boundary) {
        if (from == to) return false;
        return from < to ? (from < boundary && boundary <= to) : (from < boundary || boundary <= to);
    }

    static String clock(long ticks) {
        long sec = ticks / 20;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    public static Component compass(BlockPos from, BlockPos to) {
        double bearing = Math.toDegrees(Math.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())));
        int sector = (int) Math.floorMod(Math.round(bearing / 45.0), 8);
        String[] dirs = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("direction.demonslayer." + dirs[sector]);
    }

    public static String distance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return String.format("%,d", Math.round(Math.sqrt(dx * dx + dz * dz)));
    }
}
