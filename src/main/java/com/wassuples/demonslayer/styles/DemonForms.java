package com.wassuples.demonslayer.styles;

import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import com.wassuples.demonslayer.entity.custom.OctopusPotEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.demon.akaza.CompassNeedleForm;
import com.wassuples.demonslayer.forms.demon.akaza.DisorderForm;
import com.wassuples.demonslayer.forms.demon.hantengu.CountlessStrikingTreesForm;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Enumerates all Demon / Blood Demon Art forms available to demon-path players.
 *
 * <p>Each constant belongs to a specific {@link BloodDemonArts} style and implements the
 * {@link IForm} contract. Forms are executed via {@link com.wassuples.demonslayer.forms.ActiveFormManager}
 * and consume hunger rather than breath.
 *
 * <h2>AI usage</h2>
 * <p>Each form overrides {@link IForm#getUsagePriority(CombatContext)} to let NPC demons
 * (e.g. {@link com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity}) intelligently choose
 * which form to use based on distance, health, and {@link com.wassuples.demonslayer.nemesis.ai.Behavior}.
 *
 * @see BloodDemonArts
 * @see IForm
 * @see com.wassuples.demonslayer.demon.PlayerDemon#cycleDemonForm()
 */

/**
 * Demon Forms.
 */
public enum DemonForms implements IForm {
    THREADS_1(BloodDemonArts.THREADS,
            "Cutting Thread Cage",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cutting Thread Cage desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A mid-range area control/trap ability.
            int priority = 0;
            boolean isMidRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 256;
            boolean isUserHealthy = context.userHealthPercent() > 0.4f;

            if (isMidRange && isUserHealthy) {
                priority = 9; // Base priority for setting up a trap.
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STRATEGIST, TRICKSTER -> priority += 4; // Loves traps.
                case SKIRMISHER, CALM -> priority += 2; // Good for controlling space.
                case AGGRESSIVE, BERSERKER -> priority -= 5; // Prefers direct damage to set up.
            }
            return Math.max(0, priority);
        }
    },
    THREADS_2(BloodDemonArts.THREADS,
            "Murderous Eye Basket",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Murderous Eye Basket desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A powerful, last-resort ultimate trap. Used when desperate.
            int priority = 0;
            boolean isUserDesperate = context.userHealthPercent() <= 0.35f;
            boolean isMidToLongRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 400;
            boolean isTargetWorthTheEffort = context.targetHealthPercent() > 0.2f;

            if (isUserDesperate && isMidToLongRange && isTargetWorthTheEffort) {
                priority = 100; // Base priority for a desperation ultimate.
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case BERSERKER -> { // More willing to use it even if not completely desperate.
                    if (context.userHealthPercent() <= 0.5f && isMidToLongRange) priority = 100;
                }
                case COWARDLY, STRATEGIST -> { if (priority > 0) priority += 10; } // A perfect ultimate for them.
            }
            return priority;
        }
    },
    THREADS_3(BloodDemonArts.THREADS,
            "Cutting Thread Rotation",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cutting Thread Rotation desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A standard, reliable ranged projectile.
            int priority = 0;
            boolean isMidRange = context.distanceSqToTarget() > 9 && context.distanceSqToTarget() <= 400;

            if (isMidRange) {
                priority = 7;
                if (context.userHealthPercent() > context.targetHealthPercent()) {
                    priority += 2;
                }
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER -> priority += 5; // Bread-and-butter move for a kiter.
                case COWARDLY, CALM -> priority += 3; // A safe way to deal damage.
                case BERSERKER, DUELIST -> priority -= 4; // Wants to be in melee.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Enmu (Sleep Manip.)
    // =================================================================================
    SLEEP_1(BloodDemonArts.SLEEP,
            "Whispers of Forced Unconscious Hypnosis",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Whispers of Forced Unconscious Hypnosis desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A powerful single-target crowd control opener.
            int priority = 0;
            boolean targetIsAwake = !context.target().hasEffect(ModEffects.FORCED_SLEEP.get());
            boolean isMidRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 256;
            boolean isUserHealthy = context.userHealthPercent() > 0.5f;

            if (targetIsAwake && isMidRange && isUserHealthy) {
                priority = 10;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST -> priority += 10; // The perfect tool for a sadist.
                case STRATEGIST, TRICKSTER, CALM -> priority += 5; // Excellent setup move.
                case BERSERKER, AGGRESSIVE -> priority -= 8; // Prefers damage over CC.
            }
            return Math.max(0, priority);
        }
    },
    SLEEP_2(BloodDemonArts.SLEEP,
            "Eyes of Forced Unconscious Sleep",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eyes of Forced Unconscious Sleep desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A stationary, wide-area crowd control move for multiple targets.
            int priority = 0;
            List<Player> nearbyPlayers = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(30.0), p -> !p.isCreative() && !p.isSpectator() && p.isAlive());
            boolean isCrowdControlNeeded = nearbyPlayers.size() > 1;
            boolean isAtSafeDistance = context.distanceSqToTarget() > 100;
            boolean isUserHealthy = context.userHealthPercent() > 0.7f;

            if (isCrowdControlNeeded && isAtSafeDistance && isUserHealthy) {
                priority = 20;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST, STRATEGIST -> priority += 5;
                case BERSERKER, DUELIST, SKIRMISHER, COWARDLY -> priority = 0; // These types hate standing still in a vulnerable state.
            }
            return priority;
        }
    },

    // =================================================================================
    // BDA, Daki (Obi Sash Manip.)
    // =================================================================================
    SASH_1(BloodDemonArts.SASH,
            "Eight-Layered Obi Slash",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eight-Layered Obi Slash desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A high-damage, channeled, ranged attack. User is vulnerable.
            int priority = 0;
            boolean isMidRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 144;
            boolean isUserHealthyEnough = context.userHealthPercent() > 0.25f;

            // Berserkers ignore the danger of channeling at low health.
            if (context.behavior() == Behavior.BERSERKER) {
                isUserHealthyEnough = true;
            }

            if (isMidRange && isUserHealthyEnough) {
                priority = 8;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER, AGGRESSIVE -> priority += 3;
                case BERSERKER -> priority += 5; // Loves high-risk, high-reward.
                case COWARDLY, CALM -> priority -= 4; // Too risky and stationary.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Gyutaro (Haemokinesis)
    // =================================================================================
    HAEMO_1(BloodDemonArts.HAEMO,
            "Flying Blood Sickles",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Flying Blood Sickles desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A relentless, homing projectile barrage for sustained pressure.
            int priority = 0;
            boolean isOptimalRange = context.distanceSqToTarget() > 25 && context.distanceSqToTarget() <= 625;

            if (isOptimalRange) {
                priority = 8;
                if (context.userHealthPercent() > context.targetHealthPercent()) priority++;
                if (context.targetHealthPercent() < 0.4f) priority += 2;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER -> priority += 5; // The perfect kiting tool.
                case AGGRESSIVE, BERSERKER -> priority += 3; // Relentless pressure.
                case COWARDLY -> priority += 3; // Safe homing damage.
            }
            return priority;
        }
    },
    HAEMO_2(BloodDemonArts.HAEMO,
            "Rampant Arc Rampage",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rampant Arc Rampage desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A high-risk, high-reward parry/counter for imminent melee attacks.
            int priority = 0;
            boolean isCloseQuarters = context.distanceSqToTarget() <= 16;

            if (isCloseQuarters) {
                priority = 4 + (int) ((1.0f - context.userHealthPercent()) * 10);
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STALWART -> priority += 5; // A perfect defensive tool.
                case DUELIST, CALM -> priority += 3; // A smart, tactical choice.
                case COWARDLY -> priority += 4; // A panic defense button.
                case BERSERKER, AGGRESSIVE -> priority -= 6; // Why defend when you can attack?
            }
            return Math.max(0, priority);
        }
    },
    HAEMO_3(BloodDemonArts.HAEMO,
            "Rotating Circular Slashes",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rotating Circular Slashes desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A large, channeled AOE to control an area or repel enemies.
            int priority = 0;
            boolean isCrowdControlNeeded = !context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(16.0), p -> p != context.target() && p.isAlive()).isEmpty();
            boolean isTargetAtMidRange = context.distanceSqToTarget() > 25 && context.distanceSqToTarget() <= 196;
            boolean isUserHealthyEnough = context.userHealthPercent() > 0.35f;

            if (context.behavior() == Behavior.BERSERKER) isUserHealthyEnough = true;

            if (isUserHealthyEnough) {
                if (isCrowdControlNeeded) priority = 12;
                else if (isTargetAtMidRange) priority = 8;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STALWART -> priority += 3; // Good for area control.
                case DUELIST -> {
                    if(isCrowdControlNeeded) {
                        priority = 1;
                    } else {
                        priority -= 3;
                    }}
                case COWARDLY, SKIRMISHER -> priority -= 4; // Too stationary.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Gyokko (Fish Scale Attacks)
    // =================================================================================
    FISH_1(BloodDemonArts.FISH,
            "Thousand Needle Fish Kill",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Thousand Needle Fish Kill desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Summons a group of aggressive minions.
            int priority = 0;
            if (context.distanceSqToTarget() > 9) {
                if (context.targetHealthPercent() >= 0.9f) priority = 9;
                else if (context.userHealthPercent() < 0.6f) priority = 8;
                else priority = 5;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case OVERLORD -> priority += 10; // This is their entire purpose.
                case TRICKSTER, STRATEGIST, COWARDLY -> priority += 4; // Summons create chaos and distractions.
                case DUELIST, BERSERKER -> priority -= 6; // Fights their own battles.
            }
            return Math.max(0, priority);
        }
    },
    FISH_2(BloodDemonArts.FISH,
            "Water Prison Pot",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Water Prison Pot desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A long-range, single-target hard crowd control.
            int priority = 0;
            MobEffectInstance slowEffect = context.target().getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            boolean isTargetAlreadyTrapped = slowEffect != null && slowEffect.getAmplifier() > 100;
            boolean isAtLongRange = context.distanceSqToTarget() > 49 && context.distanceSqToTarget() <= 625;

            if (!isTargetAlreadyTrapped && isAtLongRange) {
                priority = 10;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST -> priority += 8; // Loves to trap and toy with enemies.
                case STRATEGIST, SKIRMISHER -> priority += 4; // Excellent tactical move.
                case BERSERKER, AGGRESSIVE -> priority -= 7; // Wants damage, not traps.
            }
            return Math.max(0, priority);
        }
    },
    FISH_3(BloodDemonArts.FISH,
            "Octopus Vase Hell",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Octopus Vase Hell desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A ranged summon that provides area control.
            int priority = 0;
            int nearbySummons = context.user().level.getEntitiesOfClass(OctopusPotEntity.class, context.user().getBoundingBox().inflate(30.0), pot -> pot.getOwner() != null && pot.getOwner().equals(context.user().getUUID())).size();
            boolean canSummonMore = nearbySummons < 2;

            if (context.distanceSqToTarget() > 25 && context.userHealthPercent() > 0.6f && canSummonMore) {
                priority = 9;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case OVERLORD -> priority += 10; // More summons!
                case STRATEGIST -> priority += 5; // Excellent for board control.
                case DUELIST, BERSERKER -> priority -= 6; // Doesn't rely on others/props.
            }
            return Math.max(0, priority);
        }
    },
    FISH_4(BloodDemonArts.FISH,
            "Ten Thousand Gliding Slime-Fish",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Ten Thousand Gliding Slime-Fish desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A long-range "shotgun" blast for massive burst damage.
            int priority = 0;
            boolean isAtRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 625;
            boolean isUserConfident = context.userHealthPercent() > 0.4f;

            if (isAtRange && isUserConfident) {
                priority = 8;
                if (context.targetHealthPercent() < 0.5f) priority += 3;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case AGGRESSIVE, SKIRMISHER -> priority += 3; // Great for securing kills from range.
                case SADIST -> { if (context.targetHealthPercent() < 0.2f) priority = 1; } // Avoids quick kills.
            }
            return priority;
        }
    },
    FISH_5(BloodDemonArts.FISH,
            "Killer Fish Scales",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Killer Fish Scales desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: An "all-in" offensive ultimate, used to finish a fight or as a desperate comeback.
            int priority = 0;
            boolean isInRange = context.distanceSqToTarget() <= 225;

            if (isInRange) {
                if (context.targetHealthPercent() < 0.4f) priority = 15; // Finisher
                else if (context.userHealthPercent() < 0.35f) priority = 12; // Desperation
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case BERSERKER, AGGRESSIVE -> priority += 10; // The perfect ultimate.
                case COWARDLY -> { if (context.userHealthPercent() < 0.35f) priority += 10; } // Excellent escape/comeback tool.
                case CALM, STRATEGIST, STALWART -> priority -= 10; // Too reckless and committal.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Hantengu (Emotion Manifestation)
    // =================================================================================
    EMOTION_1(BloodDemonArts.EMOTION,
            "Weeping Spears",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Weeping Spears desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A shotgun-like blast of projectiles, devastating at close-to-mid range.
            int priority = 0;
            boolean isCrowdControlScenario = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(15.0)).size() > 1;
            boolean isAtOptimalRange = context.distanceSqToTarget() > 4 && context.distanceSqToTarget() <= 144;

            if (isAtOptimalRange) {
                priority = isCrowdControlScenario ? 9 : 7;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case AGGRESSIVE, BERSERKER -> priority += 3;
                case SKIRMISHER -> priority += 4;
                case DUELIST -> { if(isCrowdControlScenario) priority = 0; }
            }
            return priority;
        }
    },
    EMOTION_2(BloodDemonArts.EMOTION,
            "Crazed Cry of Thunderdeath",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Crazed Cry of Thunderdeath desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A high-risk, stationary "ultimate" turret mode to control a huge area.
            int priority = 0;
            boolean isCrowdControlNeeded = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(40.0)).size() > 1;
            boolean isAtVeryLongRange = context.distanceSqToTarget() > 400;
            boolean isUserHealthy = context.userHealthPercent() > 0.8f;

            if (isCrowdControlNeeded && isAtVeryLongRange && isUserHealthy) {
                priority = 25;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STRATEGIST -> priority += 10;
                case BERSERKER, DUELIST, SKIRMISHER, COWARDLY, AGGRESSIVE -> priority = 0; // Hate being stationary and vulnerable.
            }
            return priority;
        }
    },
    EMOTION_3(BloodDemonArts.EMOTION,
            "Countless Striking Trees",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Countless Striking Trees desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (ActiveFormManager.getActiveForm(user) instanceof CountlessStrikingTreesForm) {
                ActiveFormManager.stop(user);
            } else {
                ActiveFormManager.start(user, this);
            }
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Another long-duration "artillery mode" ultimate.
            int priority = 0;
            boolean isAtSafeDistance = context.distanceSqToTarget() > 225;
            boolean isUserHealthy = context.userHealthPercent() > 0.75f;

            if (isAtSafeDistance && isUserHealthy) {
                boolean isCrowd = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(40.0)).size() > 1;
                priority = isCrowd ? 20 : 4;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STRATEGIST -> priority += 10;
                case BERSERKER, DUELIST, SKIRMISHER, COWARDLY, AGGRESSIVE -> priority = 0; // Despise this kind of tactic.
            }
            return priority;
        }
    },
    EMOTION_4(BloodDemonArts.EMOTION,
            "Compressed Sound Waves",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Compressed Sound Waves desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A long-range, hitscan sniper attack that also stuns.
            int priority = 0;
            boolean targetIsAwake = !context.target().hasEffect(ModEffects.FORCED_SLEEP.get());
            boolean isAtLongRange = context.distanceSqToTarget() > 100 && context.distanceSqToTarget() <= 900;

            if (targetIsAwake && isAtLongRange) {
                priority = 9;
                if (context.targetHealthPercent() < 0.4f) priority += 5;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER -> priority += 6; // The ultimate kiting tool.
                case CALM -> priority += 4; // A safe, efficient choice.
                case SADIST -> priority += 5; // A long range stun is perfect.
                case BERSERKER, DUELIST -> priority -= 5; // Prefers to get close.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Akaza (Destructive Death)
    // =================================================================================
    DESTRUCTIVE_DEATH_1(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Compass Needle",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Compass Needle desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (ActiveFormManager.getActiveForm(user) instanceof CompassNeedleForm) {
                ActiveFormManager.stop(user);
            } else {
                ActiveFormManager.start(user, this);
            }
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A non-damaging, pre-fight analysis/buff ability.
            int priority = 0;
            boolean isPreFight = context.userHealthPercent() >= 1.0f;
            boolean isDuelSituation = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(64.0)).size() == 1;
            boolean isAtSafeDistance = context.distanceSqToTarget() > 36;

            if (isPreFight && isDuelSituation && isAtSafeDistance) {
                priority = 50;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case DUELIST, CALM, STRATEGIST -> priority += 20; // These types appreciate a tactical advantage.
                case BERSERKER, AGGRESSIVE -> priority = 0; // No time for thinking, only for fighting.
            }
            return priority;
        }

        @Override
        public boolean isBuff() {
            return true;
        }
    },
    DESTRUCTIVE_DEATH_2(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Air Type",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Air Type desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A standard, fast-moving projectile for safe, consistent damage.
            int priority = 0;
            boolean isMidToLongRange = context.distanceSqToTarget() > 25 && context.distanceSqToTarget() <= 625;

            if (isMidToLongRange) {
                priority = 8;
                if (context.userHealthPercent() > context.targetHealthPercent() + 0.2f) priority++;
                if (context.targetHealthPercent() < 0.3f) priority += 3;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER -> priority += 5; // A kiter's best friend.
                case COWARDLY, CALM -> priority += 3; // Safe damage is good damage.
                case BERSERKER, DUELIST -> priority -= 4; // Would rather be punching them directly.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_3(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Disorder",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Disorder desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (ActiveFormManager.getActiveForm(user) instanceof DisorderForm) {
                ActiveFormManager.stop(user);
            } else {
                ActiveFormManager.start(user, this);
            }
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A stationary, high-risk, high-reward channeled cone AOE.
            int priority = 0;
            boolean isUserHealthy = context.userHealthPercent() > 0.6f;
            boolean isAtOptimalRange = context.distanceSqToTarget() > 36 && context.distanceSqToTarget() <= 225;

            if (context.behavior() == Behavior.BERSERKER) isUserHealthy = true; // Ignores self-preservation.

            if (isUserHealthy && isAtOptimalRange) {
                priority = 8;
                if (context.userHealthPercent() < 0.4f && context.targetHealthPercent() < 0.25f) {
                    priority = 10; // Desperate trade.
                }
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case BERSERKER -> priority += 8; // Loves this kind of move.
                case AGGRESSIVE -> priority += 4;
                case COWARDLY, SKIRMISHER, CALM -> priority -= 5; // Hates being a stationary target.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_4(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Annihilation Type",
            IForm.InputType.HOLD,
            80,
            1,
            50,
            100,
            "Annihilation Type desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A chargeable ultimate ability used to finish a fight.
            int priority = 0;
            boolean isTargetWeak = context.targetHealthPercent() <= 0.5f;
            boolean isInRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 400;

            if (isTargetWeak && isInRange) {
                priority = 15 + (int) ((0.5f - context.targetHealthPercent()) * 20);
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case BERSERKER, AGGRESSIVE -> priority += 10; // The ultimate offensive tool.
                case SADIST -> priority = 0; // Finishes the fight too quickly. No fun.
                case CALM, STALWART -> priority -= 5; // Often too risky.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_5(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Leg Type: Crown Splitter",
            IForm.InputType.PRESS,
            80,
            0,
            50,
            100,
            "Leg Type: Crown Splitter desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A short-range lunge and combo starter.
            int priority = 0;
            if (context.distanceSqToTarget() > 4 && context.distanceSqToTarget() <= 25) {
                priority = 9;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case DUELIST, AGGRESSIVE, BERSERKER -> priority += 4; // Perfect for initiating a brawl.
                case SKIRMISHER, COWARDLY -> priority = 0; // Actively avoids this range.
            }
            return priority;
        }
    },
    DESTRUCTIVE_DEATH_6(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Leg Type: Explosive Flurry",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Leg Type: Explosive Flurry desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A high-damage, committed melee combo.
            int priority = 0;
            if (context.distanceSqToTarget() <= 16) {
                priority = 9;
                if (context.userHealthPercent() > context.targetHealthPercent()) priority += 2;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case DUELIST, AGGRESSIVE, BERSERKER -> priority += 5; // Their bread-and-butter.
                case SKIRMISHER, COWARDLY, CALM -> priority -= 4; // Avoids committed melee trades.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_7(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Leg Type: Flying Planet Thousand Wheels",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Leg Type: Flying Planet Thousand Wheels desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: An arcing AOE, good as an anti-air or ground launcher.
            int priority = 0;
            boolean isTargetAirborne = context.target().position().y > context.user().position().y + 1.5;
            boolean isTargetClose = context.distanceSqToTarget() <= 49;

            if (isTargetAirborne && isTargetClose) priority = 12;
            else if (isTargetClose) priority = 7;

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case TRICKSTER -> priority += 4; // Flashy and disorienting.
                case AGGRESSIVE, DUELIST -> priority += 2;
                case SKIRMISHER -> priority = 0; // Does not want to be this close.
            }
            return priority;
        }
    },
    DESTRUCTIVE_DEATH_8(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Eight-Layered Demon Core",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eight-Layered Demon Core desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A long-duration, multi-hit cone attack for sustained pressure.
            int priority = 0;
            boolean isAtMidRange = context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 144;

            if (isAtMidRange) {
                priority = 8;
                if (context.targetHealthPercent() < 0.5f) priority += 2;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST -> priority += 5; // Loves dealing many small, painful hits.
                case AGGRESSIVE -> priority += 2;
                case BERSERKER -> priority -= 3; // Wants to be closer.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_9(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Crushing Type: Ten Thousand Leaves Flashing Willow",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Crushing Type: Ten Thousand Leaves Flashing Willow desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A stationary, expanding PBAoE "get off me" tool.
            int priority = 0;
            boolean areEnemiesNearby = context.distanceSqToTarget() <= 100;
            boolean isUserHealthyEnough = context.userHealthPercent() > 0.3f;

            if (areEnemiesNearby && isUserHealthyEnough) {
                priority = 6 + (int) ((10 - Math.sqrt(context.distanceSqToTarget())) / 3);
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STALWART -> priority += 5; // Excellent defensive tool.
                case COWARDLY -> priority += 4; // A great panic button.
                case BERSERKER -> priority += 3; // Can be used offensively to start a brawl.
                case SKIRMISHER -> priority = 0; // Hates being stationary and surrounded.
            }
            return Math.max(0, priority);
        }
    },
    DESTRUCTIVE_DEATH_10(BloodDemonArts.DESTRUCTIVE_DEATH,
            "Final Form: Blue Silver Chaotic Afterglow",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Final Form: Blue Silver Chaotic Afterglow desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Ultimate stationary AOE to end the fight.
            int priority = 0;
            if (context.distanceSqToTarget() <= 256) {
                if (context.targetHealthPercent() < 0.6f) priority = 30; // Finisher
                else if (context.userHealthPercent() < 0.25f) priority = 25; // Desperation
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case BERSERKER -> priority += 20; // The best way to go out.
                case AGGRESSIVE -> priority += 10;
                case SADIST -> priority = 0; // Too quick and merciful.
                case COWARDLY, SKIRMISHER -> priority -= 10; // Stationary = death.
            }
            return Math.max(0, priority);
        }
    },

    // =================================================================================
    // BDA, Doma (Cryokinesis)
    // =================================================================================
    CRYO_1(BloodDemonArts.CRYO,
            "Frozen Lotus",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Frozen Lotus desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Wide frontal cone with damage and strong slow.
            int priority = 0;
            if (context.distanceSqToTarget() <= 49) {
                priority = 8;
                if (context.userHealthPercent() > context.targetHealthPercent()) priority++;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST -> priority += 5; // Loves to slow and cripple.
                case STALWART, SKIRMISHER -> priority += 3; // Great for controlling enemy movement.
            }
            return priority;
        }
    },
    CRYO_2(BloodDemonArts.CRYO,
            "Barren Hanging Garden",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Barren Hanging Garden desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Multi-wave PBAoE with damage, slow, and knockback.
            int priority = 0;
            if (context.distanceSqToTarget() <= 36) {
                boolean isCrowd = context.user().level.getEntitiesOfClass(Player.class, context.user().getBoundingBox().inflate(6.0)).size() > 1;
                priority = isCrowd ? 12 : 9;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STALWART -> priority += 5; // The perfect defensive tool.
                case COWARDLY, SKIRMISHER -> priority += 4; // Creates space.
                case BERSERKER -> priority += 2; // Also an offensive tool.
            }
            return priority;
        }
    },
    CRYO_3(BloodDemonArts.CRYO,
            "Freezing Clouds",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Freezing Clouds desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Long-range area denial and crippling debuff tool.
            int priority = 0;
            if (context.distanceSqToTarget() > 100 && context.userHealthPercent() > 0.7f) {
                priority = 8;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SADIST, STRATEGIST, SKIRMISHER -> priority += 6; // Perfect for their game plan.
                case BERSERKER, AGGRESSIVE, DUELIST -> priority -= 5; // Too slow, not enough damage.
            }
            return Math.max(0, priority);
        }
    },
    CRYO_4(BloodDemonArts.CRYO,
            "Lotus Vines",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Lotus Vines desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Creates a persistent AOE hazard at a target location.
            int priority = 0;
            if (context.distanceSqToTarget() > 64 && context.distanceSqToTarget() <= 400) {
                priority = 7;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STRATEGIST -> priority += 8; // The ultimate area denial.
                case SKIRMISHER, CALM -> priority += 4; // Good for controlling the battlefield.
                case BERSERKER, AGGRESSIVE -> priority -= 5; // Why control when you can destroy?
            }
            return Math.max(0, priority);
        }
    },
    CRYO_5(BloodDemonArts.CRYO,
            "Cold White Princesses",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cold White Princesses desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: Summons two allies to create a numbers advantage.
            int priority = 0;
            int nearbyAllies = context.user().level.getEntitiesOfClass(ColdWhitePrincessEntity.class, context.user().getBoundingBox().inflate(40.0), e -> e.getOwner() == context.user()).size();

            if (nearbyAllies < 2) {
                if (context.userHealthPercent() > 0.9f) priority = 10; // Opener
                else if (context.userHealthPercent() < 0.5f) priority = 9; // Backup
                else priority = 6; // General pressure
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case OVERLORD -> priority += 12; // Must. Have. Minions.
                case TRICKSTER, COWARDLY, STRATEGIST -> priority += 5; // Minions are great distractions/tools.
                case DUELIST, BERSERKER -> priority = 0; // Fights alone.
            }
            return priority;
        }
    },
    CRYO_6(BloodDemonArts.CRYO,
            "Wintry Icicles",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Wintry Icicles desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A long-range, telegraphed AOE nuke.
            int priority = 0;
            if (context.distanceSqToTarget() > 144) {
                priority = 8;
                if (context.target().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) priority += 4;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case SKIRMISHER, CALM -> priority += 4; // A perfect safe, high-damage ability.
                case SADIST -> { if(context.target().getHealth() < 40.0f) priority = 1;} // Don't want to kill them accidentally.
                case BERSERKER -> priority -= 4; // Wants to be closer.
            }
            return Math.max(0, priority);
        }
    },
    CRYO_7(BloodDemonArts.CRYO,
            "Scattering Lotuses",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Scattering Lotuses desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A "shotgun" blast for high burst damage at close-to-mid range.
            int priority = 0;
            if (context.distanceSqToTarget() > 16 && context.distanceSqToTarget() <= 144) {
                priority = 7;
                if (context.targetHealthPercent() < 0.4f) priority += 3;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case AGGRESSIVE, BERSERKER -> priority += 4; // Great for raw damage.
                case SKIRMISHER -> priority += 2;
                case SADIST -> { if (priority > 8) priority -= 5; } // Tries to avoid burst kills.
            }
            return priority;
        }
    },
    CRYO_8(BloodDemonArts.CRYO,
            "Crystalline Divine Child",
            IForm.InputType.HOLD,
            20 * 6,
            0,
            50,
            100,
            "Crystalline Divine Child desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: A chargeable summon that requires a safe opportunity.
            int priority = 0;
            boolean isAtSafeDistance = context.distanceSqToTarget() > 225;
            boolean isUserHealthy = context.userHealthPercent() > 0.7f;
            int existingClones = context.user().level.getEntitiesOfClass(CrystallineDivineChildEntity.class, context.user().getBoundingBox().inflate(50), e -> e.getOwner() == context.user()).size();

            if (isAtSafeDistance && isUserHealthy && existingClones < 3) {
                priority = 15;
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case OVERLORD -> { // Will try to use it even in suboptimal conditions.
                    if (context.distanceSqToTarget() > 100 && context.userHealthPercent() > 0.5f && existingClones < 5) priority = 20;
                }
                case TRICKSTER, COWARDLY -> { if (priority > 0) priority += 5; }
                case BERSERKER, AGGRESSIVE, DUELIST -> priority = 0; // Hates waiting and charging.
            }
            return priority;
        }
    },
    CRYO_9(BloodDemonArts.CRYO,
            "Rime — Water Lily Bodhisattva",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rime — Water Lily Bodhisattva desc.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            // TACTICS: An ultimate transformation for survival and comeback potential.
            int priority = 0;
            if (context.userHealthPercent() < 0.30f) {
                priority = 100; // Panic button
            } else if (context.userHealthPercent() < 0.6f && context.targetHealthPercent() > 0.7f) {
                priority = 20; // Turn the tide
            }

            // BEHAVIOR ADJUSTMENTS
            switch (context.behavior()) {
                case STALWART, COWARDLY, CALM -> { if (priority > 0) priority += 20; } // The perfect ultimate for them.
                case BERSERKER, DUELIST -> { if (priority > 0) priority -= 50; } // Would rather die fighting honorably than use this "trick".
            }
            return Math.max(0, priority);
        }
    },
    // =================================================================================
    // BDA, Toon Force
    // =================================================================================
    TOON_FORCE_1(BloodDemonArts.TOON_FORCE,
            "Eye Pop",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Eye pops off, bounces off wall endlessly, and hits someone eventually.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_2(BloodDemonArts.TOON_FORCE,
            "Acme Anvil",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Giant 2D-ish cartoon anvil, flattens target.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_3(BloodDemonArts.TOON_FORCE,
            "Pie Splat",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Pie splats attacker's screen on successful counter.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_4(BloodDemonArts.TOON_FORCE,
            "Banana Peel",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "A deceptive trap that makes victims slip uncontrollably.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_5(BloodDemonArts.TOON_FORCE,
            "Shadow Toss",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Rips shadow off ground, sticks to wall/floor, recast to teleport to it.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_6(BloodDemonArts.TOON_FORCE,
            "Fake Tunnel",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Fake wall tunnel user can pass through but enemies slam into.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    TOON_FORCE_7(BloodDemonArts.TOON_FORCE,
            "Pocket",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "User reaches behind back and pulls out a random item.",
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            ActiveFormManager.start(user, this);
        }

        @Override
        public int getUsagePriority(CombatContext context) {
            return 10;
        }
    },
    ;

    private final BloodDemonArts style;
    private final String displayName;
    private final IForm.InputType InputType;
    private final int chargeTicks;
    private final int breathCost;
    private final int styleMasteryToUnlock;
    private final int masteryGoal;
    private final String description;
    private final int baseCooldown;

    DemonForms(BloodDemonArts style, String displayName, IForm.InputType InputType, int chargeTicks, int breathCost, int styleMasteryToUnlock, int masteryGoal, String description, int baseCooldown) {
        this.style = style;
        this.displayName = displayName;
        this.InputType = InputType;
        this.chargeTicks = chargeTicks;
        this.breathCost = breathCost;
        this.styleMasteryToUnlock = styleMasteryToUnlock;
        this.masteryGoal = masteryGoal;
        this.description = description;
        this.baseCooldown = baseCooldown;
    }

    @Override public Object getStyle() { return style; }
    @Override public String getDisplayName() { return this.displayName; }
    @Override public String getDescription() { return this.description; }
    @Override public IForm.InputType getInputType() { return this.InputType; }
    @Override public int getChargeTicks() { return this.chargeTicks; }
    @Override public int getBreathCost() { return this.breathCost; }
    @Override public int getStyleMasteryToUnlock() { return styleMasteryToUnlock; }
    @Override public int getMasteryGoal() { return masteryGoal; }

    @Override public int getCooldownTicks(LivingEntity user) {
        Rank rank = getPlayerRank(user);
        int reduction = rank.ordinal() * 5;
        return Math.max(1, baseCooldown - reduction);
    }

    @Override public abstract void performAction(LivingEntity user, ServerLevel level);

    private Rank getPlayerRank(LivingEntity user) {
        if (user instanceof Player player) {
            return player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                    .map(PlayerProgression::getRank)
                    .orElse(Rank.CIVILIAN);
        }
        return Rank.CIVILIAN;
    }
}
