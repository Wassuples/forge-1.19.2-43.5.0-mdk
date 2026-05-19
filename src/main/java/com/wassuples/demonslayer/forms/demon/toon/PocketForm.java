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
        String pick = fortunes.get(level.random.nextInt(fortunes.size()));
        if (user instanceof net.minecraft.server.level.ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Fortune Cookie: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(pick).withStyle(ChatFormatting.RED)));

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

