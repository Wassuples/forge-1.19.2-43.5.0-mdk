package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FakeTunnelForm extends ActiveForm {
    public FakeTunnelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_TUNNEL);
            Vec3 look = user.getLookAngle().normalize();
            ToonFakeTunnelEntity tunnel = new ToonFakeTunnelEntity(level, user, look);

            HitResult hit = user.pick(12.0, 0.0f, false);
            if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getDirection().getAxis().isHorizontal()) {
                ToonForceHelper.spawnToonText(level, user.getEyePosition().add(look.scale(1.5)),
                        Component.literal("PAINT A WALL!").withStyle(ChatFormatting.RED), 30);
                ActiveFormManager.stop(user);
                return;
            }
            Vec3 placement = new Vec3(blockHit.getLocation().x, user.getY(), blockHit.getLocation().z)
                    .add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.08));
            // Into the painted wall, so only the caster can emerge from its far side.
            tunnel.setPassageDirection(Vec3.atLowerCornerOf(blockHit.getDirection().getOpposite().getNormal()));
            tunnel.setPos(placement.x, placement.y, placement.z);
            level.addFreshEntity(tunnel);
        }
        ActiveFormManager.stop(user);
    }
    @Override
    public void onTick() {
        // no-op
    }
}
