package com.wassuples.demonslayer.styles;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BlindingMistCloudEntity;
import com.wassuples.demonslayer.entity.custom.MistCloneEntity;
import com.wassuples.demonslayer.entity.custom.ThrownNichirinEntity;
import com.wassuples.demonslayer.util.FormUtils;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public enum BreathingForms implements IForm {

    // =================================================================================
    // WATER BREATHING
    // =================================================================================

    WATER_1(
            BreathingStyles.WATER,
            "First Form: Water Surface Slash",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases a singular, horizontal slash aiming to decapitate their target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            FormUtils.dealDirectionalDamage(user, 4.0, 6.0f, 0.7); // 90 degree cone
            FormUtils.spawnParticleArc(user, ParticleTypes.SPLASH, 2.5, 140, 30);
        }
    },
    WATER_2(BreathingStyles.WATER,
            "Second Form: Water Wheel",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps forward into the air and performs a front flip, releasing a singular vertical slash in a circular motion.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WATER_3(BreathingStyles.WATER,
            "Third Form: Flowing Dance",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user swings and bends their blade in a winding motion along with their body and dances in a flowing pattern, slicing everything in its path.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WATER_4(BreathingStyles.WATER,
            "Fourth Form: Striking Tide",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs a multitude of slashes in a flowing fashion to release multiple consecutive slashes.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_SPLASH, 1.0f, 1.0f);
            FormUtils.pushPlayer(user, 2.5, false);
            FormUtils.dealThrustDamage(user, 5.0, 8.0f);
            FormUtils.spawnParticleArc(user, ParticleTypes.SPLASH, 3.0, 120, 25);
        }
    },
    WATER_5(BreathingStyles.WATER,
            "Fifth Form: Blessed Rain After the Drought",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user changes the grip on their sword and decapitates the opponent in a single flowing strike that causes little to no pain. It is a “sword strike of kindness” used when the enemy willingly surrenders.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            LivingEntity target = FormUtils.getLookedAtEntity(user, 5.0);

            if (target == null) {
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);
                FormUtils.spawnParticleArc(user, ParticleTypes.SPLASH, 2.0, 90, 15);
                return;
            }

            if (target.isCrouching()) {
                level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 2.0f);
                level.playSound(null, target.blockPosition(), SoundEvents.AMBIENT_CAVE, SoundSource.PLAYERS, 0.5f, 2.0f);

                float trueDamage = 10000.0f;
                DamageSource mercySource = DamageSource.playerAttack((Player)user).setMagic();
                target.hurt(mercySource, trueDamage);

                level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getEyeY(), target.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            } else {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.8f);
                user.sendSystemMessage(Component.literal("Your opponent has not yielded.").withStyle(ChatFormatting.AQUA));
            }
        }
    },
    WATER_6(BreathingStyles.WATER,
            "Sixth Form: Whirlpool",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user twists their body, using their blade to perform a fierce whirling motion which generates a whirlpool of air to shred apart anything caught within its current like a giant sharp blade.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WATER_7(BreathingStyles.WATER,
            "Seventh Form: Drop Ripple Thrust",
            IForm.InputType.HOLD,
            40,
            1,
            50,
            100,
            "The user performs a precise, fast and accurate thrust with their blade.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WATER_8(BreathingStyles.WATER,
            "Eighth Form: Waterfall Basin",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user cuts the target vertically in a flowing motion which is most effective when they are falling downward.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (!user.isOnGround() && user.getDeltaMovement().y < 0) {
                FormUtils.ActiveFormManager.start(user, this);
            } else {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
            }
        }
    },
    WATER_9(BreathingStyles.WATER,
            "Ninth Form: Splashing Water Flow, Turbulent",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user changes their footwork in a way that minimizes the landing time and surface needed when landing, allowing the user to move without limits. Ideal when fighting in a place with no solid foothold.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.SplashingWaterFlowForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    WATER_10(BreathingStyles.WATER,
            "Tenth Form: Constant Flux",
            IForm.InputType.PRESS,
            0,
            1,
            100,
            100,
            "The user charges toward the target whilst performing a continuous flowing attack with their blade which increases in strength and speed over time using momentum. The attacks seemingly take the form of a Japanese water dragon that accompanies the user in battle.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Toggleable form
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.ConstantFluxForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    WATER_11(BreathingStyles.WATER,
            "Eleventh Form: Dead Calm",
            IForm.InputType.PRESS,
            0,
            1,
            200,
            100,
            "The personal creation of the current Water Hashira, Giyu Tomioka. The user ceases all body movements and enters a state of complete tranquility, deflecting, blocking and cutting any incoming attacks with imperceptible speed with their blade. However, its effectiveness is limited as fast and numerous attacks can break through.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // FLAME BREATHING
    // =================================================================================

    FLAME_1(BreathingStyles.FLAME,
            "First Form: Unknowing Fire",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes towards the target at tremendous speeds, before unleashing a singular horizontal slash directed at the targets neck for a decapitation.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLAME_2(BreathingStyles.FLAME,
            "Second Form: Rising Scorching Sun",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user unleashes an arcing vertical slash in an upwards motion, often brought up from a tail guard.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            user.setDeltaMovement(user.getDeltaMovement().add(0, 0.6, 0));
            user.hurtMarked = true;
            FormUtils.performGenericAttack(user, SoundEvents.FIRECHARGE_USE, 1.0f, 1.0f);
            FormUtils.dealDirectionalDamage(user, 4.0, 7.0f, 0.6);
            FormUtils.spawnParticleArc(user, ParticleTypes.FLAME, 2.5, 160, 30);
        }
    },
    FLAME_3(BreathingStyles.FLAME,
            "Third Form: Blazing Universe",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs an arcing downwards vertical slash, often brought down from a high guard.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (user.isOnGround()) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
                return;
            }
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLAME_4(BreathingStyles.FLAME,
            "Fourth Form: Blooming Flame Undulation",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs a singular slash or multiple if needed, in a fluid circular motion to defend from incoming attacks and decapitate multiple targets.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLAME_5(BreathingStyles.FLAME,
            "Fifth Form: Flame Tiger",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes forth, bringing their blade into a high guard before performing a series of sword slashes which take the form of a flaming tiger enveloping the user.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.FlameTigerForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    FLAME_9(BreathingStyles.FLAME,
            "Ninth Form: Rengoku",
            IForm.InputType.HOLD,
            80,
            1,
            50,
            100,
            "The most powerful Flame Breathing technique. The user assumes a high stance before performing an extremely high-speed dash towards the target and unleashing a singular, devastating slash. The technique is powerful enough to completely carve out the ground in its wake. In the movie and anime adaptation, this technique seemingly took the form of a flaming Japanese dragon that envelopes the user as they are charging towards the target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // WIND BREATHING
    // =================================================================================

    WIND_1(BreathingStyles.WIND,
            "First Form: Dust Whirlwind Cutter",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes forward at blinding speeds and slashes continuously in a horizontal cyclone pattern.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_2(BreathingStyles.WIND,
            "Second Form: Claws-Purifying Wind",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user lifts the sword upwards towards the right, above their head and unleashes four vertical slashes at once down on the enemy resembling claws.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.4f);
            user.setDeltaMovement(user.getDeltaMovement().add(0, 0.6, 0));
            user.hurtMarked = true;

            final DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 1.0f);
            FormUtils.dealClawDamage(user, greenDust, 8.0f, 6.0, 4, 90.0, 0.7f);
        }
    },
    WIND_3(BreathingStyles.WIND,
            "Third Form: Clear Storm Wind Tree",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user unleashes a whirlwind of slashes around their body that can defend them from incoming attacks and slice up their surroundings.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_4(BreathingStyles.WIND,
            "Fourth Form: Rising Dust Storm",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases several whirlwind slashes above them from below their target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_5(BreathingStyles.WIND,
            "Fifth Form: Cold Mountain Wind",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user creates several circular arched slashes that increase in size from above their target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_6(BreathingStyles.WIND,
            "Sixth Form: Black Wind Mountain Mist",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user rotates their body in an uppercut movement, creating an arcing slash.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);

            final DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.2f), 1.0f);
            final DustParticleOptions whiteDust = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 1.0f);

            FormUtils.dealDirectionalDamage(user, 4.0, 8.0f, 0.4);

            FormUtils.spawnParticleArc(user, greenDust, 2.5, 180, 25);
            FormUtils.spawnParticleArc(user, whiteDust, 2.5, 180, 25);
        }
    },
    WIND_7(BreathingStyles.WIND,
            "Seventh Form: Gale, Sudden Gusts",
            IForm.InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air while swinging their blade which generates gale-force winds to shred their opponent apart.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_8(BreathingStyles.WIND,
            "Eighth Form: Primary Gale Slash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air and swings their blade which generates circular torrents of wind that slices up the opponent instantly.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    WIND_9(BreathingStyles.WIND,
            "Ninth Form: Idaten Typhoon",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user backflips into the air and while upside-down, unleashes a powerful gust of circular wind that slashes apart anything below.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // THUNDER BREATHING
    // =================================================================================
    THUNDER_1(BreathingStyles.THUNDER,
            "First Form: Thunderclap and Flash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user unsheathes their katana, dashes forward at blinding speeds and decapitates their opponent faster than they can react, before sheathing it back. This technique is extremely reminiscent of Iaijutsu and its movements are described to be akin to \"lightning ripping through the air\".",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THUNDER_2(BreathingStyles.THUNDER,
            "Second Form: Rice Spirit",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases five arched slashes accompanied by lightning in quick succession.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THUNDER_3(BreathingStyles.THUNDER,
            "Third Form: Thunder Swarm",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user surrounds the enemy with waves of arched lightning and attacks them from all directions.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THUNDER_4(BreathingStyles.THUNDER,
            "Fourth Form: Distant Thunder",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user generates a ball of electricity that releases waves of strong multi-directional lightning bolts from afar.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THUNDER_5(BreathingStyles.THUNDER,
            "Fifth Form: Heat Lightning",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user unleashes a long-ranged upward sword slash accompanied by lightning.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // 1. Perform a long-range raycast to find where the player is looking.
            // A range of 256 is effectively "infinite" for most gameplay.
            net.minecraft.world.phys.BlockHitResult hitResult = (net.minecraft.world.phys.BlockHitResult) user.pick(256, 0, false);
            if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                // If we're looking at the sky, do nothing.
                return;
            }

            // 2. The player performs their slash animation. They are free to move.
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 2.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 2.0f);

            // 3. Find the ground position at the distant target location.
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hitResult.getBlockPos());
            Vec3 groundCenter = Vec3.atBottomCenterOf(groundPos);

            // 4. Trigger the eruption of sound, particles, and damage at the target location.
            float DAMAGE = 15.0f;
            double ERUPTION_RADIUS = 1.5;
            double ERUPTION_HEIGHT = 6.0;
            final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);
            DamageSource damageSource = DamageSource.LIGHTNING_BOLT;

            level.playSound(null, groundCenter.x, groundCenter.y, groundCenter.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0f, 1.3f);

            for (int i = 0; i <= ERUPTION_HEIGHT * 2; i++) {
                Vec3 particlePos = groundCenter.add(0, (double)i * 0.5, 0);
                level.sendParticles(YELLOW_LIGHTNING, particlePos.x, particlePos.y, particlePos.z, 3, 0.2, 0.2, 0.2, 0);
            }

            FormUtils.dealVerticalColumnDamage(user, groundCenter, ERUPTION_RADIUS, ERUPTION_HEIGHT, DAMAGE, damageSource);
        }
    },
    THUNDER_6(BreathingStyles.THUNDER,
            "Sixth Form: Rumble and Flash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases a series of powerful long-ranged lightning attacks that strike his opponent from afar.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THUNDER_7(BreathingStyles.THUNDER,
            "Seventh Form: Honoikazuchi no Kami",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The personal creation of Zenitsu Agatsuma. The user dashes forward at blinding speeds and unleashes a singular forward slash at a low stance, which is seemingly accompanied by a yellow Japanese dragon shaped like a lightning bolt.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // MIST BREATHING
    // =================================================================================
    MIST_1(BreathingStyles.MIST,
            "First Form: Low Clouds, Distant Haze",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs a singular and powerful frontal thrust.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

            double range = 6.0;
            FormUtils.dealThrustDamage(user, range, 6.0f);

            Vec3 startPos = user.getEyePosition();
            Vec3 endPos = startPos.add(user.getLookAngle().scale(range));

            final DustParticleOptions denseMist = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

            FormUtils.spawnLineParticles(level, ParticleTypes.CLOUD, startPos, endPos, 20);
            FormUtils.spawnLineParticles(level, denseMist, startPos, endPos, 15);
        }
    },
    MIST_2(BreathingStyles.MIST,
            "Second Form: Eight-Layered Mist",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs eight slashes in quick succession.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MIST_3(BreathingStyles.MIST,
            "Third Form: Scattering Mist Splash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs a powerful and swift circular slash that can blow away incoming projectiles.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MIST_4(BreathingStyles.MIST,
            "Fourth Form: Shifting Flow Slash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user takes a low stance before dashing toward the target and slashing them.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MIST_5(BreathingStyles.MIST,
            "Fifth Form: Sea of Clouds and Haze",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user charges at their target at blistering speeds in a zig-zag motion while performing a barrage of continuous slashes in front and/or around them.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MIST_6(BreathingStyles.MIST,
            "Sixth Form: Lunar Dispersing Mist",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air and performs a multitude of slashes in a somersaulting motion, decimating dozens of targets at once.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MIST_7(BreathingStyles.MIST,
            "Seventh Form: Obscuring Clouds",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The personal creation of Muichiro Tokito. A movement-based technique designed to heavily disorientate the target, which is done so by fluctuating the user's tempo. When the user appears before the target, they move incredibly slow, but then accelerate to blistering speeds before getting hit, making it seem like they disappeared into a cloud of mist. When done repeatedly, it can disorientate the target to the point of making them think they are being covered in a thick layer of mist.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            final int RADIUS = 10; // A 20x20 block area of mist
            final int GRID_SPACING = 4;

            level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);

            // --- Grid-based spawning for mist clouds (Unchanged) ---
            for (int x = -RADIUS; x <= RADIUS; x += GRID_SPACING) {
                for (int z = -RADIUS; z <= RADIUS; z += GRID_SPACING) {
                    double spawnX = user.getX() + x + (level.random.nextDouble() * 2.0 - 1.0);
                    double spawnZ = user.getZ() + z + (level.random.nextDouble() * 2.0 - 1.0);
                    double spawnY = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int)spawnX, (int)spawnZ);
                    if (user.position().distanceToSqr(spawnX, spawnY, spawnZ) > (RADIUS+2)*(RADIUS+2)) continue;

                    BlindingMistCloudEntity cloud = ModEntityTypes.BLINDING_MIST_CLOUD.get().create(level);
                    if (cloud != null) {
                        cloud.setOwner(user);
                        cloud.setPos(spawnX, spawnY, spawnZ);
                        cloud.setRadius(4.0f);
                        cloud.setDuration(200);
                        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        level.addFreshEntity(cloud);
                    }
                }
            }

            // --- NEW: Smart Decoy Spawning Logic ---
            final int DECOY_COUNT = 4;
            final double MAX_DECOY_RADIUS = RADIUS * 0.9; // Don't let them spawn at the absolute edge.

            for (int i = 0; i < DECOY_COUNT; i++) {
                // 1. Calculate a base angle to divide the area into sectors (quadrants for 4 decoys).
                double angle = (2 * Math.PI / DECOY_COUNT) * i;
                // Add some randomness so they aren't on a perfect axis.
                angle += (level.random.nextDouble() - 0.5) * (Math.PI / 4);

                // 2. Use a Gaussian distribution for the distance from the center.
                // This makes them more likely to spawn closer to the player.
                double distance = Math.abs(level.random.nextGaussian() / 2.0) * MAX_DECOY_RADIUS;
                // We clamp the value to ensure they don't spawn right on top of the player or outside the radius.
                distance = net.minecraft.util.Mth.clamp(distance, 3.0, MAX_DECOY_RADIUS);

                // 3. Calculate the final X and Z coordinates using trigonometry.
                double decoyX = user.getX() + Math.cos(angle) * distance;
                double decoyZ = user.getZ() + Math.sin(angle) * distance;
                double decoyY = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int)decoyX, (int)decoyZ);

                MistCloneEntity decoy = ModEntityTypes.MIST_CLONE.get().create(level);

                if (decoy != null) {
                    if(user instanceof ServerPlayer player) {
                        decoy.setOwnerProfile(player.getGameProfile());
                    } else {
                        decoy.setIsNpcClone(true);
                    }

                    decoy.setItemSlot(EquipmentSlot.HEAD, user.getItemBySlot(EquipmentSlot.HEAD).copy());
                    decoy.setItemSlot(EquipmentSlot.CHEST, user.getItemBySlot(EquipmentSlot.CHEST).copy());
                    decoy.setItemSlot(EquipmentSlot.LEGS, user.getItemBySlot(EquipmentSlot.LEGS).copy());
                    decoy.setItemSlot(EquipmentSlot.FEET, user.getItemBySlot(EquipmentSlot.FEET).copy());
                    decoy.setItemSlot(EquipmentSlot.MAINHAND, user.getItemBySlot(EquipmentSlot.MAINHAND).copy());
                    decoy.setItemSlot(EquipmentSlot.OFFHAND, user.getItemBySlot(EquipmentSlot.OFFHAND).copy());

                    decoy.setPos(decoyX, decoyY, decoyZ);
                    decoy.setPersistenceRequired();
                    level.addFreshEntity(decoy);
                }
            }

            user.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
        }
    },

    // =================================================================================
    // SOUND BREATHING
    // =================================================================================
    SOUND_1(BreathingStyles.SOUND,
            "First Form: Roar",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user lifts their twin swords above their head and slams them down with great force, causing a loud sound resembling thunder as a result.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SOUND_4(BreathingStyles.SOUND,
            "Fourth Form: Constant Resounding Slashes",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user holds their twin swords apart and spins them rapidly around themselves, enabling the blades to destroy anything it makes contact with for powerful defensive capabilities around the user.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SOUND_5(BreathingStyles.SOUND,
            "Fifth Form: String Performance",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user holds the chain of their twin swords and rotates the blades at high speed while charging forward. The spin of the blades without changing grip allows for continual slashes while still being in motion.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // LOVE BREATHING
    // =================================================================================
    LOVE_1(BreathingStyles.LOVE,
            "First Form: Shivers of First Love",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes forward and performs a single slash that winds through the target, hitting multiple areas with one slice.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    LOVE_2(BreathingStyles.LOVE,
            "Second Form: Love Pangs",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user performs a singular, long-winding swing, akin to a whipping motion, that cuts the target at multiple angles.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    LOVE_3(BreathingStyles.LOVE,
            "Third Form: Catlove Shower",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air and performs multiple arched slashes in quick succession.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    LOVE_5(BreathingStyles.LOVE,
            "Fifth Form: Swaying Love, Wildclaw",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air before performing a somersault and releasing dozens of rapid, winding slashes from afar that essentially form a tornado of slashes.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    LOVE_6(BreathingStyles.LOVE,
            "Sixth Form: Cat-Legged Winds of Love",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user fully extends their sword to perform an arcing slash before retracting it and extending it once more, done in quick succession to form a layer of slashes.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // INSECT BREATHING
    // =================================================================================
    INSECT_1(BreathingStyles.INSECT,
            "Dance of the Butterfly: Caprice",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps into the air and charges towards the opponent, stinging them multiple times with their blade to inject poison in just seemingly one strike.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    INSECT_2(BreathingStyles.INSECT,
            "Dance of the Bee Sting: True Flutter",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes at their target at blinding speeds and uses the momentum of their speed to then strengthen a single thrust into their opponent and inject poison into them.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    INSECT_3(BreathingStyles.INSECT,
            "Dance of the Dragonfly: Compound Eye Hexagon",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "A six strike attack with which the user aims to strike and inject their target with multiple doses of poison at the target's weak spots (such as the neck, heart, torso, etc.) in order to kill them with a large dose of poison.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    INSECT_4(BreathingStyles.INSECT,
            "Dance of the Centipede: Hundred-Legged Zigzag",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "An attack with which the user uses their full speed to run a zigzag pattern that confuses their opponent in order to create an opening for them to directly stab their opponent's neck and inject poison to kill them, with the stab's force and power being further enhanced by the momentum of their speed. Doma noted that the sheer speed of this technique is strong enough to destroy an entire wooden bridge.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // STONE BREATHING
    // =================================================================================
    STONE_1(BreathingStyles.STONE,
            "First Form: Serpentinite Bipolar",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user throws both the flail and axe towards their intended target and manipulates the chain by rotating it thus causing the axe and flail to also rotate in order to drill and grind right through their target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    STONE_2(BreathingStyles.STONE,
            "Second Form: Upper Smash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user throws both their axe and spiked flail for a pincer move before smashing down on the chain to cause the flail to then rebound and smash into their target from afar.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    STONE_3(BreathingStyles.STONE,
            "Third Form: Stone Skin",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user rapidly twirls and swings the chain around them to unleash an omnidirectional flurry of attacks with both the flail and axe. This technique can also be used to block and counter enemy attacks from all directions at once.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    STONE_4(BreathingStyles.STONE,
            "Fourth Form: Volcanic Rock, Rapid Conquest",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user manipulates both the flail and axe weapons at both ends of the chain by swinging the chain on both sides in order to strike their opponents using a two pronged wide ranged attack.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    STONE_5(BreathingStyles.STONE,
            "Fifth Form: Arcs of Justice",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user utilizes a powerful barrage with their flail and axe, doing a great amount of damage. The axe is destined to hit first with the trajectory of the flail following from the opposite side and slamming it into the opponent.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // FLOWER BREATHING
    // =================================================================================
    FLOWER_2(BreathingStyles.FLOWER,
            "Second Form: Honorable Shadow Plum",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases several rotating sword slashes that deflects incoming physical attacks.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level){
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLOWER_4(BreathingStyles.FLOWER,
            "Fourth Form: Crimson Hanagoromo",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user unleashes single sword slash that curves and twists gracefully.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level){
            final float DAMAGE = 9.0f;
            final double RANGE = 4.5;

            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
            FormUtils.dealDirectionalDamage(user, RANGE, DAMAGE, 0.7);

            final DustParticleOptions pinkPetal = new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.8f), 1.0f);
            final DustParticleOptions crimsonPetal = new DustParticleOptions(new Vector3f(0.8f, 0.1f, 0.2f), 1.2f);

            FormUtils.spawnParticleArc(user, pinkPetal, 2.8, 140, 30, 0);
            FormUtils.spawnParticleArc(user, crimsonPetal, 2.5, 120, 25, 0);

            for (int i = 0; i < 5; i++) {
                level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                        user.getX() + (level.random.nextDouble() - 0.5) * 2.0,
                        user.getY() + 1.0,
                        user.getZ() + (level.random.nextDouble() - 0.5) * 2.0,
                        1, 0, 0, 0, 1);
            }
        }
    },
    FLOWER_5(BreathingStyles.FLOWER,
            "Fifth Form: Peonies of Futility",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Fifth Form: Peonies of Futility desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLOWER_6(BreathingStyles.FLOWER,
            "Sixth Form: Whirling Peach",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "A technique used after or during evasion, the user spins around moving with their body weight to deliver an attack.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FLOWER_7(BreathingStyles.FLOWER,
            "Final Form: Equinoctial Vermilion Eye",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "A focusing technique that raises the user's kinetic vision to its maximum, while using this technique the user perceives the world as if it were in slow motion. Usage of the technique, however, is highly dangerous, as the tremendous strain on the eyes could cause blood vessels to rupture and lead to partial or complete blindness. This technique is still usable even if the user only has one fully functional eye.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // SERPENT BREATHING
    // =================================================================================
    SERPENT_1(BreathingStyles.SERPENT,
            "First Form: Winding Serpent Slash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user releases a singular frontal horizontal slash aimed at their target in a winding motion.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            final float DAMAGE = 10.0f;
            final double RANGE = 6.0;

            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.5f);
            FormUtils.dealDirectionalDamage(user, RANGE, DAMAGE, 0.7);

            final DustParticleOptions serpentScale = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);
            final int segments = 25;

            Vec3 eyePos = user.getEyePosition();
            Vec3 lookDir = user.getLookAngle();
            Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

            for (int i = 0; i <= segments; i++) {
                double progress = (double) i / segments;
                Vec3 pathPoint = eyePos.add(lookDir.scale(progress * RANGE));
                double wind_amplitude = 0.8; // How wide the winding motion is
                double wind_frequency = 3.0; // How many winds there are
                double windOffset = Math.sin(progress * Math.PI * wind_frequency) * wind_amplitude;

                Vec3 particlePos1 = pathPoint.add(sideDir.scale(windOffset)).add(0, -windOffset * 0.5, 0);
                Vec3 particlePos2 = pathPoint.add(sideDir.scale(-windOffset)).add(0, windOffset * 0.5, 0);

                level.sendParticles(serpentScale, particlePos1.x, particlePos1.y, particlePos1.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, particlePos2.x, particlePos2.y, particlePos2.z, 1, 0, 0, 0, 0);
            }
        }
    },
    SERPENT_2(BreathingStyles.SERPENT,
            "Second Form: Venom Fangs of the Narrow Head",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user dashes behind their opponent at blinding speed and aims to swiftly slice off their head from behind.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SERPENT_3(BreathingStyles.SERPENT,
            "Third Form: Coil Choke",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user circles around their opponent while using their sword to slice them from all directions.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SERPENT_4(BreathingStyles.SERPENT,
            "Fourth Form: Twin-Headed Reptile",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user leaps forward and performs a horizontal slash that slices through the target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            final float DAMAGE = 16.0f;
            final double RANGE = 5.0;
            final float LEAP_STRENGTH = 0.6f;
            final float FORWARD_STRENGTH = 0.8f;

            Vec3 lookVec = user.getLookAngle();
            Vec3 leapVec = new Vec3(lookVec.x * FORWARD_STRENGTH, LEAP_STRENGTH, lookVec.z * FORWARD_STRENGTH);
            user.setDeltaMovement(user.getDeltaMovement().add(leapVec));
            user.hurtMarked = true;

            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            FormUtils.dealDirectionalDamage(user, RANGE, DAMAGE, 0.7);

            final DustParticleOptions serpentScale = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);
            FormUtils.spawnParticleArc(user, serpentScale, 3.0, 140, 25);
            FormUtils.spawnParticleArc(user, ParticleTypes.CRIT, 3.2, 140, 20);
        }
    },
    SERPENT_5(BreathingStyles.SERPENT,
            "Fifth Form: Slithering Serpent",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user charges forth in a twisting and winding motion, curving their sword in multiple directions to strike multiple targets at once. This technique is capable of decapitating multiple enemies at once.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    // =================================================================================
    // Moon Breathing
    // =================================================================================
    MOON_1(BreathingStyles.MOON,
            "First Form: Dark Moon, Evening Palace",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "First Form: Dark Moon, Evening Palace desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 2.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);

            float damage = 22.0f;
            double range = 6.0;
            double angleThreshold = 0.0;
            FormUtils.dealDirectionalDamage(user, range, damage, angleThreshold);

            final DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
            final ParticleOptions silver = ParticleTypes.END_ROD;

            FormUtils.spawnParticleArc(user, purple, 4.0, 160, 50, 0);
            FormUtils.spawnParticleArc(user, silver, 4.0, 160, 20, 0);
        }
    },
    MOON_2(BreathingStyles.MOON,
            "Second Form: Pearl Flower Moongazing",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Second Form: Pearl Flower Moongazing desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_3(BreathingStyles.MOON,
            "Third Form: Loathsome Moon, Chains",
            InputType.PRESS,
            1,
            0,
            50,
            100,
            "Third Form: Loathsome Moon, Chains desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SKELETON_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.9f);

            float damage = 25.0f;
            double range = 8.0;
            FormUtils.dealDirectionalDamage(user, range, damage, 0.4);

            final DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.2f);

            Vec3 upperArcOrigin = user.getEyePosition().add(0, 0.5, 0);
            FormUtils.spawnParticleArcInWorld(level, upperArcOrigin, 5.0, 160, 60, user.getYRot(), 0, purple);

            Vec3 lowerArcOrigin = user.position().add(0, user.getBbHeight() * 0.5, 0);
            FormUtils.spawnParticleArcInWorld(level, lowerArcOrigin, 5.0, 160, 60, user.getYRot(), 0, purple);
        }
    },
    MOON_5(BreathingStyles.MOON,
            "Fifth Form: Moon Spirit Calamitous Eddy",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Fifth Form: Moon Spirit Calamitous Eddy desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_6(BreathingStyles.MOON,
            "Sixth Form: Perpetual Night, Lonely Moon - Incessant",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Sixth Form: Perpetual Night, Lonely Moon - Incessant desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_7(BreathingStyles.MOON,
            "Seventh Form: Mirror of Misfortune, Moonlit",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Seventh Form: Mirror of Misfortune, Moonlit desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_8(BreathingStyles.MOON,
    "Eighth Form: Moon-Dragon Ringtail",
    InputType.PRESS,
    0,
    1,
    50,
    100,
    "Eighth Form: Moon-Dragon Ringtail desc.",
    false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_9(BreathingStyles.MOON,
            "Ninth Form: Waning Moonswaths",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Ninth Form: Waning Moonswaths desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_10(BreathingStyles.MOON,
            "Tenth Form: Drilling Slashes, Moon Through Bamboo Leaves",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Tenth Form: Drilling Slashes, Moon Through Bamboo Leaves desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_14(BreathingStyles.MOON,
            "Fourteenth Form: Catastrophe, Tenman Crescent Moon",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Fourteenth Form: Catastrophe, Tenman Crescent Moon desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    MOON_16(BreathingStyles.MOON,
            "Sixteenth Form: Moonbow, Half Moon",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Sixteenth Form: Moonbow, Half Moon desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    // =================================================================================
    // Beast Breathing
    // =================================================================================
    BEAST_1(BreathingStyles.BEAST,
            "First Fang: Pierce",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "First Fang: Pierce desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.HOGLIN_ATTACK, SoundSource.PLAYERS, 0.8f, 1.2f);

            float damage = 18.0f;
            double range = 4.5;
            FormUtils.dealThrustDamage(user, range, damage);

            Vec3 startPos = user.getEyePosition();
            Vec3 lookVec = user.getLookAngle();
            Vec3 sideVec = lookVec.cross(new Vec3(0, 1, 0)).normalize().scale(0.2);

            // Calculate start/end points for two parallel lines
            Vec3 leftBladeStart = startPos.add(sideVec);
            Vec3 rightBladeStart = startPos.subtract(sideVec);
            Vec3 leftBladeEnd = leftBladeStart.add(lookVec.scale(range));
            Vec3 rightBladeEnd = rightBladeStart.add(lookVec.scale(range));

            // Spawn the particle trails
            FormUtils.spawnLineParticles(level, ParticleTypes.CRIT, leftBladeStart, leftBladeEnd, 15);
            FormUtils.spawnLineParticles(level, ParticleTypes.CRIT, rightBladeStart, rightBladeEnd, 15);
        }
    },
    BEAST_2(BreathingStyles.BEAST,
            "Second Fang: Slice",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Second Fang: Slice desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.9f);
            level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 0.7f, 1.1f);

            float damage = 16.0f;
            double range = 5.0;
            double angleThreshold = 0.6;
            FormUtils.dealDirectionalDamage(user, range, damage, angleThreshold);

            // top-left to bottom-right
            FormUtils.spawnParticleArc(user, ParticleTypes.CRIT, 3.0, 120, 25, 45.0f);
            // top-right to bottom-left
            FormUtils.spawnParticleArc(user, ParticleTypes.CRIT, 3.0, 120, 25, -45.0f);

            FormUtils.spawnParticleArc(user, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()), 2.5, 120, 10);
        }
    },
    // Add this to your BreathingForms.java enum
    BEAST_3(BreathingStyles.BEAST,
            "Third Fang: Devour",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Third Fang: Devour desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.performGenericAttack(user, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8f, 1.3f);

            float damage = 20.0f;
            double range = 4.0;
            double angleThreshold = 0.9;
            FormUtils.dealDirectionalDamage(user, range, damage, angleThreshold);

            Vec3 eyePos = user.getEyePosition();
            FormUtils.spawnParticleArc(user, ParticleTypes.CRIT, 2.5, 120, 20, 90.0f);
            FormUtils.spawnParticleArcInWorld(level, eyePos.add(0, -0.3, 0), 2.5, 120, 20, user.getYRot(), user.getXRot(), ParticleTypes.CRIT);
        }
    },
    BEAST_4(BreathingStyles.BEAST,
            "Fourth Fang: Slice 'n' Dice",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Fourth Fang: Slice 'n' Dice desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_5(BreathingStyles.BEAST,
            "Fifth Fang: Crazy Cutting",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Fifth Fang: Crazy Cutting desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (user.isOnGround()) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
                user.sendSystemMessage(Component.literal("You must be in the air to use this form!").withStyle(ChatFormatting.RED));
                return;
            }

            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_6(BreathingStyles.BEAST,
            "Sixth Fang: Palisade Bite",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Sixth Fang: Palisade Bite desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_7(BreathingStyles.BEAST,
            "Seventh Form: Spatial Awareness",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Seventh Form: Spatial Awareness desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_8(BreathingStyles.BEAST,
            "Eighth Form: Explosive Rush",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Eighth Form: Explosive Rush desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_9(BreathingStyles.BEAST,
            "Ninth Fang: Extending Bendy Slash",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Ninth Fang: Extending Bendy Slash desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_10(BreathingStyles.BEAST,
            "Tenth Fang: Whirling Fangs",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Tenth Fang: Whirling Fangs desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    BEAST_11(BreathingStyles.BEAST,
            "Eleventh Fang: Sudden Throwing Strike",
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "Eleventh Fang: Sudden Throwing Strike desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.2f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);

            Vec3 lookVec = user.getLookAngle();
            Vec3 sideVec = lookVec.cross(new Vec3(0, 1, 0)).normalize().scale(0.3);

            ThrownNichirinEntity blade1 = new ThrownNichirinEntity(level, user);
            blade1.shoot(lookVec.x - sideVec.x, lookVec.y, lookVec.z - sideVec.z, 2.0f, 2.0f);
            level.addFreshEntity(blade1);

            ThrownNichirinEntity blade2 = new ThrownNichirinEntity(level, user);
            blade2.shoot(lookVec.x + sideVec.x, lookVec.y, lookVec.z + sideVec.z, 2.0f, 2.0f);
            level.addFreshEntity(blade2);
        }
    },
    // =================================================================================
    // Sun Breathing (Hinokami Kagura)
    // =================================================================================
    // In your BreathingForms.java enum, add this new form:
    SUN_1(BreathingStyles.SUN,
            "Dance",
            InputType.PRESS,
            0,
            100,
            50,
            100,
            "The most fundamental slash, from which all other techniques are born. A single, perfect vertical slash that shatters all defenses.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 1.0f);

            double range = 6.0;
            AABB attackAABB = user.getBoundingBox().expandTowards(user.getLookAngle().scale(range)).inflate(1.5, 1.5, 1.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackAABB, e ->
                    e != user && e.isAlive() && user.hasLineOfSight(e));

            float baseDamage = 35.0f;
            float parryShatterBonus = 20.0f;

            DamageSource sunSource = DamageSource.playerAttack((Player)user).setMagic();

            for (LivingEntity target : targets) {
                FormUtils.ActiveForm activeForm = FormUtils.ActiveFormManager.getActiveForm(target);
                if (activeForm instanceof FormUtils.ParryForm) {
                    target.hurt(sunSource, baseDamage + parryShatterBonus);
                    FormUtils.ActiveFormManager.stop(target);

                    level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getEyeY(), target.getZ(), 30, 0.5, 0.5, 0.5, 0.0);
                } else {
                    target.hurt(sunSource, baseDamage);
                }

                if (target.isBlocking()) {
                    if (target instanceof Player p) {
                        p.getCooldowns().addCooldown(p.getUseItem().getItem(), 100);
                    }
                    target.stopUsingItem();
                }
            }

            final DustParticleOptions redHot = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);
            final ParticleOptions embers = ParticleTypes.LAVA;

            FormUtils.spawnParticleArc(user, redHot, 3.5, 160, 60, 90.0f);
            FormUtils.spawnParticleArc(user, embers, 3.5, 160, 30, 90.0f);
        }
    },
    SUN_2(BreathingStyles.SUN,
            "Clear Blue Sky",
            InputType.PRESS,
            0,
            120,
            50,
            100,
            "The user spins their body, releasing a 360° slash that purifies the battlefield.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_3(BreathingStyles.SUN,
            "Raging Sun",
            InputType.PRESS,
            0,
            130,
            0,
            0,
            "Unleash two consecutive 360-degree slashes that incinerate incoming attacks.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_4(BreathingStyles.SUN,
            "Burning Bones, Summer Sun",
            InputType.PRESS,
            1,
            140,
            50,
            400,
            "A spiraling slash that creates a defensive vortex to intercept frontal attacks.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_5(BreathingStyles.SUN,
            "Setting Sun Transformation",
            InputType.PRESS,
            0,
            130,
            0,
            0,
            "A graceful backflip into the air to unleash a devastating horizontal slash below.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_6(BreathingStyles.SUN,
            "Solar Heat Haze",
            InputType.PRESS,
            0,
            120,
            0,
            0,
            "Dash forward, masking the true length of your blade in a heat haze to land a surprising long-range strike.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_7(BreathingStyles.SUN,
            "Beneficent Radiance",
            InputType.PRESS,
            0,
            140,
            50,
            100,
            "Spiral into the air while performing a swirling upward slash to strike the target.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_8(BreathingStyles.SUN,
            "Sunflower Thrust",
            InputType.HOLD,
            30,
            150,
            50,
            100,
            "A devastating thrust that blooms with solar energy upon impact.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_9(BreathingStyles.SUN,
            "Sun Halo Dragon Head Dance",
            IForm.InputType.PRESS,
            0,
            150,
            50,
            100,
            "Unleash a continuous, flowing attack in the form of a solar dragon.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.SunHaloDragonForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    SUN_10(BreathingStyles.SUN,
            "Fire Wheel",
            InputType.PRESS,
            0,
            150,
            50,
            100,
            "Fire Wheel desc.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_11(BreathingStyles.SUN,
            "Fake Rainbow",
            IForm.InputType.PRESS,
            0,
            80,
            50,
            100,
            "Perform a high-speed dash that creates afterimages to confuse enemies.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_12(BreathingStyles.SUN,
            "Flame Dance",
            IForm.InputType.PRESS,
            0,
            150,
            50,
            100,
            "A two-strike combo starting with a vertical slash, followed by a horizontal one.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SUN_13(BreathingStyles.SUN,
            "Thirteenth Form",
            InputType.HOLD,
            40,
            200,
            50,
            100,
            "Continuously perform all twelve forms in rapid succession.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    ;


    // =================================================================================
    // Enum Boilerplate
    // =================================================================================
    private final BreathingStyles style;
    private final String displayName;
    private final IForm.InputType inputType;
    private final int chargeTicks;
    private final int breathCost;
    private final int styleMasteryToUnlock;
    private final int masteryGoal;
    private final String description;
    private final Boolean isBDA;

    BreathingForms(BreathingStyles style, String displayName, IForm.InputType inputType, int chargeTicks, int breathCost, int styleMasteryToUnlock, int masteryGoal, String description, Boolean isBDA) {
        this.style = style;
        this.displayName = displayName;
        this.inputType = inputType;
        this.chargeTicks = chargeTicks;
        this.breathCost = breathCost;
        this.styleMasteryToUnlock = styleMasteryToUnlock;
        this.masteryGoal = masteryGoal;
        this.description = description;
        this.isBDA = isBDA;
    }

    public BreathingStyles getStyle() { return style; }
    public String getDisplayName() { return displayName; }
    public IForm.InputType getInputType() { return inputType; }
    public int getChargeTicks() { return chargeTicks; }
    public int getBreathCost() { return breathCost; }
    public int getStyleMasteryToUnlock() { return styleMasteryToUnlock; }
    public int getMasteryGoal() { return masteryGoal; }
    public String getDescription() { return description; }
    public boolean IsBDA() { return isBDA; }

    public abstract void performAction(LivingEntity user, ServerLevel level);
}