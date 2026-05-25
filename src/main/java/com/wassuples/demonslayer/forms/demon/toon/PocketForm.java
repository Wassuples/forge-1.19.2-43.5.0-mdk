package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.CombatHelper;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ModUtils;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PocketForm extends TimedForm {
    private static final int HOT_POTATO_FUSE = 60;
    private static final float MALLET_DAMAGE = 8.0f;
    private static final float FISH_DAMAGE = 1.0f;
    private static final int RESOLVE_TICK = 32;
    private static final int TOTAL_TICKS = 40;
    private static final String[] SLOT_FRAMES = {
            "[ 7 | HAMMER | COD ]",
            "[ BOMB | ? | COOKIE ]",
            "[ COD | 7 | HAMMER ]",
            "[ COOKIE | BOMB | ? ]"
    };
    private int roll = -1;
    private boolean resolved;

    public PocketForm(LivingEntity user, IForm formType) {
        super(user, formType, TOTAL_TICKS);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_POCKET);
            this.roll = level.random.nextInt(4);
            showSlotFrame("[ ? | ? | ? ]", ChatFormatting.GOLD, 12);
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        if (level.isClientSide || resolved) {
            return;
        }

        if (ticksActive > 0 && ticksActive < RESOLVE_TICK && ticksActive % 5 == 0) {
            String frame = SLOT_FRAMES[(ticksActive / 5 + level.random.nextInt(SLOT_FRAMES.length)) % SLOT_FRAMES.length];
            showSlotFrame(frame, ChatFormatting.YELLOW, 10);
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 0.45f, 1.2f + ticksActive * 0.025f);
        }

        if (ticksActive >= RESOLVE_TICK) {
            resolved = true;
            showSlotFrame(resultFrame(), ChatFormatting.GREEN, 26);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.7f);
            switch (roll) {
                case 0 -> mallet();
                case 1 -> hotPotato();
                case 2 -> fortuneCookie();
                default -> fish();
            }
        }
    }

    private void showSlotFrame(String text, ChatFormatting color, int durationTicks) {
        Vec3 pos = slotMachinePos();
        ToonForceHelper.spawnToonText(level, pos, Component.literal(text).withStyle(color), durationTicks);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y - 0.2, pos.z, 10, 0.35, 0.2, 0.35, 0.02);
    }

    private Vec3 slotMachinePos() {
        Vec3 look = user.getLookAngle().normalize();
        return user.getEyePosition().add(look.scale(1.65)).add(0, 0.35, 0);
    }

    private String resultFrame() {
        return switch (roll) {
            case 0 -> "[ HAMMER | HAMMER | HAMMER ]";
            case 1 -> "[ BOMB | BOMB | BOMB ]";
            case 2 -> "[ COOKIE | COOKIE | COOKIE ]";
            default -> "[ COD | COD | COD ]";
        };
    }

    private void mallet() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 4.0);
        if (target == null) {
            Vec3 show = user.getEyePosition().add(user.getLookAngle().scale(1.8));
            ToonForceHelper.spawnToonText(level, show, Component.literal("HAMMER?").withStyle(ChatFormatting.GRAY), 18);
            level.sendParticles(ParticleTypes.CRIT, show.x, show.y, show.z, 8, 0.25, 0.25, 0.25, 0.0);
            return;
        }

        ToonForceHelper.spawnToonText(level, target.position().add(0, target.getBbHeight() + 0.35, 0),
                Component.literal("HAMMER!").withStyle(ChatFormatting.GOLD), 22);
        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, MALLET_DAMAGE));
        ToonForceHelper.applyFlattened(target, 40);
        ToonForceHelper.tryApplyDizzy(target, user, 0.3f);
        ToonForceHelper.spawnBonkText(level, target.position());
        FormAnimations.play(target, FormAnimations.TOON_BONKED);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 18, 0.35, 0.25, 0.35, 0.0);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.4f);
    }

    private void hotPotato() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 6.0);
        if (target == null) {
            Vec3 show = user.getEyePosition().add(user.getLookAngle().scale(1.5));
            ToonForceHelper.spawnToonText(level, show, Component.literal("TICK?").withStyle(ChatFormatting.RED), 18);
            FormAnimations.play(user, FormAnimations.TOON_BOMB_GET);
            level.sendParticles(ParticleTypes.SMOKE, show.x, show.y, show.z, 8, 0.2, 0.2, 0.2, 0.0);
            return;
        }

        ToonForceHelper.applyHotPotatoBomb(target, user, HOT_POTATO_FUSE);
        ToonForceHelper.tryApplyDizzy(target, user, 0.25f);
        FormAnimations.play(user, FormAnimations.TOON_BOMB_PASS);
        FormAnimations.play(target, FormAnimations.TOON_BOMB_GET);
        ToonForceHelper.spawnToonText(level, target.position().add(0, target.getBbHeight() + 0.25, 0),
                Component.literal("TICK TICK!").withStyle(ChatFormatting.RED), 26);
        level.playSound(null, target.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private void fortuneCookie() {
        FormAnimations.play(user, FormAnimations.TOON_COOKIE);
        List<String> fortunes = List.of(
                "DEATH!",
                "UH OH!",
                "LOOK UP!",
                "YAY!",
                "THE WIND IS LAUGHING.",
                "DON'T STEP THERE.",
                "BOING!");
        int index = level.random.nextInt(fortunes.size());
        String pick = fortunes.get(index);

        if (user instanceof net.minecraft.server.level.ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Fortune Cookie: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(pick).withStyle(ChatFormatting.RED)));
            ToonForceHelper.spawnToonText(level, player.position().add(0, player.getBbHeight() + 0.75, 0),
                    Component.literal(pick).withStyle(ChatFormatting.GOLD), 44);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COOKIE)),
                    player.getX(), player.getEyeY(), player.getZ(), 18, 0.3, 0.2, 0.3, 0.05);

            switch (pick) {
                case "DEATH!" -> {
                    player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0f);
                    level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
                }
                case "LOOK UP!" -> {
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE, SoundSource.PLAYERS, 1.0f, 2.0f);
                    ToonAnvilEntity anvil = new ToonAnvilEntity(com.wassuples.demonslayer.entity.ModEntityTypes.TOON_ANVIL.get(), level);
                    anvil.setPos(player.getX(), player.getY() + 5.0, player.getZ());
                    level.addFreshEntity(anvil);
                    player.hurt(net.minecraft.world.damagesource.DamageSource.ANVIL, 4.0f);
                    FormAnimations.play(player, FormAnimations.TOON_BONKED);
                    level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 3, player.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 3, player.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
                }
                case "BOING!" -> {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 1.2, 0));
                    player.hurtMarked = true;
                    level.playSound(null, player.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                case "YAY!" -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK, 600, 1));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 1));
                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
                }
            }

            if (level.random.nextFloat() < 0.005f) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    demon.addBloodPoints(500);
                    ModUtils.syncDemon(player);
                });
                player.sendSystemMessage(Component.literal("JACKPOT!").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private void fish() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 4.0);
        if (target == null) {
            Vec3 show = user.getEyePosition().add(user.getLookAngle().scale(1.7));
            FormAnimations.play(user, FormAnimations.TOON_FISH);
            ToonForceHelper.spawnToonText(level, show, Component.literal("FISH?").withStyle(ChatFormatting.AQUA), 18);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COD)),
                    show.x, show.y, show.z, 12, 0.2, 0.2, 0.2, 0.04);
            return;
        }

        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, FISH_DAMAGE));
        target.setDeltaMovement(new Vec3(0, 0.2, 0));
        ToonForceHelper.tryApplyDizzy(target, user, 0.15f);
        ToonForceHelper.recordFishHumiliation(user, target);
        FormAnimations.play(user, FormAnimations.TOON_FISH);
        FormAnimations.play(target, FormAnimations.TOON_BONKED);
        ToonForceHelper.spawnToonText(level, target.position().add(0, target.getBbHeight() + 0.25, 0),
                Component.literal("FISH!").withStyle(ChatFormatting.AQUA), 20);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COD)),
                target.getX(), target.getEyeY(), target.getZ(), 18, 0.28, 0.22, 0.28, 0.05);
        level.playSound(null, target.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.9f, 1.4f);
    }
}
