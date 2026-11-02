package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.entity.custom.ToonDizzyBirdsEntity;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.trait.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public final class ToonForceHelper {
    public static final String FLATTEN_UNTIL_TAG = "ds_toon_flatten_until";
    public static final String BANANA_SPIN_UNTIL_TAG = "ds_toon_banana_spin_until";
    private static final String BANANA_SPIN_STRENGTH_TAG = "ds_toon_banana_spin_strength";
    public static final String AIRWALK_TAG = "ds_toon_airwalk";
    public static final String HOT_POTATO_UNTIL_TAG = "ds_toon_hot_potato_until";
    public static final String HOT_POTATO_OWNER_TAG = "ds_toon_hot_potato_owner";
    private static final String HOT_POTATO_TRANSFER_LOCK_TAG = "ds_toon_hot_potato_transfer_lock";
    private static final String TOON_TEXT_UNTIL_TAG = "ds_toon_text_until";

    private ToonForceHelper() {
    }

    public static void tryApplyDizzy(LivingEntity target, @Nullable LivingEntity source, float chance) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        if (level.random.nextFloat() > chance) {
            return;
        }

        applyDizzyStars(level, target);

    }

    public static void applyDizzyStars(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, true, false, true));

        for (int i = 0; i < 14; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + level.random.nextDouble() * 0.4;
            double x = target.getX() + Math.cos(angle) * radius;
            double y = target.getEyeY() + 0.25 + level.random.nextDouble() * 0.3;
            double z = target.getZ() + Math.sin(angle) * radius;
            level.sendParticles(i % 2 == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                    x, y, z, 1, 0, 0.015, 0, 0.0);
        }
    }

    public static void applyFlattened(LivingEntity target, int durationTicks) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        long until = level.getGameTime() + durationTicks;
        target.getPersistentData().putLong(FLATTEN_UNTIL_TAG, until);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 3, true, false, true));
    }

    public static void tickFlattened(LivingEntity target) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        long until = target.getPersistentData().getLong(FLATTEN_UNTIL_TAG);
        if (until <= level.getGameTime()) {
            if (until > 0) {
                target.getPersistentData().remove(FLATTEN_UNTIL_TAG);
            }
            return;
        }

        target.setPose(Pose.SWIMMING);
        target.setSprinting(false);
    }

    /** Length of the slip/tunnel-bonk "flat on your back" sequence; matches the client animation and camera. */
    public static final int SLIP_DOWN_TICKS = 44;

    public static void applyBananaSlip(LivingEntity target, @Nullable LivingEntity owner, float baseLaunch) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        // Feet shoot out along the direction of travel; the body goes up a little and lands on its back.
        Vec3 currentVel = target.getDeltaMovement();
        Vec3 horizontal = new Vec3(currentVel.x, 0.0, currentVel.z);
        Vec3 direction = horizontal.lengthSqr() > 0.0025 ? horizontal.normalize()
                : new Vec3(target.getLookAngle().x, 0.0, target.getLookAngle().z).normalize();
        float sprintBonus = target.isSprinting() ? 1.6f : 1.0f;
        target.setDeltaMovement(direction.scale(baseLaunch * 0.55f * sprintBonus).add(0, 0.42, 0));
        target.hurtMarked = true;
        target.setSprinting(false);

        FormAnimations.play(target, FormAnimations.TOON_SLIP);
        knockFlat(level, target, SLIP_DOWN_TICKS);

        if (target instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty()) {
                ItemStack dropped = held.copy();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.drop(dropped, true);
            }
        }

        for (int i = 0; i < 10; i++) {
            level.sendParticles(ParticleTypes.ITEM_SLIME,
                    target.getX() + (level.random.nextDouble() - 0.5) * 0.6,
                    target.getY() + 0.2 + level.random.nextDouble() * 0.4,
                    target.getZ() + (level.random.nextDouble() - 0.5) * 0.6,
                    1, 0, 0, 0, 0.0);
        }

        tryApplyDizzy(target, owner, owner != null ? 0.4f : 0.25f);
    }

    /**
     * Fake Tunnel: a non-owner runs face-first into the "tunnel", rebounds, lands on their back and
     * gets a ring of cartoon birds. {@code wallNormal} points out of the wall, toward the victim.
     */
    public static void applyTunnelBonk(LivingEntity target, @Nullable LivingEntity owner, Vec3 wallNormal) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        Vec3 out = new Vec3(wallNormal.x, 0.0, wallNormal.z).normalize();
        target.setDeltaMovement(out.scale(0.55).add(0, 0.32, 0));
        target.hurtMarked = true;
        target.setSprinting(false);

        FormAnimations.play(target, FormAnimations.TOON_TUNNEL_SLAM);
        knockFlat(level, target, SLIP_DOWN_TICKS);
        spawnDizzyBirds(level, target, 100);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));

        Vec3 head = target.position().add(0, target.getBbHeight() + 0.2, 0).subtract(out.scale(0.2));
        ToonSfx.spawn(level, head, ToonSfx.Kind.CLONK, 1.15f);
        level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 14, 0.25, 0.2, 0.25, 0.1);
        level.playSound(null, target.blockPosition(), com.wassuples.demonslayer.sound.ModSounds.TOON_BONK.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    /** Shared "flat on the floor" state: can't sprint or really move until the get-up part of the animation. */
    private static void knockFlat(ServerLevel level, LivingEntity target, int ticks) {
        target.getPersistentData().putLong(BANANA_SPIN_UNTIL_TAG, level.getGameTime() + ticks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks - 6, 5, true, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false, true));
    }

    public static void spawnDizzyBirds(ServerLevel level, LivingEntity target, int durationTicks) {
        // Refresh instead of stacking a second flock.
        for (ToonDizzyBirdsEntity existing : level.getEntitiesOfClass(ToonDizzyBirdsEntity.class,
                target.getBoundingBox().inflate(2.0), birds -> birds.getTargetId() == target.getId())) {
            existing.discard();
        }
        ToonDizzyBirdsEntity birds = new ToonDizzyBirdsEntity(level, target, durationTicks);
        level.addFreshEntity(birds);
    }

    public static void tickBananaSpin(Player player) {
        if (!(player.level instanceof ServerLevel level)) {
            return;
        }

        long until = player.getPersistentData().getLong(BANANA_SPIN_UNTIL_TAG);
        if (until <= level.getGameTime()) {
            if (until > 0) {
                player.getPersistentData().remove(BANANA_SPIN_UNTIL_TAG);
                player.getPersistentData().remove(BANANA_SPIN_STRENGTH_TAG);
            }
            return;
        }

        // The old random yaw/pitch jitter is gone: the server can't rotate a player's camera anyway,
        // and the client-side camera now follows the fall-on-your-back animation instead.
        player.setSprinting(false);
    }

    public static void tickBananaSpin(LivingEntity target) {
        if (target instanceof Player player) {
            tickBananaSpin(player);
        }
    }

    public static void recordFishHumiliation(LivingEntity user, LivingEntity target) {
        recordHumiliation(user, target, "fish");
    }

    /** Nemesis demons remember being made fools of (fish slap, rubber chicken...) and hate the player a bit more. */
    public static void recordHumiliation(LivingEntity user, LivingEntity target, String how) {
        if (!(target instanceof NemesisDemonEntity nemesis) || !(target.level instanceof ServerLevel level)) {
            return;
        }

        nemesis.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            long time = level.getGameTime();
            trait.addMemory(DemonMemory.of(MemoryType.HUMILIATED, time, user.getUUID(), nemesis.getUUID(), Map.of("toon_force", how)));

            EmotionState emotions = trait.getEmotionState();
            float delta = Mth.nextFloat(level.random, 0.05f, 0.10f);
            emotions.setIntensity(EmotionType.HATE_PLAYER, emotions.getIntensity(EmotionType.HATE_PLAYER) + delta, time);
        });
    }

    // ------------------------------------------------------------------ Pocket gadgets

    public static final String RAIN_CLOUD_UNTIL_TAG = "ds_toon_rain_cloud_until";
    public static final String SAFE_LANDING_UNTIL_TAG = "ds_toon_safe_landing_until";

    /** A tiny personal storm cloud follows the target: drizzle, slowness, and the odd lightning zap. */
    public static void applyRainCloud(LivingEntity target, int ticks) {
        if (target.level instanceof ServerLevel level) {
            target.getPersistentData().putLong(RAIN_CLOUD_UNTIL_TAG, level.getGameTime() + ticks);
        }
    }

    public static void tickRainCloud(LivingEntity target) {
        if (!(target.level instanceof ServerLevel level)) return;
        long until = target.getPersistentData().getLong(RAIN_CLOUD_UNTIL_TAG);
        if (until <= 0L) return;
        if (until <= level.getGameTime() || !target.isAlive()) {
            target.getPersistentData().remove(RAIN_CLOUD_UNTIL_TAG);
            return;
        }

        double cloudY = target.getY() + target.getBbHeight() + 1.1;
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), cloudY, target.getZ(), 3, 0.35, 0.08, 0.35, 0.0);
        level.sendParticles(ParticleTypes.FALLING_WATER, target.getX(), cloudY - 0.15, target.getZ(), 2, 0.3, 0.0, 0.3, 0.0);
        target.clearFire();
        if (target.tickCount % 10 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 0, true, false, false));
        }
        if (level.random.nextInt(45) == 0) {
            target.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY(), target.getZ(), 18, 0.3, 0.6, 0.3, 0.1);
            ToonSfx.spawn(level, new Vec3(target.getX(), cloudY + 0.3, target.getZ()), ToonSfx.Kind.ZAP, 0.9f);
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);
            FormAnimations.play(target, FormAnimations.TOON_BONKED);
        }
    }

    /** Next landing within {@code ticks} takes no fall damage and boings instead (Spring Shoes). */
    public static void grantSafeLanding(LivingEntity target, int ticks) {
        if (target.level instanceof ServerLevel level) {
            target.getPersistentData().putLong(SAFE_LANDING_UNTIL_TAG, level.getGameTime() + ticks);
        }
    }

    /** Consumes the safe landing if one is active. */
    public static boolean consumeSafeLanding(LivingEntity target) {
        long until = target.getPersistentData().getLong(SAFE_LANDING_UNTIL_TAG);
        if (until <= 0L) return false;
        target.getPersistentData().remove(SAFE_LANDING_UNTIL_TAG);
        return until >= target.level.getGameTime();
    }

    public static void applyHotPotatoBomb(LivingEntity target, @Nullable LivingEntity owner, int fuseTicks) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        long until = level.getGameTime() + fuseTicks;
        target.getPersistentData().putLong(HOT_POTATO_UNTIL_TAG, until);
        if (owner != null) {
            target.getPersistentData().putUUID(HOT_POTATO_OWNER_TAG, owner.getUUID());
        }

        level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getEyeY(), target.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.TNT_PRIMED, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    public static boolean hasHotPotatoBomb(LivingEntity target) {
        return target.getPersistentData().contains(HOT_POTATO_UNTIL_TAG);
    }

    public static void transferHotPotatoBomb(LivingEntity from, LivingEntity to) {
        if (!(from.level instanceof ServerLevel level)) {
            return;
        }

        long until = from.getPersistentData().getLong(HOT_POTATO_UNTIL_TAG);
        if (until <= level.getGameTime()) {
            clearHotPotatoBomb(from);
            return;
        }

        UUID owner = from.getPersistentData().hasUUID(HOT_POTATO_OWNER_TAG)
                ? from.getPersistentData().getUUID(HOT_POTATO_OWNER_TAG) : null;
        clearHotPotatoBomb(from);
        to.getPersistentData().putLong(HOT_POTATO_UNTIL_TAG, until);
        to.getPersistentData().putLong(HOT_POTATO_TRANSFER_LOCK_TAG, level.getGameTime() + 8);

        if (owner != null) {
            to.getPersistentData().putUUID(HOT_POTATO_OWNER_TAG, owner);
        }

        level.sendParticles(ParticleTypes.SMOKE, to.getX(), to.getEyeY(), to.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        FormAnimations.play(from, FormAnimations.TOON_BOMB_PASS);
        FormAnimations.play(to, FormAnimations.TOON_BOMB_GET);
    }

    public static void tickHotPotatoBomb(LivingEntity target) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        long until = target.getPersistentData().getLong(HOT_POTATO_UNTIL_TAG);
        if (until <= 0L) {
            return;
        }

        if (level.getGameTime() >= until) {
            clearHotPotatoBomb(target);
            level.explode(null, target.getX(), target.getY(), target.getZ(), 2.5f, net.minecraft.world.level.Explosion.BlockInteraction.NONE);
            return;
        }

        if (target.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getEyeY(), target.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.WOODEN_BUTTON_CLICK_ON, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.6f);

            // Hot potato transfer on collision
            if (target.getPersistentData().getLong(HOT_POTATO_TRANSFER_LOCK_TAG) > level.getGameTime()) {
                return;
            }
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(0.4))) {
                if (other != target && other.isAlive()) {
                    transferHotPotatoBomb(target, other);
                    break;
                }
            }
        }
    }

    public static void clearHotPotatoBomb(LivingEntity target) {
        target.getPersistentData().remove(HOT_POTATO_UNTIL_TAG);
        target.getPersistentData().remove(HOT_POTATO_OWNER_TAG);
        target.getPersistentData().remove(HOT_POTATO_TRANSFER_LOCK_TAG);
    }

    public static void spawnBonkText(ServerLevel level, Vec3 pos) {
        ToonSfx.spawn(level, pos.add(0, 0.6, 0), ToonSfx.Kind.BONK);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 12, 0.35, 0.25, 0.35, 0.0);
    }

    /**
     * Kept for every existing call site. The text is classified into a {@link ToonSfx.Kind} when it matches
     * one (so "BOING!" gets random BOING variants), otherwise it keeps its spelling but still gets the full
     * cartoon styling. Prefer {@link ToonSfx#spawn} in new code.
     */
    public static void spawnToonText(ServerLevel level, Vec3 pos, Component text, int durationTicks) {
        ToonSfx.spawnFromComponent(level, pos, text, durationTicks);
    }

    public static void tickToonText(LivingEntity livingEntity) {
        if (!(livingEntity.level instanceof ServerLevel level)) {
            return;
        }

        if (!(livingEntity instanceof ArmorStand)) {
            return;
        }

        long until = livingEntity.getPersistentData().getLong(TOON_TEXT_UNTIL_TAG);
        if (until > 0 && level.getGameTime() >= until) {
            livingEntity.discard();
        }
    }
}
