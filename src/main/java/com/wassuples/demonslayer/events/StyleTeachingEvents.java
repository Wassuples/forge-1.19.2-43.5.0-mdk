// src/main/java/com/wassuples/demonslayer/events/StyleTeachingEvents.java
package com.wassuples.demonslayer.events;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.StyleMasterNpcEntity;
import com.wassuples.demonslayer.entity.custom.master.*;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;

import static com.mojang.datafixers.TypeRewriteRule.orElse;

@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StyleTeachingEvents {

    // --- Base forms per style -----------------------------------------------
    // Only forms part of the original style are required for P2P teaching.
    // Individual practitioner additions (Dead Calm, Honoikazuchi no Kami,
    // Obscuring Clouds) are excluded.
    private static final Map<BreathingStyles, Set<BreathingForms>> BASE_FORMS;
    static {
        Map<BreathingStyles, Set<BreathingForms>> m = new EnumMap<>(BreathingStyles.class);
        m.put(BreathingStyles.WATER,   setOf(
            BreathingForms.WATER_1, BreathingForms.WATER_2, BreathingForms.WATER_3,
            BreathingForms.WATER_4, BreathingForms.WATER_5, BreathingForms.WATER_6,
            BreathingForms.WATER_7, BreathingForms.WATER_8, BreathingForms.WATER_9,
            BreathingForms.WATER_10));
        // WATER_11 (Dead Calm) excluded -- Giyu's unique form
        m.put(BreathingStyles.THUNDER, setOf(
            BreathingForms.THUNDER_1, BreathingForms.THUNDER_2, BreathingForms.THUNDER_3,
            BreathingForms.THUNDER_4, BreathingForms.THUNDER_5, BreathingForms.THUNDER_6));
        // THUNDER_7 (Honoikazuchi no Kami) excluded -- Zenitsu's unique form
        m.put(BreathingStyles.MIST,    setOf(
            BreathingForms.MIST_1, BreathingForms.MIST_2, BreathingForms.MIST_3,
            BreathingForms.MIST_4, BreathingForms.MIST_5, BreathingForms.MIST_6));
        // MIST_7 (Obscuring Clouds) excluded -- Muichiro's unique form
        m.put(BreathingStyles.FLAME,   setOf(
            BreathingForms.FLAME_1, BreathingForms.FLAME_2, BreathingForms.FLAME_3,
            BreathingForms.FLAME_4, BreathingForms.FLAME_5, BreathingForms.FLAME_9));
        m.put(BreathingStyles.WIND,    setOf(
            BreathingForms.WIND_1, BreathingForms.WIND_2, BreathingForms.WIND_3,
            BreathingForms.WIND_4, BreathingForms.WIND_5, BreathingForms.WIND_6,
            BreathingForms.WIND_7, BreathingForms.WIND_8, BreathingForms.WIND_9));
        m.put(BreathingStyles.STONE,   setOf(
            BreathingForms.STONE_1, BreathingForms.STONE_2, BreathingForms.STONE_3,
            BreathingForms.STONE_4, BreathingForms.STONE_5));
        m.put(BreathingStyles.INSECT,  setOf(
            BreathingForms.INSECT_1, BreathingForms.INSECT_2,
            BreathingForms.INSECT_3, BreathingForms.INSECT_4));
        m.put(BreathingStyles.SOUND,   setOf(
            BreathingForms.SOUND_1, BreathingForms.SOUND_4, BreathingForms.SOUND_5));
        m.put(BreathingStyles.LOVE,    setOf(
            BreathingForms.LOVE_1, BreathingForms.LOVE_2, BreathingForms.LOVE_3, BreathingForms.LOVE_5, BreathingForms.LOVE_6));
        m.put(BreathingStyles.SERPENT, setOf(
            BreathingForms.SERPENT_1, BreathingForms.SERPENT_2, BreathingForms.SERPENT_3,
            BreathingForms.SERPENT_4, BreathingForms.SERPENT_5));
        BASE_FORMS = Collections.unmodifiableMap(m);
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        return new LinkedHashSet<>(Arrays.asList(items));
    }

    // --- Entity -> master ID mapping ----------------------------------------
    /**
     * Resolves a master ID string from any entity that can teach a style.
     * Uses entity type for Hashira NPCs (avoids relying on a getBreathingStyle() method),
     * and getMasterId() for StyleMasterNpcEntity subclasses.
     */
    @Nullable
    private static String getMasterIdForEntity(Entity entity) {
        // Non-Hashira masters -- check concrete subclass first
        if (entity instanceof UrokodakiEntity)      return "sakonji_urokodaki";
        if (entity instanceof ShinjuroEntity)        return "shinjuro_rengoku";
        if (entity instanceof JigoroEntity)          return "jigoro_kuwajima";
        if (entity instanceof TengenRetiredEntity)   return "tengen_uzui_retired";
        // Generic fallback for any other StyleMasterNpcEntity
        if (entity instanceof StyleMasterNpcEntity npc && !npc.getMasterId().isEmpty())
            return npc.getMasterId();

        // Hashira NPCs -- match by registered entity type
        EntityType<?> type = entity.getType();
        if (type == ModEntityTypes.WATER_HASHIRA.get())   return "giyu_tomioka";
        if (type == ModEntityTypes.FLAME_HASHIRA.get())   return "rengoku_kyojuro";
        if (type == ModEntityTypes.MIST_HASHIRA.get())    return "muichiro_tokito";
        if (type == ModEntityTypes.WIND_HASHIRA.get())    return "sanemi_shinazugawa";
        if (type == ModEntityTypes.STONE_HASHIRA.get())   return "gyomei_himejima";
        if (type == ModEntityTypes.INSECT_HASHIRA.get())  return "shinobu_kocho";
        if (type == ModEntityTypes.LOVE_HASHIRA.get())    return "mitsuri_kanroji";
        if (type == ModEntityTypes.SNAKE_HASHIRA.get())   return "obanai_iguro";
        if (type == ModEntityTypes.SOUND_HASHIRA.get())   return "tengen_uzui_active";
        // FIX: Thunder Hashira (Jigoro Kuwajima) was missing -- he could never start ThunderMinigame
        if (type == ModEntityTypes.THUNDER_HASHIRA.get()) return "jigoro_kuwajima";
        return null;
    }

    // --- Interaction event --------------------------------------------------

    @SubscribeEvent
    public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        // FIX: only process for the main hand to prevent every message and interaction
        // from firing twice (Forge fires EntityInteract once per hand).
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer interactor)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Player-to-player teaching: Teacher shift-right-clicks Student
        if (event.getTarget() instanceof ServerPlayer student && interactor.isShiftKeyDown()) {
            event.setCanceled(true);
            handlePlayerToPlayerTeaching(interactor, student, level);
            return;
        }

        // NPC / Hashira master
        String masterId = getMasterIdForEntity(event.getTarget());
        if (masterId != null) {
            event.setCanceled(true);
            handleNpcMasterInteraction(interactor, masterId, level);
        }
    }

    // --- NPC interaction flow -----------------------------------------------

    private static void handleNpcMasterInteraction(ServerPlayer student, String masterId,
                                                   ServerLevel level) {
        Optional<StyleMasterRecord> optRecord = StyleMasterRegistry.byId(masterId);
        if (optRecord.isEmpty()) {return;}
        StyleMasterRecord master = optRecord.get();
        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);

        if (mgr.hasActiveSession(student.getUUID())) return;

        // Cooldown
        if (mgr.isOnCooldown(student.getUUID(), master.style(), level.getGameTime())) {
            long remaining = (mgr.getCooldownEndTick(student.getUUID(), master.style())
                - level.getGameTime()) / 20;
            student.sendSystemMessage(Component.literal(
                    "You need to rest. Come back in ~" + remaining + "s.")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Access check
        StyleMasterAccess.Result access = master.access().evaluate(student, level);
        if (!access.allowed()) {
            student.sendSystemMessage(access.denialMessage());
            return;
        }

        PlayerProgression prog = student.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElse(null);
        if (prog == null) return;

        boolean isTraining = prog.isStyleUnlocked(master.style());
        int tier  = calculateTier(prog, master.style());
        int lives = StyleMinigameType.forStyle(master.style()).hasLives()
            ? mgr.getLivesRemaining(student.getUUID(), master.style()) : 0;
        UUID sid  = UUID.randomUUID();

        mgr.startSession(student.getUUID(), master.style(), isTraining, null);
        sendMasterDialogue(student, master, isTraining);
        ModMessages.sendToPlayer(new StartStyleMinigameS2CPacket(sid, master.style(), tier, lives), student);
    }

    // --- Player-to-player teaching ------------------------------------------

    private static void handlePlayerToPlayerTeaching(ServerPlayer teacher, ServerPlayer student,
                                                     ServerLevel level) {
        if (teacher.equals(student)) return;

        PlayerProgression teacherProg = teacher.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElse(null);
        if (teacherProg == null) return;

        PlayerStyle styleCap = teacher.getCapability(PlayerStyleProvider.PLAYER_STYLE)
            .orElse(null);
        if (styleCap == null) {return;}

        BreathingStyles style = styleCap.getCurrentStyles();
        if (style == null || style == BreathingStyles.NONE) {return;}

        // Check teacher has mastered all BASE forms of that style
        Set<BreathingForms> required = BASE_FORMS.getOrDefault(style, Collections.emptySet());
        List<BreathingForms> missing = new ArrayList<>();
        for (BreathingForms f : required) {
            if (teacherProg.getFormMastery(f) < f.getMasteryGoal()) missing.add(f);
        }
        if (!missing.isEmpty()) {
            teacher.sendSystemMessage(Component.literal(
                    "You have not mastered the base forms of " + style.getDisplayName() + " yet.")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }

        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);

        if (mgr.hasActiveSession(student.getUUID())) {
            return;
        }

        if (mgr.isOnCooldown(student.getUUID(), style, level.getGameTime())) {
            return;
        }

        PlayerProgression studentProg = student.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElse(null);

        boolean isTraining = studentProg.isStyleUnlocked(style);
        int tier  = calculateTier(studentProg, style);
        int lives = StyleMinigameType.forStyle(style).hasLives()
            ? mgr.getLivesRemaining(student.getUUID(), style) : 0;
        UUID sid  = UUID.randomUUID();

        mgr.startSession(student.getUUID(), style, isTraining, teacher.getUUID());
        teacher.sendSystemMessage(Component.literal(
                "You begin teaching " + student.getName().getString()
                    + " the ways of " + style.getDisplayName() + ".")
            .withStyle(ChatFormatting.GREEN));
        student.sendSystemMessage(Component.literal(
                teacher.getName().getString() + " begins teaching you "
                    + style.getDisplayName() + "!")
            .withStyle(ChatFormatting.GREEN));
        ModMessages.sendToPlayer(new StartStyleMinigameS2CPacket(sid, style, tier, lives), student);
    }

    // --- Minigame result handler --------------------------------------------

    public static void handleMinigameResult(ServerPlayer player, UUID sessionId, boolean passed) {
        if (!(player.level instanceof ServerLevel level)) return;

        StyleMinigameServerManager mgr = StyleMinigameServerManager.get(level);
        StyleMinigameSession session   = mgr.popSession(player.getUUID());
        if (session == null) {
            DemonSlayer.LOGGER.warn("[StyleTeaching] No active session for {} -- ignoring",
                player.getName().getString());
            return;
        }

        if (passed) applyPassReward(player, level, session, mgr);
        else        applyFailConsequence(player, level, session.style(), mgr);
    }

    private static void applyPassReward(ServerPlayer player, ServerLevel level,
                                        StyleMinigameSession session,
                                        StyleMinigameServerManager mgr) {
        BreathingStyles style = session.style();
        PlayerProgression prog = player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
            .orElse(null);
        if (prog == null) return;

        mgr.resetLives(player.getUUID(), style);

        if (!session.isTraining()) {
            prog.unlockStyle(style);
            // Auto-unlock first form
            BreathingForms firstForm = Arrays.stream(BreathingForms.values())
                .filter(f -> f.getStyle() == style).findFirst().orElse(null);
            if (firstForm != null) prog.unlockForm(firstForm);

            level.playSound(null, player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        } else {
            prog.addStyleMastery(style, 10);
            checkAndUnlockForms(prog, style);
            level.playSound(null, player.blockPosition(),
                SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
            player.sendSystemMessage(Component.literal(
                    style.getDisplayName() + " mastery: " + prog.getStyleMastery(style) + " / 1000")
                .withStyle(ChatFormatting.AQUA));
        }
        syncProgression(player, prog);
    }

    private static void applyFailConsequence(ServerPlayer player, ServerLevel level,
                                             BreathingStyles style,
                                             StyleMinigameServerManager mgr) {
        boolean blocked = mgr.recordFailure(player.getUUID(), style, level.getGameTime());
        StyleMinigameType type = StyleMinigameType.forStyle(style);
        if (blocked) {
            String msg = switch (type.getFailureConsequence()) {
                case COOLDOWN_ONE_DAY ->
                    "You have exhausted yourself. Rest a full day before trying again.";
                case RETRY_AFTER_200T ->
                    "Collect yourself. You may try again shortly.";
                default -> "";
            };
            if (!msg.isEmpty())
                player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        } else if (type.hasLives()) {
            int left = mgr.getLivesRemaining(player.getUUID(), style);
            player.sendSystemMessage(Component.literal(
                "Wrong. " + left + " attempt(s) remaining.").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void checkAndUnlockForms(PlayerProgression prog, BreathingStyles style) {
        int mastery = prog.getStyleMastery(style);
        for (BreathingForms form : BreathingForms.values()) {
            if (form.getStyle() != style) continue;
            if (!prog.isFormUnlocked(form) && mastery >= form.getStyleMasteryToUnlock()) {
                prog.unlockForm(form);
            }
        }
    }

    private static int calculateTier(PlayerProgression prog, BreathingStyles style) {
        return Math.min(5, prog.getStyleMastery(style) / 200 + 1);
    }

    private static void sendMasterDialogue(ServerPlayer student, StyleMasterRecord master,
                                           boolean training) {
        String line = training
            ? master.masterName() + ": \"Good. Show me what you have learned.\""
            : master.masterName() + ": \"So you seek " + master.style().getDisplayName()
              + ". Prove you are worthy.\"";
        student.sendSystemMessage(Component.literal(line)
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE));
    }

    private static void syncProgression(ServerPlayer player, PlayerProgression prog) {
        ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(
            player.getId(), prog.getRank(), prog.getUnlockedStyles(),
            prog.getStyleMasteryMap(), prog.getFormMasteryMap()), player);
    }
}
