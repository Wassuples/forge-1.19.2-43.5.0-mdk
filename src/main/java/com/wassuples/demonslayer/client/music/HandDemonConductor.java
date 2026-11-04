package com.wassuples.demonslayer.client.music;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket;
import com.wassuples.demonslayer.networking.packet.TakeHandC2SPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Plays the Hand Demon's theme from what the server reports (see {@link HandDemonMusicS2CPacket}).
 *
 * <p><b>The prelude</b>: on night 7 the intro (the children's taunt, slams coming closer) plays before the Hand Demon
 * appears; it appears as the intro lands, and the Crawl comes in on that downbeat.
 *
 * <p><b>The fight</b> is horizontal re-sequencing of seamless loops, one per health phase: the Crawl (13/8), Grudge,
 * Thirteen (with a 14th stop-hit when Tanjiro is in the fight) and Fear. A phase change waits for the end of the
 * current phrase, then a one-bar fill plays while the old loop fades under it, and the new loop comes in on the
 * downbeat (a drop-out fill into Fear; the rewind when it regrows back into an earlier phase). As the demon loses track
 * of its quarry the music fades with it; when it gives up, the music is gone (and comes back, at the phase its health
 * calls for, when it finds someone again).
 *
 * <p><b>The death</b>: the fight music is dragged to a halt (tape-stop) under the collapse, then the natural reprise,
 * the reach (held while the hand waits) and one of two endings, then silence.
 *
 * <p>Vanilla music never plays over it, and on nights 3-6 one-off hints of it come from the den's direction.
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HandDemonConductor {
    private HandDemonConductor() {}

    private static final float VOLUME = 0.9F;

    /**
     * A score piece: its sound, its length, and the phrase length it may be left at (in milliseconds). Loops are left
     * only at the end of a phrase; {@code lastPhraseMs} caps that (Thirteen's count is one indivisible phrase).
     */
    private record Piece(String name, int lengthMs, int phraseMs, int lastPhraseMs) {
        Piece(String name, int lengthMs, int phraseMs) {
            this(name, lengthMs, phraseMs, lengthMs);
        }
    }

    private static final Piece INTRO = new Piece("intro", 12800, 1600);
    private static final Piece CRAWL = new Piece("crawl", 20800, 5200);                 // two bars of 13/8
    private static final Piece GRUDGE = new Piece("grudge", 25600, 6400);               // four bars of 4/4
    private static final Piece THIRTEEN = new Piece("thirteen", 33800, 5200, 31200);
    private static final Piece FOURTEEN = new Piece("thirteen_fourteen", 34000, 5200, 31200);
    private static final Piece FEAR = new Piece("fear", 25600, 6400);
    private static final Piece REWIND = new Piece("rewind", 1600, 1600);
    private static final Piece FILL_UP = new Piece("fill_up", 1600, 1600);
    private static final Piece FILL_FEAR = new Piece("fill_fear", 1600, 1600);
    private static final int REACH_START_TICKS = 17;
    private static final int STALE_MS = 3500;

    // ---- what the server last said
    private static byte mode = HandDemonMusicS2CPacket.IDLE;
    private static int entityId = -1;
    private static int phase = 1;
    private static int rewindsSeen = -1;
    private static int rewindsPending;
    private static float intensity = 1.0F;
    private static boolean fourteen;
    private static int fightAge;
    private static long lastPacketMs;
    private static long lastTickMs;
    /** Hand Demons whose death has fully played out (late packets about them are ignored). */
    private static final Set<Integer> finished = new HashSet<>();

    // ---- the fight's playback
    private static MusicSegmentSound current;
    private static Piece currentPiece;
    private static long currentStart;
    private static Piece next;            // the fill (or rewind) waiting for the end of the phrase
    private static long switchAt = -1;
    private static MusicSegmentSound leaving;   // the loop fading out under a fill

    // ---- the death's playback
    private static int deathTicks = -1;
    private static int outcome;
    private static boolean repriseOn, reachOn, holdOn, endingOn;
    private static int endingEnds;
    private static MusicSegmentSound reachHold;

    // =================================================================== packets

    public static void receive(HandDemonMusicS2CPacket p) {
        if (p.mode == HandDemonMusicS2CPacket.HINT) {
            playHint(p.night, p);
            return;
        }
        if (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F) return;   // music is off
        if (p.mode == HandDemonMusicS2CPacket.PRELUDE) {
            if (mode == HandDemonMusicS2CPacket.IDLE || mode == HandDemonMusicS2CPacket.PRELUDE) lastPacketMs = Util.getMillis();
            if (mode == HandDemonMusicS2CPacket.IDLE && current == null && p.fightAge < 40) {
                Minecraft mc = Minecraft.getInstance();
                mc.getMusicManager().stopPlaying();
                mode = HandDemonMusicS2CPacket.PRELUDE;
                current = new MusicSegmentSound(ModSounds.HAND_DEMON_MUSIC.get(INTRO.name).get(), false, VOLUME);
                mc.getSoundManager().play(current);
                currentPiece = INTRO;
                currentStart = Util.getMillis();
            }
            return;
        }
        if (finished.contains(p.entityId)) return;
        if (p.entityId != entityId && entityId != -1 && (mode == HandDemonMusicS2CPacket.FIGHT || mode == HandDemonMusicS2CPacket.DEATH)
                && Util.getMillis() - lastPacketMs < STALE_MS) return;   // another Hand Demon: follow the one we're following
        lastPacketMs = Util.getMillis();
        if (p.entityId != entityId && p.mode != HandDemonMusicS2CPacket.IDLE) {
            if (mode == HandDemonMusicS2CPacket.DEATH && deathTicks >= 0 && p.mode == HandDemonMusicS2CPacket.FIGHT) return;
            entityId = p.entityId;
            rewindsSeen = -1;
        }
        switch (p.mode) {
            case HandDemonMusicS2CPacket.FIGHT -> {
                if (mode == HandDemonMusicS2CPacket.DEATH) return;   // a stale fight packet after the killing blow
                if (mode == HandDemonMusicS2CPacket.PRELUDE) rewindsSeen = p.rewinds;
                mode = p.mode;
                phase = Math.max(1, Math.min(4, p.phase));
                intensity = p.intensity;
                fourteen = p.fourteen;
                fightAge = p.fightAge;
                if (rewindsSeen < 0) rewindsSeen = p.rewinds;
                else if (p.rewinds > rewindsSeen) {
                    rewindsPending += p.rewinds - rewindsSeen;
                    rewindsSeen = p.rewinds;
                }
            }
            case HandDemonMusicS2CPacket.IDLE -> {
                if (p.entityId == entityId && mode == HandDemonMusicS2CPacket.FIGHT) mode = HandDemonMusicS2CPacket.IDLE;
            }
            case HandDemonMusicS2CPacket.DEATH -> {
                if (mode != HandDemonMusicS2CPacket.DEATH || deathTicks < 0) beginDeath(p.deathTime);
                mode = HandDemonMusicS2CPacket.DEATH;
                if (Math.abs(p.deathTime - deathTicks) > 6) deathTicks = p.deathTime;
                outcome = p.outcome;
            }
            default -> {
            }
        }
    }

    /** True while any of the theme is sounding (vanilla music stays out of the way). */
    public static boolean active() {
        return current != null || mode == HandDemonMusicS2CPacket.DEATH || reachHold != null;
    }

    // =================================================================== ticking

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        long now = Util.getMillis();
        if (mc.isPaused()) {   // the sound is paused too: hold the clocks still
            long d = lastTickMs > 0 ? now - lastTickMs : 0;
            currentStart += d;
            lastPacketMs += d;
            if (switchAt >= 0) switchAt += d;
            lastTickMs = now;
            return;
        }
        lastTickMs = now;
        if (mc.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F) {   // music is off: play nothing at all
            if (mode != HandDemonMusicS2CPacket.IDLE || current != null || reachHold != null || leaving != null) reset();
            return;
        }
        boolean stale = now - lastPacketMs > STALE_MS;
        if (mode == HandDemonMusicS2CPacket.DEATH) {
            if (stale && deathTicks < HandDemonEntity.REPRISE_AT) {   // walked off the mountain before it began
                reset();
                return;
            }
            tickDeath(mc);
            return;
        }
        if (mode == HandDemonMusicS2CPacket.PRELUDE && !stale) {
            return;   // the intro plays on; the Hand Demon appears as it lands
        }
        if (mode == HandDemonMusicS2CPacket.FIGHT && !stale) {
            tickFight(mc, now);
        } else if (current != null) {
            // it lost everyone (or we left the mountain): the music is gone
            if (!current.fading()) current.fadeOut(40);
            if (!mc.getSoundManager().isActive(current)) {
                current = null;
                currentPiece = null;
                next = null;
                switchAt = -1;
                mode = HandDemonMusicS2CPacket.IDLE;
            }
        } else if (mode == HandDemonMusicS2CPacket.PRELUDE) {
            mode = HandDemonMusicS2CPacket.IDLE;   // the prelude ran out with nothing to follow it
        }
    }

    private static void tickFight(Minecraft mc, long now) {
        Piece wanted = pieceFor(phase);
        long pos = currentPiece == null ? 0 : now - currentStart;
        boolean oneShot = currentPiece == INTRO || currentPiece == REWIND || currentPiece == FILL_UP || currentPiece == FILL_FEAR;

        if (current == null || (!oneShot && !mc.getSoundManager().isActive(current))) {
            // no prelude (or we joined late, or it found us again): straight in
            mc.getMusicManager().stopPlaying();
            startLoop(mc, wanted, now);
            return;
        }
        if (oneShot) {
            // the intro, a fill or the rewind leads straight into the phase that is wanted now, on its downbeat
            if (pos >= currentPiece.lengthMs - 25) startLoop(mc, wanted, now);
        } else {
            if (switchAt < 0) {
                if (rewindsPending > 0) {
                    next = REWIND;
                    switchAt = endOfPhrase(now);
                } else if (wanted != currentPiece && !(wanted == THIRTEEN && currentPiece == FOURTEEN)
                        && !(wanted == FOURTEEN && currentPiece == THIRTEEN)) {
                    next = wanted == FEAR ? FILL_FEAR : FILL_UP;
                    switchAt = endOfPhrase(now);
                }
            }
            if (switchAt >= 0 && now >= switchAt - 25) {
                Piece fill = rewindsPending > 0 ? REWIND : wanted == currentPiece ? null : wanted == FEAR ? FILL_FEAR : FILL_UP;
                rewindsPending = 0;
                if (fill != null) startFill(mc, fill, now);
                else { next = null; switchAt = -1; }   // it healed back before the phrase ended: stay
            }
        }
        if (current != null && !current.fading() && !oneShot) {
            current.fadeTo(VOLUME * Math.max(0.0F, Math.min(1.0F, intensity)), 10);
        }
    }

    /** The end of the phrase now playing in the current loop (where it may be left). */
    private static long endOfPhrase(long now) {
        long pos = (now - currentStart) % currentPiece.lengthMs;
        long loopStart = now - pos;
        long b = ((pos / currentPiece.phraseMs) + 1) * currentPiece.phraseMs;
        if (b > currentPiece.lastPhraseMs) b = currentPiece.lengthMs;
        if (b - pos < 300) {   // too close to land cleanly: take the following phrase
            b += currentPiece.phraseMs;
            if (b > currentPiece.lastPhraseMs) b = currentPiece.lengthMs + (b > currentPiece.lengthMs ? currentPiece.phraseMs : 0);
        }
        return loopStart + b;
    }

    /** A fill (or the rewind) on top of the old loop, which fades out underneath it. */
    private static void startFill(Minecraft mc, Piece fill, long now) {
        if (leaving != null) leaving.fadeOut(2);
        leaving = current;
        if (leaving != null) leaving.fadeOut(30);
        current = new MusicSegmentSound(ModSounds.HAND_DEMON_MUSIC.get(fill.name).get(), false, VOLUME * Math.max(0.0F, Math.min(1.0F, intensity)));
        mc.getSoundManager().play(current);
        currentPiece = fill;
        currentStart = now;
        next = null;
        switchAt = -1;
    }

    private static void startLoop(Minecraft mc, Piece piece, long now) {
        if (current != null) current.fadeOut(3);
        current = new MusicSegmentSound(ModSounds.HAND_DEMON_MUSIC.get(piece.name).get(), true, VOLUME * Math.max(0.0F, Math.min(1.0F, intensity)));
        mc.getSoundManager().play(current);
        currentPiece = piece;
        currentStart = now;
        next = null;
        switchAt = -1;
    }

    private static Piece pieceFor(int phase) {
        return switch (phase) {
            case 2 -> GRUDGE;
            case 3 -> fourteen ? FOURTEEN : THIRTEEN;
            case 4 -> FEAR;
            default -> CRAWL;
        };
    }

    // =================================================================== the death

    private static void beginDeath(int at) {
        Minecraft mc = Minecraft.getInstance();
        deathTicks = at;
        outcome = HandDemonEntity.OUTCOME_WAITING;
        rewindsPending = 0;
        next = null;
        switchAt = -1;
        repriseOn = reachOn = holdOn = endingOn = false;
        if (current != null) current.tapeStop(36);   // the demon half of the theme is dragged to a halt
        if (leaving != null) leaving.tapeStop(20);
        leaving = null;
        current = null;
        currentPiece = null;
        if (at < HandDemonEntity.REPRISE_AT) play(mc, "collapse", false);
    }

    private static void tickDeath(Minecraft mc) {
        deathTicks++;
        if (!repriseOn && deathTicks >= HandDemonEntity.REPRISE_AT && deathTicks < HandDemonEntity.REACH_AT) {
            repriseOn = true;
            play(mc, "reprise", false);
        }
        if (!reachOn && deathTicks >= HandDemonEntity.REACH_AT && outcome == HandDemonEntity.OUTCOME_WAITING) {
            reachOn = true;
            play(mc, "reach_start", false);
        }
        if (!holdOn && reachOn && deathTicks >= HandDemonEntity.REACH_AT + REACH_START_TICKS && outcome == HandDemonEntity.OUTCOME_WAITING) {
            holdOn = true;
            reachHold = play(mc, "reach_hold", true);
        }
        if (!endingOn && outcome != HandDemonEntity.OUTCOME_WAITING) {
            endingOn = true;
            if (reachHold != null) reachHold.fadeOut(6);
            reachHold = null;
            boolean held = outcome == HandDemonEntity.OUTCOME_HELD;
            play(mc, held ? "ending_held" : "ending_unheld", false);
            endingEnds = deathTicks + (held ? HandDemonEntity.END_HELD : HandDemonEntity.END_UNHELD);
        }
        if (endingOn && deathTicks > endingEnds + 40) {
            finished.add(entityId);
            mode = HandDemonMusicS2CPacket.IDLE;   // silence
            deathTicks = -1;
            entityId = -1;
        }
        if (Util.getMillis() - lastPacketMs > 15000 && !endingOn) reset();   // we wandered away mid-reprise
    }

    private static MusicSegmentSound play(Minecraft mc, String name, boolean loop) {
        MusicSegmentSound s = new MusicSegmentSound(ModSounds.HAND_DEMON_MUSIC.get(name).get(), loop, VOLUME);
        mc.getSoundManager().play(s);
        return s;
    }

    // =================================================================== nights 3-6

    private static void playHint(int night, HandDemonMusicS2CPacket p) {
        if (night < 3 || night > 6 || active()) return;
        float vol = 0.3F + 0.12F * (night - 3);   // a little nearer every night
        Minecraft.getInstance().getSoundManager().play(
                new DirectionalHintSound(ModSounds.HAND_DEMON_MUSIC.get("night" + night).get(), p.den, vol));
    }

    // =================================================================== the hand

    /** Right-click the dying demon (or its outstretched hand) to take its hand, when Tanjiro isn't there to. */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(6.0));
        for (HandDemonEntity hand : mc.level.getEntitiesOfClass(HandDemonEntity.class, player.getBoundingBox().inflate(10.0D))) {
            if (!hand.handOpen() || hand.tanjiroHolds()) continue;
            AABB box = hand.getBoundingBox().inflate(1.0D);
            boolean hit = box.contains(eye) || box.clip(eye, end).isPresent();
            if (!hit) {
                Entity t = mc.level.getEntity(hand.reachTarget());
                Vec3 dir = t != null ? t.position().subtract(hand.position()).multiply(1, 0, 1).normalize()
                        : Vec3.directionFromRotation(0, hand.yBodyRot);
                Vec3 tip = hand.position().add(dir.scale(3.0)).add(0, 1.5, 0);
                hit = new AABB(tip, tip).inflate(1.5D).clip(eye, end).isPresent();
            }
            if (hit) {
                ModMessages.sendToServer(new TakeHandC2SPacket(hand.getId()));
                event.setCanceled(true);
                event.setSwingHand(true);
                return;
            }
        }
    }

    // =================================================================== vanilla music stays out

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s != null && s.getSource() == SoundSource.MUSIC && "minecraft".equals(s.getLocation().getNamespace()) && active()) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        finished.clear();
    }

    private static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (current != null) mc.getSoundManager().stop(current);
        if (reachHold != null) mc.getSoundManager().stop(reachHold);
        if (leaving != null) mc.getSoundManager().stop(leaving);
        leaving = null;
        current = null;
        currentPiece = null;
        reachHold = null;
        next = null;
        switchAt = -1;
        mode = HandDemonMusicS2CPacket.IDLE;
        deathTicks = -1;
        entityId = -1;
        rewindsSeen = -1;
        rewindsPending = 0;
    }
}
