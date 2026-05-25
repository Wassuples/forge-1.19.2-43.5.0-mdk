package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.world.entity.LivingEntity;
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
            tunnel.setPos(user.getX() + look.x * 2.0, user.getY(), user.getZ() + look.z * 2.0);
            level.addFreshEntity(tunnel);
        }
        // TODO: Rebuild Fake Tunnel once wall placement, pass-through rules, and readable counterplay are designed.
        ActiveFormManager.stop(user);
    }
    @Override
    public void onTick() {
        // no-op
    }
}
