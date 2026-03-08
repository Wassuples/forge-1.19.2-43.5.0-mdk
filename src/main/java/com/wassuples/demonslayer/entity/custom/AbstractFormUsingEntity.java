package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Abstract base for all NPCs that can use combat forms ({@link com.wassuples.demonslayer.styles.IForm}).
 *
 * <p>Provides the bridge between the entity AI system and the {@link ActiveFormManager}:
 * <ul>
 *   <li>Ticks any active form each server tick</li>
 *   <li>Registers common AI goals (float, stroll, look around)</li>
 *   <li>Exposes {@link #getBreathingStyle()} and {@link #getBloodDemonArt()} for subclass specialization</li>
 * </ul>
 *
 * <p>Direct subclasses: {@link DemonEntity}, {@link HashiraEntity}
 */
public abstract class AbstractFormUsingEntity extends PathfinderMob {

    public AbstractFormUsingEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * This method is called every tick to update any active forms.
     */
    @Override
    public void tick() {
        super.tick();
        // On the server side, check for an active form and tick it.
        if (!this.level.isClientSide()) {
            ActiveForm activeForm = ActiveFormManager.getActiveForm(this);
            if (activeForm != null) {
                activeForm.tick();
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /**
     * Gets the Breathing Style for this entity.
     * @return The BreathingStyle, or null if it's not a breathing user.
     */
    @Nullable
    public abstract BreathingStyles getBreathingStyle();

    /**
     * Gets the Blood Demon Art for this entity.
     * @return The BloodDemonArt, or null if it's not a demon.
     */
    @Nullable
    public abstract BloodDemonArts getDemonArt();
}
