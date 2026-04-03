package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.wassuples.demonslayer.entity.ally.SummonAllies.isFriendlyForTargeting;

/**
 * AI goal: Doma Clone Attack Goal.
 */
public class DomaCloneAttackGoal extends Goal {
    private final CrystallineDivineChildEntity clone;
    private final double speedModifier;
    private int attackCooldown;
    private int timeSinceLastAttack;

    public DomaCloneAttackGoal(CrystallineDivineChildEntity pMob, double pSpeedModifier, boolean pFollowTargetEvenIfNotSeen) {
        this.clone = pMob;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.clone.getTarget();
        return target != null && target.isAlive() && !isFriendlyForTargeting(this.clone, target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.clone.getTarget();
        return target != null && target.isAlive() && !isFriendlyForTargeting(this.clone, target);
    }

    @Override public void start() {
        this.attackCooldown = 20;
        this.timeSinceLastAttack = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.clone.getTarget();
        if (target == null || !target.isAlive() || isFriendlyForTargeting(this.clone, target)) return;

        this.clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.timeSinceLastAttack++;

        this.clone.getNavigation().moveTo(target, this.speedModifier);

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        double distSqr = this.clone.distanceToSqr(target);

        // If we're in melee range, always swing sometimes.
        if (distSqr <= getAttackReachSqr(target) && this.clone.hasLineOfSight(target) && this.timeSinceLastAttack >= 20) {
            this.timeSinceLastAttack = 0;
            this.clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.clone.doHurtTarget(target);
            this.attackCooldown = 10;
            return;
        }

        // Otherwise occasionally use a form (ranged/utility attacks).
        if (this.attackCooldown <= 0 && distSqr < 64.0 && this.timeSinceLastAttack >= 60) {
            this.timeSinceLastAttack = 0;
            if (chooseAndUseRandomForm()) {
                this.attackCooldown = 40;
            } else {
                // fallback: don't stall if no forms available
                this.attackCooldown = 20;
            }
        }
    }

    private boolean chooseAndUseRandomForm() {
        if (this.clone.level instanceof ServerLevel serverLevel) {
            List<DemonForms> availableForms = Arrays.stream(DemonForms.values())
                    .filter(form -> form.getStyle() == BloodDemonArts.CRYO
                            && form.getInputType() == BreathingForms.InputType.PRESS)
                    .collect(Collectors.toList());

            if (!availableForms.isEmpty()) {
                DemonForms formToUse = availableForms.get(this.clone.getRandom().nextInt(availableForms.size()));
                formToUse.performAction(this.clone, serverLevel);
                return true;
            }
        }
        return false;
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return (double)(this.clone.getBbWidth() * 2.0F * this.clone.getBbWidth() * 2.0F + pAttackTarget.getBbWidth());
    }
}
