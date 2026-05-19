package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.CombatHelper;
import com.wassuples.demonslayer.util.ModUtils;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PocketForm extends ActiveForm {
    private static final int HOT_POTATO_FUSE = 60;
    private static final float MALLET_DAMAGE = 8.0f;
    private static final float FISH_DAMAGE = 1.0f;

    public PocketForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            int roll = level.random.nextInt(4);
            switch (roll) {
                case 0 -> mallet();
                case 1 -> hotPotato();
                case 2 -> fortuneCookie();
                default -> fish();
            }
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }

    private void mallet() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 4.0);
        if (target == null) {
            return;
        }

        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, MALLET_DAMAGE));
        ToonForceHelper.applyFlattened(target, 40);
        ToonForceHelper.tryApplyDizzy(target, user, 0.3f);
        ToonForceHelper.spawnBonkText(level, target.position());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.4f);
    }

    private void hotPotato() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 6.0);
        if (target == null) {
            return;
        }

        ToonForceHelper.applyHotPotatoBomb(target, user, HOT_POTATO_FUSE);
        ToonForceHelper.tryApplyDizzy(target, user, 0.25f);
        level.playSound(null, target.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private void fortuneCookie() {
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

            switch (pick) {
                case "DEATH!" -> {
                    player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0f);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
                }
                case "LOOK UP!" -> {
                    // Spawn a fake anvil above user
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE, SoundSource.PLAYERS, 1.0f, 2.0f);
                    // We can reuse AcmeAnvilForm's logic by just starting it on the user?
                    // No, AcmeAnvilForm targets someone. Let's just spawn particles and damage them after a delay.
                    // Or better, just give them a "hit" from above.
                    player.hurt(net.minecraft.world.damagesource.DamageSource.ANVIL, 4.0f);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, player.getX(), player.getY() + 3, player.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 3, player.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
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
            return;
        }

        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, FISH_DAMAGE));
        target.setDeltaMovement(new Vec3(0, 0.2, 0));
        ToonForceHelper.tryApplyDizzy(target, user, 0.15f);
        ToonForceHelper.recordFishHumiliation(user, target);
        level.playSound(null, target.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.9f, 1.4f);
    }
}

