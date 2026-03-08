package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.entity.custom.AbstractFormUsingEntity;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goal: Form User Attack Goal.
 */
public class FormUserAttackGoal extends Goal {
    private final AbstractFormUsingEntity pMob;
    private final double speedModifier;
    private int attackCooldown;
    private int timeSinceLastAttack;

    public FormUserAttackGoal(AbstractFormUsingEntity pMob, double pSpeedModifier, boolean pFollowTargetEvenIfNotSeen) {
        this.pMob = pMob;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.pMob.getTarget() != null && this.pMob.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        super.start();
        this.attackCooldown = 0;
        this.timeSinceLastAttack = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.pMob.getTarget();
        if (target == null) return;

        this.pMob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSq = this.pMob.distanceToSqr(target);
        this.timeSinceLastAttack++;

        if (distanceSq > this.getAttackReachSqr(target)) {
            this.pMob.getNavigation().moveTo(target, this.speedModifier);
        } else {
            this.pMob.getNavigation().stop();
        }


        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        if (distanceSq <= this.getAttackReachSqr(target) && this.timeSinceLastAttack >= 20) {
            this.timeSinceLastAttack = 0;
            chooseAndUseRandomForm(target);
        }
    }

    private void chooseAndUseRandomForm(LivingEntity target) {
        if (!(this.pMob.level instanceof ServerLevel serverLevel)) return;

        List<IForm> allAvailableForms = new ArrayList<>();

        if (this.pMob.getBreathingStyle() != null) {
            Arrays.stream(BreathingForms.values())
                    .filter(form -> form.getStyle() == this.pMob.getBreathingStyle())
                    .forEach(allAvailableForms::add);
        }

        if (this.pMob.getDemonArt() != null) {
            Arrays.stream(DemonForms.values())
                    .filter(form -> form.getStyle() == this.pMob.getDemonArt())
                    .forEach(allAvailableForms::add);
        }

        if (!allAvailableForms.isEmpty()) {
            IForm formToUse = allAvailableForms.get(this.pMob.getRandom().nextInt(allAvailableForms.size()));
            formToUse.performAction(this.pMob, serverLevel);
            this.attackCooldown = 40;
        } else {
            this.pMob.doHurtTarget(target);
            this.attackCooldown = 20;
        }
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return 16.0D;
    }
}