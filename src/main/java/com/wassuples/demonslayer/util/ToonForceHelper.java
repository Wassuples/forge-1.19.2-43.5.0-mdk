package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.trait.DemonMemory;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.EmotionState;
import com.wassuples.demonslayer.nemesis.trait.EmotionType;
import com.wassuples.demonslayer.nemesis.trait.MemoryType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;

public final class ToonForceHelper {
    public static final String FLATTEN_UNTIL_TAG = "ds_toon_flatten_until";
    public static final String BANANA_SPIN_UNTIL_TAG = "ds_toon_banana_spin_until";
    private static final String BANANA_SPIN_STRENGTH_TAG = "ds_toon_banana_spin_strength";
    public static final String AIRWALK_TAG = "ds_toon_airwalk";
    public static final String HOT_POTATO_UNTIL_TAG = "ds_toon_hot_potato_until";
    public static final String HOT_POTATO_OWNER_TAG = "ds_toon_hot_potato_owner";
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

        if (source != null) {
            target.hurt(DamageSource.thorns(source), 0.0f);
        }
    }

    public static void applyDizzyStars(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, true, false, true));

        for (int i = 0; i < 14; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + level.random.nextDouble() * 0.4;
            double x = target.getX() + Math.cos(angle) * radius;
            double y = target.getEyeY() + 0.25 + level.random.nextDouble() * 0.3;
            double z = target.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0, 0, 0, 0.0);
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

    public static void applyBananaSlip(LivingEntity target, @Nullable LivingEntity owner, float baseLaunch) {
        if (!(target.level instanceof ServerLevel level)) {
            return;
        }

        Vec3 currentVel = target.getDeltaMovement();
        Vec3 direction = currentVel.lengthSqr() > 0.01 ? currentVel.normalize() : target.getLookAngle().normalize();
        float sprintBonus = target.isSprinting() ? 1.8f : 1.0f;
        Vec3 launch = direction.scale(baseLaunch * sprintBonus).add(0, 0.35, 0);
        target.setDeltaMovement(launch);
        target.hurtMarked = true;

        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false, true));
        FormAnimations.play(target, FormAnimations.TOON_SLIP);

        long spinUntil = level.getGameTime() + 30;
        target.getPersistentData().putLong(BANANA_SPIN_UNTIL_TAG, spinUntil);
        target.getPersistentData().putFloat(BANANA_SPIN_STRENGTH_TAG, 18.0f + level.random.nextFloat() * 12.0f);

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

        if (owner != null) {
            tryApplyDizzy(target, owner, 0.4f);
        }
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

        float strength = player.getPersistentData().getFloat(BANANA_SPIN_STRENGTH_TAG);
        float yaw = player.getYRot() + (level.random.nextFloat() - 0.5f) * strength;
        float pitch = Mth.clamp(player.getXRot() + (level.random.nextFloat() - 0.5f) * (strength * 0.3f), -60.0f, 60.0f);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
        player.setSprinting(false);
    }

    public static void recordFishHumiliation(LivingEntity user, LivingEntity target) {
        if (!(target instanceof NemesisDemonEntity nemesis) || !(target.level instanceof ServerLevel level)) {
            return;
        }

        nemesis.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            long time = level.getGameTime();
            trait.addMemory(DemonMemory.of(MemoryType.HUMILIATED, time, user.getUUID(), nemesis.getUUID(), Map.of("toon_force", "fish")));

            EmotionState emotions = trait.getEmotionState();
            float delta = Mth.nextFloat(level.random, 0.05f, 0.10f);
            emotions.setIntensity(EmotionType.HATE_PLAYER, emotions.getIntensity(EmotionType.HATE_PLAYER) + delta, time);
        });
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

        from.getPersistentData().remove(HOT_POTATO_UNTIL_TAG);
        to.getPersistentData().putLong(HOT_POTATO_UNTIL_TAG, until);

        if (from.getPersistentData().hasUUID(HOT_POTATO_OWNER_TAG)) {
            to.getPersistentData().putUUID(HOT_POTATO_OWNER_TAG, from.getPersistentData().getUUID(HOT_POTATO_OWNER_TAG));
        }

        level.sendParticles(ParticleTypes.SMOKE, to.getX(), to.getEyeY(), to.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
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
    }

    public static void spawnBonkText(ServerLevel level, Vec3 pos) {
        spawnToonText(level, pos.add(0, 0.6, 0), Component.literal("BONK!").withStyle(ChatFormatting.GOLD), 24);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 12, 0.35, 0.25, 0.35, 0.0);
    }

    public static void spawnToonText(ServerLevel level, Vec3 pos, Component text, int durationTicks) {
        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(text);
        stand.getPersistentData().putLong(TOON_TEXT_UNTIL_TAG, level.getGameTime() + durationTicks);
        level.addFreshEntity(stand);
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
