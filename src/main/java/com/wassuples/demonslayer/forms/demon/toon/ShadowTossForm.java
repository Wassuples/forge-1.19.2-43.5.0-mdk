package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShadowTossForm extends ActiveForm {
    public ShadowTossForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_SHADOW);
            AABB searchBox = user.getBoundingBox().inflate(64.0);
            ToonShadowAnchorEntity existing = level.getEntitiesOfClass(ToonShadowAnchorEntity.class, searchBox,
                    anchor -> user.getUUID().equals(anchor.getOwnerUUID()))
                    .stream().findFirst().orElse(null);

            if (existing != null) {
                Vec3 pos = existing.findSafeTeleportPosition(user);
                level.sendParticles(ParticleTypes.SQUID_INK, user.getX(), user.getY() + 0.1, user.getZ(), 24, 0.45, 0.08, 0.45, 0.0);
                if (pos != null) {
                    user.teleportTo(pos.x, pos.y, pos.z);
                    user.fallDistance = 0.0f;
                    level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.2, pos.z, 24, 0.45, 0.08, 0.45, 0.0);
                    ToonForceHelper.spawnToonText(level, pos.add(0, 1.0, 0),
                            Component.literal("SNAP!").withStyle(ChatFormatting.DARK_PURPLE), 18);
                    level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
                } else {
                    ToonForceHelper.spawnToonText(level, existing.position().add(0, 0.75, 0),
                            Component.literal("NOPE!").withStyle(ChatFormatting.DARK_GRAY), 18);
                    level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, 0.6f);
                }
                existing.discard();
            } else {
                ToonShadowAnchorEntity anchor = new ToonShadowAnchorEntity(level, user);
                anchor.setPos(user.getEyePosition());
                anchor.setDeltaMovement(user.getLookAngle().normalize().scale(2.1));
                level.addFreshEntity(anchor);
                level.sendParticles(ParticleTypes.SQUID_INK, user.getX(), user.getY() + 0.04, user.getZ(),
                        42, 0.8, 0.03, 0.8, 0.01);
                ToonForceHelper.spawnToonText(level, user.getEyePosition().add(user.getLookAngle().scale(1.1)),
                        Component.literal("TOSS!").withStyle(ChatFormatting.DARK_GRAY), 18);
                level.playSound(null, user.blockPosition(), ModSounds.TOON_BOING.get(), SoundSource.PLAYERS, 0.7f, 0.55f);
            }
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}
