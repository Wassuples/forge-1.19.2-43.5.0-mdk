package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A scripted story mission. Each mission is one class implementing these hooks; {@link MissionManager} drives them.
 *
 * <p>All hooks run on the logical server. Run state lives in {@link MissionContext#state()} (persisted with the
 * player), so a mission survives relogs and server restarts. Entities a mission spawns for itself go through
 * {@link MissionContext#spawnScoped} and are removed automatically when the mission ends.
 *
 * <p>Text uses translation keys under {@code mission.demonslayer.<path>.*}.
 */
public abstract class StoryMission {
    private final ResourceLocation id;
    private final String path;
    private final int number;
    private final Arc arc;

    protected StoryMission(String path, int number, Arc arc) {
        this.id = new ResourceLocation(DemonSlayer.MOD_ID, path);
        this.path = path;
        this.number = number;
        this.arc = arc;
    }

    public final ResourceLocation id() {
        return id;
    }

    public final int number() {
        return number;
    }

    public final Arc arc() {
        return arc;
    }

    protected final String key(String suffix) {
        return "mission.demonslayer." + path + "." + suffix;
    }

    public Component title() {
        return Component.translatable(key("title"));
    }

    public Component summary() {
        return Component.translatable(key("summary"));
    }

    public Component location() {
        return Component.translatable(key("location"));
    }

    /** Objective outline for the crow's details pane. */
    public abstract List<Component> briefing();

    /** Implemented missions return true; planned ones show as sealed in the list. */
    public boolean isPlayable() {
        return true;
    }

    /** Story prerequisites (previous missions). Shown as LOCKED when false. */
    public abstract boolean isUnlocked(ServerPlayer player, PlayerMissions missions);

    /** Explanation for the LOCKED state. */
    public Component lockedReason(ServerPlayer player, PlayerMissions missions) {
        return Component.translatable("mission.demonslayer.locked.previous");
    }

    /** Extra acceptance checks beyond {@link #isUnlocked}; return the reason it can't start, or null. */
    @Nullable
    public Component acceptBlocker(ServerPlayer player, PlayerMissions missions) {
        return null;
    }

    /** Missions handed out by someone in the world (not through the crow) only start from that person's dialogue. */
    public boolean requiresGiver() {
        return false;
    }

    /** Where to find the giver, shown when the crow is asked instead. */
    public Component giverHint() {
        return Component.translatable("mission.demonslayer.refuse.giver");
    }

    /** Whether the player may sleep through nights right now. */
    public boolean allowsSleep(MissionContext ctx) {
        return true;
    }

    // ------------------------------------------------------------------ lifecycle

    public abstract void onAccept(MissionContext ctx);

    /** Called once per second while active. */
    public abstract void tick(MissionContext ctx);

    /** The owner logged back in with this mission active. */
    public void onLogin(MissionContext ctx) {
    }

    /** The owner killed something. */
    public void onKill(MissionContext ctx, LivingEntity victim, DamageSource source) {
    }

    /** One of this run's scoped entities took damage (from anyone). */
    public void onScopedHurt(MissionContext ctx, String key, LivingEntity victim, DamageSource source, float amount) {
    }

    /** One of this run's scoped entities died (from anyone). */
    public void onScopedDeath(MissionContext ctx, String key, LivingEntity victim, DamageSource source) {
    }

    /** The owner died. Default: the mission fails. */
    public void onPlayerDeath(MissionContext ctx, DamageSource source) {
        ctx.fail(Component.translatable("mission.demonslayer.failed.death"));
    }

    /** Testing aid ({@code /demonslayer mission advance}): skip to the mission's next step. */
    public void onDebugAdvance(MissionContext ctx) {
    }

    /** The mission succeeded on {@code outcome}: grant its rewards here (runs before {@link #onEnd}). */
    public void onComplete(MissionContext ctx, String outcome) {
    }

    /** Last hook before the run is cleared (success, failure or forfeit). Scoped entities are still present. */
    public void onEnd(MissionContext ctx, boolean success) {
    }

    /** HUD tracker contents. */
    public abstract MissionTracker tracker(MissionContext ctx);
}
