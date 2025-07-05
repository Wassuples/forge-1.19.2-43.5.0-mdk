package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.styles.BreathingForms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class HashiraAttackGoal extends Goal {
    private final HashiraEntity hashira;
    private final double speedModifier;
    private final boolean followTargetEvenIfNotSeen;
    private int attackCooldown;
    private int timeSinceLastAttack;

    public HashiraAttackGoal(HashiraEntity pMob, double pSpeedModifier, boolean pFollowTargetEvenIfNotSeen) {
        this.hashira = pMob;
        this.speedModifier = pSpeedModifier;
        this.followTargetEvenIfNotSeen = pFollowTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hashira.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.hashira.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return canUse();
    }

    @Override
    public void start() {
        super.start();
        this.attackCooldown = 0;
        this.timeSinceLastAttack = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.hashira.setTarget(null);
    }

    @Override
    public void tick() {
        LivingEntity target = this.hashira.getTarget();
        if (target == null) return;

        this.hashira.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSq = this.hashira.distanceToSqr(target.getX(), target.getY(), target.getZ());
        this.timeSinceLastAttack++;

        // --- MOVEMENT ---
        // Always try to move towards the target
        this.hashira.getNavigation().moveTo(target, this.speedModifier);

        // --- ATTACKING ---
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        // Check if in attack range and if enough time has passed since the last attack
        if (distanceSq <= this.getAttackReachSqr(target) && this.timeSinceLastAttack >= 20) {
            this.timeSinceLastAttack = 0;
            // For now, we will pick a random form to use.
            // Later, you can add more complex logic here.
            chooseAndUseRandomForm(target);
        }
    }

    private void chooseAndUseRandomForm(LivingEntity target) {
        if (this.hashira.level instanceof ServerLevel serverLevel) {
            // Get all forms for the Hashira's style
            List<BreathingForms> availableForms = Arrays.stream(BreathingForms.values())
                    .filter(form -> form.getStyle() == this.hashira.getBreathingStyle())
                    .collect(Collectors.toList());

            if (!availableForms.isEmpty()) {
                // Pick one at random and perform the action
                BreathingForms formToUse = availableForms.get(this.hashira.getRandom().nextInt(availableForms.size()));

                formToUse.performAction(this.hashira, serverLevel);

                // Set a cooldown so the Hashira doesn't spam attacks
                this.attackCooldown = 10; // 0.5 seconds
            }
        }
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return 9.0D;
    }
}