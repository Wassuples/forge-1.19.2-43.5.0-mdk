package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ColdWhitePrincessesForm extends ActiveForm {
    private static final double SPAWN_DISTANCE = 4.0;

    public ColdWhitePrincessesForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.5f);
        level.playSound(null, user.blockPosition(), SoundEvents.AMBIENT_CAVE, SoundSource.PLAYERS, 1.0f, 1.8f);

        Vec3 lookDir = user.getLookAngle();
        Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

        Vec3 pos1 = user.position().add(sideDir.scale(SPAWN_DISTANCE));
        Vec3 pos2 = user.position().add(sideDir.scale(-SPAWN_DISTANCE));

        spawnPrincessAt(pos1);
        spawnPrincessAt(pos2);
        ActiveFormManager.stop(user);
    }

    private void spawnPrincessAt(Vec3 position) {
        ColdWhitePrincessEntity princess = ModEntityTypes.COLD_WHITE_PRINCESS.get().create(level);
        if (princess != null) {
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(position));
            princess.setPos(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            princess.setOwner(user);
            level.addFreshEntity(princess);
        }
    }

    @Override
    public void onTick() {
    }
}
