package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.entity.custom.CrystallineDivineChildEntity;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override public boolean canUse() { return this.clone.getTarget() != null; }
    @Override public boolean canContinueToUse() { return this.clone.getTarget() != null && this.clone.getTarget().isAlive(); }

    @Override public void start() { this.attackCooldown = 20; }

    @Override
    public void tick() {
        LivingEntity target = this.clone.getTarget();
        if (target == null) return;

        this.clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.timeSinceLastAttack++;

        this.clone.getNavigation().moveTo(target, this.speedModifier);

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        if (this.clone.distanceToSqr(target) < 64.0 && this.timeSinceLastAttack >= 60) {
            this.timeSinceLastAttack = 0;
            chooseAndUseRandomForm();
        }
    }

    private void chooseAndUseRandomForm() {
        if (this.clone.level instanceof ServerLevel serverLevel) {
            List<BreathingForms> availableForms = Arrays.stream(BreathingForms.values())
                    .filter(form -> form.getStyle() == BreathingStyles.CRYO
                            && form.getInputType() == BreathingForms.InputType.PRESS)
                    .collect(Collectors.toList());

            if (!availableForms.isEmpty()) {
                BreathingForms formToUse = availableForms.get(this.clone.getRandom().nextInt(availableForms.size()));
                formToUse.performAction(this.clone, serverLevel);
//                this.attackCooldown = formToUse.getCooldownTicks();
            }
        }
    }
}