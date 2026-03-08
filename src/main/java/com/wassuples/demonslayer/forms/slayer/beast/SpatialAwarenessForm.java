package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.UpdateGlowDataPacket;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Beast Breathing form: Spatial Awareness.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SpatialAwarenessForm extends ActiveForm {
    private static final double SENSE_RADIUS = 128.0;
    private static final int RESCAN_INTERVAL = 20; // Rescans every second

    private Set<LivingEntity> currentTargets;
    private int rescanTimer = 0;

    public SpatialAwarenessForm(LivingEntity user, IForm form) {
        super(user, form);
        this.currentTargets = new HashSet<>();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        if (!level.isClientSide && user instanceof ServerPlayer player) {
            level.playSound(null, user.blockPosition(), SoundEvents.HOGLIN_ANGRY, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateTargets(player);
        }
    }

    @Override
    public void onStop() {
        if (!level.isClientSide && user instanceof ServerPlayer player) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (this.currentTargets != null) {
                for (LivingEntity target : this.currentTargets) {
                    if (target.isAlive()) {
                        target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                            if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(player.getUUID())) {
                                cap.clear();
                                ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (level.isClientSide || !(user instanceof ServerPlayer player)) {
            return;
        }

        rescanTimer++;
        if (rescanTimer >= RESCAN_INTERVAL) {
            rescanTimer = 0;
            updateTargets(player);
        }
    }

    private void updateTargets(ServerPlayer player) {
        Set<LivingEntity> newTargets = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SENSE_RADIUS),
                entity -> entity != player && entity.isAlive()));

        Set<LivingEntity> targetsToAdd = new HashSet<>(newTargets);
        targetsToAdd.removeAll(this.currentTargets);

        String glowColor = "#A52A2A";
        for (LivingEntity target : targetsToAdd) {
            target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                cap.setGlowColor(glowColor);
                cap.setExclusiveViewer(player.getUUID());
                cap.setGlowing(true);
                ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), glowColor, true, player.getUUID()));
            });
        }

        Set<LivingEntity> targetsToRemove = new HashSet<>(this.currentTargets);
        targetsToRemove.removeAll(newTargets);

        for (LivingEntity target : targetsToRemove) {
            if (target.isAlive()) {
                target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                    if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(player.getUUID())) {
                        cap.clear();
                        ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                    }
                });
            }
        }
        this.currentTargets = newTargets;
    }
}
