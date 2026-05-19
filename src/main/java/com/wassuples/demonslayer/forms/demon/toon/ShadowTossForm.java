package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
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
            AABB searchBox = user.getBoundingBox().inflate(64.0);
            ToonShadowAnchorEntity existing = level.getEntitiesOfClass(ToonShadowAnchorEntity.class, searchBox,
                    anchor -> user.getUUID().equals(anchor.getOwnerUUID()))
                    .stream().findFirst().orElse(null);

            if (existing != null) {
                Vec3 pos = existing.position();
                user.teleportTo(pos.x, pos.y + 0.2, pos.z);
                level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
                existing.discard();
            } else {
                ToonShadowAnchorEntity anchor = new ToonShadowAnchorEntity(level, user);
                anchor.setPos(user.getEyePosition());
                anchor.setDeltaMovement(user.getLookAngle().normalize().scale(1.2));
                level.addFreshEntity(anchor);
                level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.7f, 1.6f);
            }
        }
        ActiveFormManager.stop(user);
    }

    @Override
    public void onTick() {
        // no-op
    }
}

