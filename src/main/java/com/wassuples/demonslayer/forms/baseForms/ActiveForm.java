package com.wassuples.demonslayer.forms.baseForms;

import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import static com.wassuples.demonslayer.util.CombatHelper.isPlayerHoldingNichirin;

public abstract class ActiveForm {
    protected final LivingEntity user;
    protected final ServerLevel level;
    protected final IForm form;
    protected final int startTick;
    protected int ticksActive;

    public ActiveForm(LivingEntity user, IForm form) {
        this.user = user;
        this.level = (ServerLevel) user.getLevel();
        this.form = form;
        this.startTick = user.tickCount;
        this.ticksActive = 0;
    }

    public final void tick() {
        if (user instanceof ServerPlayer player) {
            boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(PlayerDemon::isDemon)
                    .orElse(false);
            boolean hasSword = isPlayerHoldingNichirin(player);

            // Is anyone trying to use a breathing style without a sword?
            if (!hasSword && (form instanceof com.wassuples.demonslayer.styles.BreathingForms)) {
                ActiveFormManager.stop(player);
                return;
            }

            // Is a human trying to use BDA?
            if ((this.form instanceof com.wassuples.demonslayer.styles.DemonForms) && !isDemon) {
                ActiveFormManager.stop(player);
                return;
            }
        }

        this.ticksActive = user.tickCount - startTick;
        onTick();
    }

    /**
     * Ticks this form as a sub-step of another form.
     * This bypasses ActiveFormManager validation and is intended for composite forms (like chained combos)
     * which want to reuse existing form logic without letting the sub-form stop the active parent form.
     */
    public void tickAsSubForm() {
        this.ticksActive = user.tickCount - startTick;
        onTick();
    }

    // --- Methods to be overridden by implementations ---
    public void onStart() {
    }

    public abstract void onTick();

    public void onStop() {
    }

    public void onHurt(LivingHurtEvent event) {
    }

    public boolean isInterruptible() {
        return true;
    }

    public IForm getForm() {
        return form;
    }
}
