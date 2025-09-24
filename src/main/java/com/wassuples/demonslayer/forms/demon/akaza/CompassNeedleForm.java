package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.entity.custom.CompassNeedleEntity;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import com.wassuples.demonslayer.entity.custom.demon.UpperMoonOneEntity;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.UpdateCompassInfoPacket;
import com.wassuples.demonslayer.networking.packet.UpdateGlowDataPacket;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

public class CompassNeedleForm extends ActiveForm {
    private Set<LivingEntity> currentTargets;
    private int rescanTimer = 0;

    public CompassNeedleForm(LivingEntity user, IForm form) {
        super(user, form);
        this.currentTargets = new HashSet<>();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        if (!level.isClientSide && user instanceof ServerPlayer caster) {
            applyImmobility(caster, 40);
            updateTargets(caster);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);

            CompassNeedleEntity effect = new CompassNeedleEntity(level, caster.getX(), caster.getY() + 0.01, caster.getZ());
            effect.setYRot(caster.getYRot());
            level.addFreshEntity(effect);
        }
    }

    @Override
    public void onStop() {
        if (!level.isClientSide && user instanceof ServerPlayer caster) {
            removeImmobility(caster);
            if (this.currentTargets == null) return;

            for (LivingEntity target : this.currentTargets) {
                if (target.isAlive()) {
                    target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                        if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(caster.getUUID())) {
                            cap.clear();
                            ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                        }
                    });
                }
            }
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
            ModMessages.sendToPlayer(new UpdateCompassInfoPacket(-1, Component.empty(), 0, 0, 0, 0, 0, BlockPos.ZERO, new ArrayList<>(), Component.empty(), Component.empty(), -1, "", -1, -1, "", false, "", -1, ""), caster);
        }
    }

    @Override
    public void onTick() {
        if (level.isClientSide || !(user instanceof ServerPlayer caster)) {
            return;
        }

        rescanTimer++;
        if (rescanTimer >= 20) {
            rescanTimer = 0;
            updateTargets(caster);
        }

        updateAndSendTargetInfo(caster);
    }

    private void updateAndSendTargetInfo(ServerPlayer caster) {
        double range = 128.0;

        Vec3 startPos = caster.getEyePosition();
        Vec3 lookVector = caster.getLookAngle();
        Vec3 endPos = startPos.add(lookVector.scale(range));

        AABB searchBox = caster.getBoundingBox().expandTowards(lookVector.scale(range)).inflate(1.0D, 1.0D, 1.0D);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                caster.level,
                caster,
                startPos,
                endPos,
                searchBox,
                (entity) -> !entity.isSpectator() && entity.isPickable() && entity.isAlive() && entity instanceof LivingEntity && !entity.is(caster)
        );

        if (result != null && result.getEntity() instanceof LivingEntity target) {
            Component entityTypeName = target.getType().getDescription();

            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            int armor = target.getArmorValue();

            int air = target.getAirSupply();
            int maxAir = target.getMaxAirSupply();

            BlockPos position = target.blockPosition();

            Component mainHandItem = target.getMainHandItem().getHoverName();
            Component offHandItem = target.getOffhandItem().getHoverName();

            List<String> effects = target.getActiveEffects().stream().map(effect -> effect.getEffect().getDisplayName().getString() + " " + (effect.getAmplifier() + 1)).collect(Collectors.toList());

            int hunger = (target instanceof Player p) ? p.getFoodData().getFoodLevel() : -1;

            String slayerRank = target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).map(prog -> prog.getRank().getDisplayName().getString()).orElse("");

            final int[] breathData = {-1, -1};
            target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(cap -> {
                breathData[0] = cap.getBreath();
                breathData[1] = cap.getMaxBreath();
            });

            String style = target.getCapability(PlayerStyleProvider.PLAYER_STYLE).map(cap -> cap.getCurrentStyles().getDisplayName()).orElse("");

            final boolean[] isDemon = {false};
            final String[] demonRank = {""};
            final int[] bloodPoints = {-1};
            final String[] bda = {""};

            if (target instanceof DemonEntity demonMob) {
                isDemon[0] = true;
                demonRank[0] = demonMob.getDemonRank().getDisplayName().getString();
                if (demonMob.getDemonArt() != null) {
                    bda[0] = demonMob.getDemonArt().getDisplayName();
                }
            } else if (target instanceof UpperMoonOneEntity um1) {
                isDemon[0] = true;
                demonRank[0] = um1.getDemonRank().getDisplayName().getString();
                if (um1.getDemonArt() != null) {
                    bda[0] = um1.getDemonArt().getDisplayName();
                }
            } else {
                target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(cap -> {
                    if (cap.isDemon()) {
                        isDemon[0] = true;
                        demonRank[0] = cap.getRank().getDisplayName().getString();
                        bloodPoints[0] = cap.getBloodPoints();
                        bda[0] = cap.getEquippedBda().getDisplayName();
                    }
                });
            }

            ModMessages.sendToPlayer(new UpdateCompassInfoPacket(target.getId(), entityTypeName, health, maxHealth, armor, air, maxAir, position, effects, mainHandItem, offHandItem, hunger, slayerRank, breathData[0], breathData[1], style, isDemon[0], demonRank[0], bloodPoints[0], bda[0]), caster);
        } else {
            ModMessages.sendToPlayer(new UpdateCompassInfoPacket(-1, Component.empty(), 0, 0, 0, 0, 0, BlockPos.ZERO, new ArrayList<>(), Component.empty(), Component.empty(), -1, "", -1, -1, "", false, "", -1, ""), caster);
        }
    }

    private void updateTargets(ServerPlayer caster) {
        Set<LivingEntity> newTargets = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(64),
                entity -> entity != caster && entity.isAlive()));
        Set<LivingEntity> targetsToAdd = new HashSet<>(newTargets);
        targetsToAdd.removeAll(this.currentTargets);

        String glowColor = "#FF5555";
        for (LivingEntity target : targetsToAdd) {
            target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                cap.setGlowColor(glowColor);
                cap.setExclusiveViewer(caster.getUUID());
                cap.setGlowing(true);
                ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), glowColor, true, caster.getUUID()));
            });
        }
        Set<LivingEntity> targetsToRemove = new HashSet<>(this.currentTargets);
        targetsToRemove.removeAll(newTargets);

        for (LivingEntity target : targetsToRemove) {
            if (target.isAlive()) {
                target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                    if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(caster.getUUID())) {
                        cap.clear();
                        ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                    }
                });
            }
        }
        this.currentTargets = newTargets;
    }
}
