package com.wassuples.demonslayer.styles;

import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public enum DemonForms implements IForm {
    THREADS_1(BloodDemonArts.THREADS,
            "Cutting Thread Cage",
            IForm.InputType.PRESS,
            0,
            0,
            50,
            100,
            "Cutting Thread Cage desc.",
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20){
        @Override
        public void performAction(LivingEntity user,  ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.CountlessStrikingTreesForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level)  {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.CompassNeedleForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            if (FormUtils.ActiveFormManager.getActiveForm(user) instanceof FormUtils.DisorderForm) {
                FormUtils.ActiveFormManager.stop(user);
            } else {
                FormUtils.ActiveFormManager.start(user, this);
            }
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    CRYO_8(BloodDemonArts.CRYO,
            "Crystalline Divine Child",
            IForm.InputType.HOLD,
            20*6,
            0,
            50,
            100,
            "Crystalline Divine Child desc.",
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
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
            true,
            20) {
        @Override
        public void performAction(LivingEntity user, ServerLevel level) {
            FormUtils.ActiveFormManager.start(user, this);
        }
    },
    ;

    private Rank getPlayerRank(LivingEntity user) {
        if (user instanceof Player player) {
            return player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                    .map(PlayerProgression::getRank)
                    .orElse(Rank.CIVILIAN);
        }
        return Rank.CIVILIAN;
    }

    private final BloodDemonArts style;
    private final String displayName;
    private final IForm.InputType InputType;
    private final int chargeTicks;
    private final int breathCost;
    private final int styleMasteryToUnlock;
    private final int masteryGoal;
    private final String description;
    private final Boolean isBDA;
    private final int baseCooldown;

    DemonForms(BloodDemonArts style, String displayName, IForm.InputType InputType, int chargeTicks, int breathCost, int styleMasteryToUnlock, int masteryGoal, String description, boolean isBDA, int baseCooldown) {
        this.style = style;
        this.displayName = displayName;
        this.InputType = InputType;
        this.chargeTicks = chargeTicks;
        this.breathCost = breathCost;
        this.styleMasteryToUnlock = styleMasteryToUnlock;
        this.masteryGoal = masteryGoal;
        this.description = description;
        this.isBDA = isBDA;
        this.baseCooldown = baseCooldown;
    }

    public BloodDemonArts getStyle() { return style; }
    @Override public String getDisplayName() { return this.displayName; }
    @Override public IForm.InputType getInputType() { return this.InputType; }
    @Override public int getChargeTicks() { return this.chargeTicks; }
    @Override public int getBreathCost() { return this.breathCost; }
    @Override public boolean IsBDA() { return this.isBDA; }
    public int getStyleMasteryToUnlock() { return styleMasteryToUnlock; }
    public int getMasteryGoal() { return masteryGoal; }
    public String getDescription() { return description; }

    @Override public int getCooldownTicks(LivingEntity user) {
        Rank rank = getPlayerRank(user);
        int reduction = rank.ordinal() * 5; // rank 1 gets 5 ticks off, rank 11 gets 55 ticks off

        return Math.max(1, baseCooldown - reduction); // minimum cooldown is 0.05 seconds, 1 tick
    }
    @Override public abstract void performAction(LivingEntity user, ServerLevel level);
}
