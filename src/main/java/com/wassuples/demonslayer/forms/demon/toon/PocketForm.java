package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import com.wassuples.demonslayer.entity.custom.ToonSlotMachineEntity;
import com.wassuples.demonslayer.util.ToonSfx;
import com.wassuples.demonslayer.util.ToonPocketOutcome;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import com.wassuples.demonslayer.entity.custom.ToonPieEntity;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
            this.roll = level.random.nextInt(ToonPocketOutcome.count());
            // The slot machine is a world object now, so the caster AND their targets watch it spin.
            level.addFreshEntity(new ToonSlotMachineEntity(level, user, this.roll, RESOLVE_TICK, TOTAL_TICKS + 12));
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        if (level.isClientSide || resolved) {
            return;
        }

        if (ticksActive > 0 && ticksActive < RESOLVE_TICK && ticksActive % 3 == 0) {
            // reel clicks, slowing down as the reels stop (they stop at RESOLVE_TICK-12, -6 and 0)
            if (ticksActive < RESOLVE_TICK - 12 || ticksActive % 6 == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 0.45f, 1.2f + ticksActive * 0.025f);
            }
            if (ticksActive % 6 == 0) {
                sparkle();
            }
        }
        if (ticksActive == RESOLVE_TICK - 12 || ticksActive == RESOLVE_TICK - 6) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 0.7f, 1.4f);
        }

        if (ticksActive >= RESOLVE_TICK) {
            resolved = true;
            sparkle();
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.7f);
            switch (ToonPocketOutcome.byIndex(roll)) {
                case MALLET -> mallet();
                case HOT_POTATO -> hotPotato();
                case FORTUNE_COOKIE -> fortuneCookie();
                case FISH -> fish();
                case SPRING_GLOVE -> springGlove();
                case MAGNET -> magnet();
                case RUBBER_CHICKEN -> rubberChicken();
                case RAIN_CLOUD -> rainCloud();
                case CREAM_PIE -> creamPie();
                case BANANA_BUNCH -> bananaBunch();
                case CUCKOO_CLOCK -> cuckooClock();
                case SPRING_SHOES -> springShoes();
            }
        }
    }

    private void sparkle() {
        Vec3 pos = ToonSlotMachineEntity.anchorFor(user);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y - 0.2, pos.z, 10, 0.45, 0.25, 0.45, 0.02);
    }

    private void mallet() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 4.0);
        if (target == null) {
            Vec3 show = user.getEyePosition().add(user.getLookAngle().scale(1.8));
            ToonForceHelper.spawnToonText(level, show, Component.literal("HAMMER?").withStyle(ChatFormatting.GRAY), 18);
            level.sendParticles(ParticleTypes.CRIT, show.x, show.y, show.z, 8, 0.25, 0.25, 0.25, 0.0);
            return;
        }

        showVictimResult(target, 0);

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

        showVictimResult(target, 1);

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

        showVictimResult(target, 3);

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

    private void showVictimResult(LivingEntity target, int result) {
        // Victims already watched the in-world slot machine; give the result a punchy word over their head.
        ToonSfx.Kind kind = switch (result) {
            case 0 -> ToonSfx.Kind.HAMMER;
            case 1 -> ToonSfx.Kind.TICK;
            default -> ToonSfx.Kind.FISH;
        };
        ToonSfx.spawn(level, target.position().add(0, target.getBbHeight() + 0.9, 0), kind, 0.8f);
    }

    // ------------------------------------------------------------------ extra gadgets

    /** "Whiff" feedback when a targeted gadget has nobody to hit. */
    private void whiff(String text) {
        Vec3 show = user.getEyePosition().add(user.getLookAngle().scale(1.6));
        ToonSfx.spawn(level, show, ToonSfx.Kind.CUSTOM, text, -1, 20, 0.9f);
        level.sendParticles(ParticleTypes.POOF, show.x, show.y, show.z, 8, 0.2, 0.2, 0.2, 0.02);
    }

    /** Boxing glove on a spring: one huge punch that sends the target flying. */
    private void springGlove() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 7.0);
        if (target == null) {
            whiff("BOING?");
            return;
        }
        Vec3 push = target.position().subtract(user.position()).multiply(1.0, 0.0, 1.0);
        push = push.lengthSqr() < 1.0E-4 ? user.getLookAngle() : push.normalize();
        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, 5.0f));
        target.setDeltaMovement(push.scale(2.2).add(0, 0.55, 0));
        target.hurtMarked = true;
        ToonForceHelper.tryApplyDizzy(target, user, 0.5f);
        FormAnimations.play(target, FormAnimations.TOON_BONKED);
        ToonSfx.spawn(level, target.getEyePosition().add(0, 0.5, 0), ToonSfx.Kind.POW, 1.2f);
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getEyeY(), target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, user.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, 1.3f);
        level.playSound(null, target.blockPosition(), ModSounds.TOON_BONK.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** Giant horseshoe magnet: yanks every nearby enemy toward the caster. */
    private void magnet() {
        List<LivingEntity> pulled = level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(10.0),
                e -> e != user && e.isAlive() && !e.isSpectator());
        Vec3 pos = user.getEyePosition().add(user.getLookAngle().scale(1.2));
        ToonSfx.spawn(level, pos.add(0, 0.6, 0), ToonSfx.Kind.THWOOMP);
        level.playSound(null, user.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0f, 0.6f);
        for (LivingEntity e : pulled) {
            Vec3 toUser = user.position().subtract(e.position());
            double dist = Math.max(1.0, toUser.length());
            e.setDeltaMovement(toUser.normalize().scale(Math.min(1.6, 0.35 * dist)).add(0, 0.25, 0));
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                    6, 0.2, 0.3, 0.2, 0.05);
        }
        if (pulled.isEmpty()) {
            whiff("CLANK?");
        }
    }

    /** Rubber chicken slap: barely hurts, hugely humiliating. */
    private void rubberChicken() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 4.5);
        if (target == null) {
            whiff("BAWK?");
            return;
        }
        target.hurt(CombatHelper.getDamageSource(user), CombatHelper.calculateFinalDamage(user, 2.0f));
        Vec3 push = user.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        target.setDeltaMovement(push.scale(0.9).add(0, 0.35, 0));
        target.hurtMarked = true;
        ToonForceHelper.tryApplyDizzy(target, user, 0.35f);
        ToonForceHelper.recordHumiliation(user, target, "rubber_chicken");
        FormAnimations.play(target, FormAnimations.TOON_BONKED);
        ToonSfx.spawn(level, target.getEyePosition().add(0, 0.6, 0), ToonSfx.Kind.BAWK, 1.1f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
                target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.3, 0.3, 0.08);
        level.playSound(null, target.blockPosition(), SoundEvents.CHICKEN_HURT, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    /** A personal rain cloud over the target (or over the caster, if nobody is there: it's that kind of day). */
    private void rainCloud() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 12.0);
        LivingEntity victim = target != null ? target : user;
        ToonForceHelper.applyRainCloud(victim, 120);
        ToonSfx.spawn(level, victim.position().add(0, victim.getBbHeight() + 1.5, 0), ToonSfx.Kind.CUSTOM,
                target != null ? "DRIZZLE!" : "UH OH...", -1, 26, 1.0f);
        level.playSound(null, victim.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    /** Pulls out a cream pie and throws it (same visible pie as Pie Splat). */
    private void creamPie() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 10.0);
        if (target == null) {
            whiff("SPLAT?");
            return;
        }
        level.addFreshEntity(new ToonPieEntity(level, user, target));
        FormAnimations.play(user, FormAnimations.TOON_PIE_SPLAT);
        level.playSound(null, user.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    /** Scatters a ring of banana peels. Careful where you step afterwards. */
    private void bananaBunch() {
        int count = 6;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count + level.random.nextDouble() * 0.4;
            ToonBananaPeelEntity peel = new ToonBananaPeelEntity(level, user);
            peel.setPos(user.getX(), user.getY() + 1.2, user.getZ());
            peel.setDeltaMovement(Math.cos(angle) * 0.45, 0.35, Math.sin(angle) * 0.45);
            level.addFreshEntity(peel);
        }
        ToonSfx.spawn(level, user.getEyePosition().add(0, 0.6, 0), ToonSfx.Kind.YEET);
        level.playSound(null, user.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** Cuckoo clock: the target's head gets the birds. No target? Guess whose head. */
    private void cuckooClock() {
        LivingEntity target = CombatHelper.getLookedAtEntity(user, 8.0);
        LivingEntity victim = target != null ? target : user;
        ToonForceHelper.spawnDizzyBirds(level, victim, 100);
        victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false, false));
        ToonSfx.spawn(level, victim.position().add(0, victim.getBbHeight() + 0.9, 0), ToonSfx.Kind.CUCKOO);
        level.playSound(null, victim.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE, SoundSource.PLAYERS, 1.0f, 1.8f);
        level.playSound(null, victim.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    /** Spring shoes: a huge cartoon bounce forward, landing safely. */
    private void springShoes() {
        Vec3 look = user.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
        user.setDeltaMovement(look.scale(0.9).add(0, 1.45, 0));
        user.hurtMarked = true;
        user.fallDistance = 0.0f;
        ToonForceHelper.grantSafeLanding(user, 20 * 10);
        ToonSfx.spawn(level, user.position().add(0, 0.3, 0), ToonSfx.Kind.BOING, 1.2f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, user.getX(), user.getY() + 0.1, user.getZ(), 20, 0.4, 0.05, 0.4, 0.15);
        level.playSound(null, user.blockPosition(), ModSounds.TOON_BOING.get(), SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
