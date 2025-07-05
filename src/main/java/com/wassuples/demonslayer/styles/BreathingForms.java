package com.wassuples.demonslayer.styles;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BlindingMistCloudEntity;
import com.wassuples.demonslayer.entity.custom.MistCloneEntity;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.text.Normalizer;


/**
 * Enum defining all breathing forms, their properties, and their execution logic.
 * The performAction method is the entry point for activating a form.
 */
public enum BreathingForms {

    // =================================================================================
    // WATER BREATHING
    // =================================================================================

    WATER_1(
            BreathingStyles.WATER,
            "First Form: Water Surface Slash",
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user changes the grip on their sword and decapitates the opponent in a single flowing strike that causes little to no pain. It is a “sword strike of kindness” used when the enemy willingly surrenders.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Do me later...
        }
    },
    WATER_6(BreathingStyles.WATER,
            "Sixth Form: Whirlpool",
            InputType.PRESS,
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
            InputType.HOLD,
            40,
            1,
            50,
            100,
            "The user performs a precise, fast and accurate thrust with their blade.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            FormUtils.performGenericAttack(user, SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
            FormUtils.dealThrustDamage(user, 8.0, 20.0f);
            FormUtils.spawnLineParticles(level, ParticleTypes.SPLASH, user.getEyePosition(), user.getEyePosition().add(user.getLookAngle().scale(8.0)), 30);
        }
    },
    WATER_8(BreathingStyles.WATER,
            "Eighth Form: Waterfall Basin",
            InputType.PRESS,
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
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user changes their footwork in a way that minimizes the landing time and surface needed when landing, allowing the user to move without limits. Ideal when fighting in a place with no solid foothold.\n",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Do me later...
            }
    },
    WATER_10(BreathingStyles.WATER,
            "Tenth Form: Constant Flux",
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.HOLD,
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
            InputType.PRESS,
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
            InputType.PRESS,
            0,
            1,
            50,
            100,
            "The user lifts the sword upwards towards the right, above their head and unleashes four vertical slashes at once down on the enemy resembling claws.",
            false) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            // Doesn't have ActiveForm class, because it is very simple.
            FormUtils.performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.4f);
            user.setDeltaMovement(user.getDeltaMovement().add(0, 0.6, 0));
            user.hurtMarked = true;

            final DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 1.0f);
            FormUtils.spawnParticleArc(user, greenDust, 3, 120, 40);
            FormUtils.dealClawDamage(user, 8.0f, 6.0, 4, 90.0, 0.7f);
        }
    },
    WIND_3(BreathingStyles.WIND,
            "Third Form: Clear Storm Wind Tree",
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
            InputType.PRESS,
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
    // BDA, Rui (Thread Manipulation)
    // =================================================================================
    THREADS_1(BreathingStyles.THREADS,
            "Cutting Thread Cage",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cutting Thread Cage desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    THREADS_2(BreathingStyles.THREADS,
            "Murderous Eye Basket",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Murderous Eye Basket desc.",
            true) {
      @Override
      public void performAction(LivingEntity user, ServerLevel level) {
          FormUtils.ActiveFormManager.start(user, this);
      }
    },
    THREADS_3(BreathingStyles.THREADS,
            "Cutting Thread Rotation",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cutting Thread Rotation desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Enmu (Sleep Manip.)
    // =================================================================================
    SLEEP_1(BreathingStyles.SLEEP,
            "Whispers of Forced Unconscious Hypnosis",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Whispers of Forced Unconscious Hypnosis desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    SLEEP_2(BreathingStyles.SLEEP,
            "Eyes of Forced Unconscious Sleep",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eyes of Forced Unconscious Sleep desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Daki (Obi Sash Manip.)
    // =================================================================================
    SASH_1(BreathingStyles.SASH,
            "Eight-Layered Obi Slash",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eight-Layered Obi Slash desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Gyutaro (Haemokinesis)
    // =================================================================================
    HAEMO_1(BreathingStyles.HAEMO,
            "Flying Blood Sickles",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Flying Blood Sickles desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    HAEMO_2(BreathingStyles.HAEMO,
            "Rampant Arc Rampage",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rampant Arc Rampage desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    HAEMO_3(BreathingStyles.HAEMO,
            "Rotating Circular Slashes",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rotating Circular Slashes desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Gyokko (Fish Scale Attacks)
    // =================================================================================
    FISH_1(BreathingStyles.FISH,
            "Thousand Needle Fish Kill",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Thousand Needle Fish Kill desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FISH_2(BreathingStyles.FISH,
            "Water Prison Pot",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Water Prison Pot desc.",
            true){
        @Override
        public void performAction(LivingEntity user,  ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FISH_3(BreathingStyles.FISH,
            "Octopus Vase Hell",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Octopus Vase Hell desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FISH_4(BreathingStyles.FISH,
            "Ten Thousand Gliding Slime-Fish",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Ten Thousand Gliding Slime-Fish desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    FISH_5(BreathingStyles.FISH,
            "Killer Fish Scales",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Killer Fish Scales desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Hantengu (Emotion Manifestation)
    // =================================================================================
    EMOTION_1(BreathingStyles.EMOTION,
            "Weeping Spears",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Weeping Spears desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    EMOTION_2(BreathingStyles.EMOTION,
            "Crazed Cry of Thunderdeath",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Crazed Cry of Thunderdeath desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    EMOTION_3(BreathingStyles.EMOTION,
            "Countless Striking Trees",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Countless Striking Trees desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.CountlessStrikingTreesForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    EMOTION_4(BreathingStyles.EMOTION,
            "Compressed Sound Waves",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Compressed Sound Waves desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Akaza (Destructive Death)
    // =================================================================================
    DESTRUCTIVE_DEATH_1(BreathingStyles.DESTRUCTIVE_DEATH,
            "Compass Needle",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Compass Needle desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.CompassNeedleForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    DESTRUCTIVE_DEATH_2(BreathingStyles.DESTRUCTIVE_DEATH,
            "Air Type",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Air Type desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_3(BreathingStyles.DESTRUCTIVE_DEATH,
            "Disorder",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Disorder desc.",
    true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.DisorderForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
        }
    },
    DESTRUCTIVE_DEATH_4(BreathingStyles.DESTRUCTIVE_DEATH,
            "Annihilation Type",
            InputType.HOLD,
            80,
            1,
            50,
            100,
            "Annihilation Type desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_5(BreathingStyles.DESTRUCTIVE_DEATH,
            "Leg Type: Crown Splitter",
            InputType.PRESS,
            80,
            0,
            50,
            100,
            "Leg Type: Crown Splitter desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_6(BreathingStyles.DESTRUCTIVE_DEATH,
            "Leg Type: Explosive Flurry",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Leg Type: Explosive Flurry desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_7(BreathingStyles.DESTRUCTIVE_DEATH,
            "Leg Type: Flying Planet Thousand Wheels",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Leg Type: Flying Planet Thousand Wheels desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_8(BreathingStyles.DESTRUCTIVE_DEATH,
            "Eight-Layered Demon Core",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eight-Layered Demon Core desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_9(BreathingStyles.DESTRUCTIVE_DEATH,
            "Crushing Type: Ten Thousand Leaves Flashing Willow",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Crushing Type: Ten Thousand Leaves Flashing Willow desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    DESTRUCTIVE_DEATH_10(BreathingStyles.DESTRUCTIVE_DEATH,
            "Final Form: Blue Silver Chaotic Afterglow",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Final Form: Blue Silver Chaotic Afterglow desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },

    // =================================================================================
    // BDA, Doma (Cryokinesis)
    // =================================================================================
    CRYO_1(BreathingStyles.CRYO,
            "Frozen Lotus",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Frozen Lotus desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_2(BreathingStyles.CRYO,
            "Barren Hanging Garden",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Barren Hanging Garden desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_3(BreathingStyles.CRYO,
            "Freezing Clouds",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Freezing Clouds desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_4(BreathingStyles.CRYO,
            "Lotus Vines",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Lotus Vines desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_5(BreathingStyles.CRYO,
            "Cold White Princesses",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cold White Princesses desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_6(BreathingStyles.CRYO,
            "Wintry Icicles",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Wintry Icicles desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_7(BreathingStyles.CRYO,
            "Scattering Lotuses",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Scattering Lotuses desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_8(BreathingStyles.CRYO,
            "Crystalline Divine Child",
            InputType.HOLD,
            20*6,
            0,
            50,
            100,
            "Crystalline Divine Child desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_9(BreathingStyles.CRYO,
            "Rime — Water Lily Bodhisattva",
            InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rime — Water Lily Bodhisattva desc.",
            true) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    ;


    // =================================================================================
    // Enum Boilerplate
    // =================================================================================

    public enum InputType { PRESS, HOLD }

    private final BreathingStyles style;
    private final String displayName;
    private final InputType inputType;
    private final int chargeTicks;
    private final int breathCost;
    private final int styleMasteryToUnlock;
    private final int masteryGoal;
    private final String description;
    private final Boolean isBDA;

    BreathingForms(BreathingStyles style, String displayName, InputType inputType, int chargeTicks, int breathCost, int styleMasteryToUnlock, int masteryGoal, String description, Boolean isBDA) {
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
    public InputType getInputType() { return inputType; }
    public int getChargeTicks() { return chargeTicks; }
    public int getBreathCost() { return breathCost; }
    public int getStyleMasteryToUnlock() { return styleMasteryToUnlock; }
    public int getMasteryGoal() { return masteryGoal; }
    public String getDescription() { return description; }
    public boolean IsBDA() { return isBDA; }

    public abstract void performAction(LivingEntity user, ServerLevel level);
}