package com.wassuples.demonslayer.nemesis.ai.goals;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.*;
import java.util.stream.Collectors;

public class NemesisAttackGoal extends Goal {
    protected final NemesisDemonEntity mob;
    private final double speedModifier;
    private int attackCooldown;
    private int timeSinceLastAttack;
    private final boolean followTargetEvenIfNotSeen;
    private final float cooldownModifier;

    public NemesisAttackGoal(NemesisDemonEntity mob, double speedModifier, boolean followTargetEvenIfNotSeen, float cooldownModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.cooldownModifier = cooldownModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
        this.attackCooldown = 0;
        this.timeSinceLastAttack = 0;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        this.timeSinceLastAttack++;

        // Movement
        if (this.followTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target)) {
            if (distanceSq > this.getAttackReachSqr(target)) {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            } else {
                this.mob.getNavigation().stop();
            }
        }

        // Attack
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        if (distanceSq <= this.getAttackReachSqr(target)) {
            chooseAndUseForm();
        }
    }

    protected void chooseAndUseForm() {
        if (!(this.mob.level instanceof ServerLevel serverLevel)) return;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            BloodDemonArts bda = trait.getAssignedBDA();

            // a list of all possible actions
            List<IForm> allForms = (bda != null && bda != BloodDemonArts.NONE) ?
                    Arrays.stream(DemonForms.values()).filter(form -> form.getStyle() == bda).collect(Collectors.toList()) :
                    new ArrayList<>();

            // Create the Context
            IForm.CombatContext context = new IForm.CombatContext(
                    this.mob,
                    target,
                    this.mob.distanceToSqr(target),
                    this.mob.getHealth() / this.mob.getMaxHealth(),
                    target.getHealth() / target.getMaxHealth(),
                    trait.getBehavior()
            );

            // Calculate Priority for each Form
            IForm bestForm = allForms.stream()
                    .max(Comparator.comparingInt(form -> form.getUsagePriority(context)))
                    .orElse(null);

            int bestFormPriority = (bestForm != null) ? bestForm.getUsagePriority(context) : 0;

            // Decide whether to use the best form or a basic attack
            // A basic melee attack has a default priority of 1.
            if (bestForm != null && bestFormPriority > 1) {
                bestForm.performAction(this.mob, serverLevel);
                this.attackCooldown = (int)(bestForm.getCooldownTicks(this.mob) * this.cooldownModifier);
            } else {
                // If no form has a high enough priority, just do a basic physical attack.
                performBasicAttack();
            }
        });
    }

    protected void performBasicAttack() {
        this.mob.doHurtTarget(this.mob.getTarget());
        this.attackCooldown = (int)(20 * this.cooldownModifier);
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        // 4x4 blocks = 16. A good melee range.
        return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pAttackTarget.getBbWidth();
    }
}