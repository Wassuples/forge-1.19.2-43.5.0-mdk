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
            HitResult hit = user.pick(6.0, 0.0f, false);
            Vec3 placement = hit instanceof BlockHitResult blockHit
                    ? blockHit.getLocation().add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.06))
                    : user.position().add(look.scale(2.0));
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
