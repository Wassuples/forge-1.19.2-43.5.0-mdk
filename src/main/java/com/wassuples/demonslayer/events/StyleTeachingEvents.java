package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.entity.custom.StyleMasterNpcEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayerProgressionSyncS2CPacket;
import com.wassuples.demonslayer.networking.packet.StartStyleMinigameS2CPacket;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.training.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all style-teaching interactions:
 *
 * <ol>
 *   <li><b>Player → NPC master</b>: right-click a {@link StyleMasterNpcEntity} or Hashira NPC
 *       → access check → start minigame session → send S2C packet.</li>
 *   <li><b>Player → Player teaching</b>: right-click another {@link ServerPlayer} while sneaking
 *       → teacher mastery check → start minigame for the student.</li>
 *   <li><b>Minigame result</b>: called by {@link com.wassuples.demonslayer.networking.packet.StyleMinigameResultC2SPacket}
 *       → apply unlock / mastery gain / failure consequence.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StyleTeachingEvents {

    // ─── NPC Master interaction ───────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerPlayer interactor = (ServerPlayer) event.getEntity();

        // ── Non-Hashira style master ──────────────────────────────────────────
        if (event.getTarget() instanceof StyleMasterNpcEntity masterNpc) {
            event.setCanceled(true);
            handleNpcMasterInteraction(interactor, masterNpc.getMasterId(), level);
            return;
        }

        // ── Hashira NPC (uses their breathing style as the master id lookup) ──
        if (event.getTarget() instanceof HashiraEntity hashira) {
            BreathingStyles style = hashira.getBreathingStyle();
            if (style == null || style == BreathingStyles.NONE) return;

            Optional<StyleMasterRecord> record = StyleMasterRegistry.forStyle(style)
                .stream()
                .filter(StyleMasterRecord::isHashira)
                .findFirst();

            if (record.isEmpty()) return;

            event.setCanceled(true);
            handleNpcMasterInteraction(interactor, record.get().masterId(), level);
            return;
        }

        // ── Player-to-player teaching ─────────────────────────────────────────
        if (event.getTarget() instanceof ServerPlayer student) {
            // Sneaking = teacher is attempting to teach
            if (interactor.isShiftKeyDown()) {
                event.setCanceled(true);
                handlePlayerToPlayerTeaching(student, interactor, level);
            }
        }
    }

    // ─── Core NPC interaction flow ────────────────────────────────────────────

    private static void handleNpcMasterInteraction(ServerPlayer student, String masterId,
                                                    ServerLevel level) {
        Optional<StyleMasterRecord> optRecord = StyleMasterRegistry.byId(masterId);
        if (optRecord.isEmpty()) {
            student.sendSystemMessage(Component.literal("[Debug] Unknown master id: " + masterId)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        StyleMasterRecord master = optRecord.get();

        // Check cooldown
        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);
        if (mgr.isOnCooldown(student.getUUID(), master.style(), level.getGameTime())) {
            long remaining = mgr.getCooldownEndTick(student.getUUID(), master.style())
                           - level.getGameTime();
            long seconds = remaining / 20;
            student.sendSystemMessage(
                    Component.literal("You need to rest. Come back in approximately "
                            + seconds + " seconds.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Access check
        StyleMasterAccess.Result accessResult = master.access().evaluate(student, level);
        if (!accessResult.allowed()) {
            student.sendSystemMessage(accessResult.denialMessage());
            return;
        }

        // Check if already has a session open (prevent double-trigger)
        if (mgr.hasActiveSession(student.getUUID())) return;

        // Determine whether this is an unlock or a training session
        PlayerProgression prog = student.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                .orElseThrow(() -> new IllegalStateException("PlayerProgression capability missing."));
        boolean alreadyUnlocked = prog.isStyleUnlocked(master.style());
        boolean isTraining = alreadyUnlocked;

        int tier = calculateTier(prog, master.style());
        int lives = StyleMinigameType.forStyle(master.style()).hasLives()
                ? mgr.getLivesRemaining(student.getUUID(), master.style())
                : 0;

        UUID sessionId = UUID.randomUUID();
        mgr.startSession(student.getUUID(), master.style(), isTraining, null);

        // Flavour dialogue
        sendMasterDialogue(student, master, isTraining);

        // Send the minigame to the client
        ModMessages.sendToPlayer(new StartStyleMinigameS2CPacket(sessionId, master.style(), tier, lives), student);
    }

    // ─── Player-to-player teaching ────────────────────────────────────────────

    private static void handlePlayerToPlayerTeaching(ServerPlayer teacher, ServerPlayer student,
                                                      ServerLevel level) {
        if (teacher.equals(student)) return;

        // Teacher must have mastered (100/100 on every form) all forms of at least one style
        PlayerProgression teacherProg = teacher.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElseThrow(() -> new IllegalStateException("PlayerProgression capability missing."));

        BreathingStyles masteredStyle = null;
        outer:
        for (BreathingStyles style : teacherProg.getUnlockedStyles()) {
            if (style == BreathingStyles.NONE) continue;
            List<BreathingForms> styleForms = Arrays.stream(BreathingForms.values())
                    .filter(f -> f.getStyle() == style).toList();
            if (styleForms.isEmpty()) continue;
            for (BreathingForms form : styleForms) {
                if (teacherProg.getFormMastery(form) < form.getMasteryGoal()) continue outer;
            }
            masteredStyle = style;
            break;
        }

        if (masteredStyle == null) {
            teacher.sendSystemMessage(Component.literal(
                    "You have not yet mastered all forms of any style completely.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);

        // Student cooldown check
        if (mgr.isOnCooldown(student.getUUID(), masteredStyle, level.getGameTime())) {
            teacher.sendSystemMessage(Component.literal(
                    student.getName().getString() + " is still resting from their last attempt.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (mgr.hasActiveSession(student.getUUID())) {
            teacher.sendSystemMessage(Component.literal(
                    student.getName().getString() + " is already in a training session.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        PlayerProgression studentProg = student.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElseThrow(() -> new IllegalStateException("PlayerProgression capability missing."));
        boolean isTraining = studentProg.isStyleUnlocked(masteredStyle);
        int tier = calculateTier(studentProg, masteredStyle);
        int lives = StyleMinigameType.forStyle(masteredStyle).hasLives()
                ? mgr.getLivesRemaining(student.getUUID(), masteredStyle) : 0;

        UUID sessionId = UUID.randomUUID();
        mgr.startSession(student.getUUID(), masteredStyle, isTraining, teacher.getUUID());

        teacher.sendSystemMessage(Component.literal(
                "You begin to teach " + student.getName().getString() + " the ways of "
                + masteredStyle.getDisplayName() + ".")
                .withStyle(ChatFormatting.GREEN));
        student.sendSystemMessage(Component.literal(
                teacher.getName().getString() + " begins teaching you " + masteredStyle.getDisplayName() + "!")
                .withStyle(ChatFormatting.GREEN));

        ModMessages.sendToPlayer(new StartStyleMinigameS2CPacket(sessionId, masteredStyle, tier, lives), student);
    }

    // ─── Minigame result handler (called from result packet) ──────────────────

    /**
     * Processes the result packet sent by the client when a minigame ends.
     * Called on the server main thread by {@link com.wassuples.demonslayer.networking.packet.StyleMinigameResultC2SPacket}.
     */
    public static void handleMinigameResult(ServerPlayer player, UUID sessionId, boolean passed) {
        if (!(player.level instanceof ServerLevel level)) return;

        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);
        StyleMinigameSession session = mgr.popSession(player.getUUID());

        if (session == null) {
            DemonSlayer.LOGGER.warn("[StyleTeaching] No active session for player {} – ignoring result", player.getName().getString());
            return;
        }

        BreathingStyles style = session.style();

        if (passed) {
            applyPassReward(player, level, session, mgr);
        } else {
            applyFailConsequence(player, level, style, mgr);
        }
    }

    // ─── Reward / consequence helpers ────────────────────────────────────────

    private static void applyPassReward(ServerPlayer player, ServerLevel level,
                                         StyleMinigameSession session,
                                         StyleMinigameServerManager mgr) {
        BreathingStyles style = session.style();
        PlayerProgression prog = player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElseThrow(() -> new IllegalStateException("PlayerProgression capability missing."));

        mgr.resetLives(player.getUUID(), style);

        if (!session.isTraining()) {
            // Initial unlock
            prog.unlockStyle(style);
            // Unlock the first form of the style automatically
            BreathingForms firstForm = Arrays.stream(BreathingForms.values())
                    .filter(f -> f.getStyle() == style)
                    .findFirst().orElse(null);
            if (firstForm != null) prog.unlockForm(firstForm);

            player.level.playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal(
                    "You have unlocked " + style.getDisplayName() + "! The path is now open to you.")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            // Training — award mastery to style and check for new form unlocks
            int masteryGain = 10 * getMasterAccelerationMultiplier(session);
            prog.addStyleMastery(style, masteryGain);

            // Unlock new forms based on style mastery thresholds
            checkAndUnlockForms(prog, style);

            player.level.playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
            int styleMastery = prog.getStyleMastery(style);
            player.sendSystemMessage(Component.literal(
                    style.getDisplayName() + " mastery: " + styleMastery + " / 1000")
                    .withStyle(ChatFormatting.AQUA));
        }

        syncAndBroadcast(player, prog);
    }

    private static void applyFailConsequence(ServerPlayer player, ServerLevel level,
                                              BreathingStyles style,
                                              StyleMinigameServerManager mgr) {
        boolean blocked = mgr.recordFailure(player.getUUID(), style, level.getGameTime());
        StyleMinigameType type = StyleMinigameType.forStyle(style);

        if (blocked) {
            switch (type.getFailureConsequence()) {
                case COOLDOWN_ONE_DAY -> player.sendSystemMessage(
                        Component.literal("You have exhausted yourself. Rest for a full day before trying again.")
                                .withStyle(ChatFormatting.RED));
                case RETRY_AFTER_200T -> player.sendSystemMessage(
                        Component.literal("Collect yourself. You may try again shortly.")
                                .withStyle(ChatFormatting.YELLOW));
                default -> {}
            }
        } else if (type.hasLives()) {
            int livesLeft = mgr.getLivesRemaining(player.getUUID(), style);
            player.sendSystemMessage(Component.literal(
                    "Wrong. " + livesLeft + " attempt(s) remaining.")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    // ─── Form unlock logic ────────────────────────────────────────────────────

    /**
     * Unlocks forms for the given style based on the player's current style mastery.
     * Each form has a {@link BreathingForms#getStyleMasteryToUnlock()} threshold.
     */
    private static void checkAndUnlockForms(PlayerProgression prog, BreathingStyles style) {
        int mastery = prog.getStyleMastery(style);
        for (BreathingForms form : BreathingForms.values()) {
            if (form.getStyle() != style) continue;
            if (!prog.isFormUnlocked(form) && mastery >= form.getStyleMasteryToUnlock()) {
                prog.unlockForm(form);
            }
        }
    }

    // ─── Misc helpers ────────────────────────────────────────────────────────

    /**
     * Returns 2 if the teacher (NPC) can accelerate the student's current form;
     * otherwise returns 1 (no acceleration).
     */
    private static int getMasterAccelerationMultiplier(StyleMinigameSession session) {
        if (session.teacherUUID() != null) return 1; // player teachers don't accelerate
        // NPC acceleration is handled in StyleMasterRegistry; here we return 1 for simplicity.
        // Full acceleration would look up the master's acceleratedForms list and the student's
        // current focused form; kept as 1 for the base training pass reward.
        return 1;
    }

    /**
     * Tier is derived from the player's style mastery:
     * mastery 0–199 → tier 1, 200–399 → tier 2, 400–599 → tier 3, 600–799 → tier 4, 800+ → tier 5.
     */
    private static int calculateTier(PlayerProgression prog, BreathingStyles style) {
        int mastery = prog.getStyleMastery(style);
        return Math.min(5, mastery / 200 + 1);
    }

    private static void sendMasterDialogue(ServerPlayer student, StyleMasterRecord master,
                                            boolean isTraining) {
        String line = isTraining
                ? master.masterName() + ": \"Good. Show me what you have learned.\""
                : master.masterName() + ": \"So you seek to learn " + master.style().getDisplayName()
                  + ". Very well. Prove you are worthy.\"";
        student.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.ITALIC,
                ChatFormatting.WHITE));
    }

    private static void syncAndBroadcast(ServerPlayer player, PlayerProgression prog) {
        ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(
                player.getId(),
                prog.getRank(),
                prog.getUnlockedStyles(),
                prog.getStyleMasteryMap(),
                prog.getFormMasteryMap()
        ), player);
    }
}
