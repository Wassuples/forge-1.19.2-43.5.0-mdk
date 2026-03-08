package com.wassuples.demonslayer.forms.slayer.love;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnHeartShape;

/**
 * Love Breathing form: Catlove Shower.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class CatloveShowerForm extends TimedForm {
    private static final int SLASH_DELAY = 5;
    private static final float LEAP_STRENGTH = 0.9f;
    private static final float DAMAGE = 18.0f;
    private static final double RADIUS = 6.0;
    private static final float KNOCKBACK = 0.7f;
    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);

    public CatloveShowerForm(LivingEntity user, IForm form) {
        super(user, form, 20);
    }

    @Override
    public void onStart() {
        user.setDeltaMovement(user.getDeltaMovement().add(0, LEAP_STRENGTH, 0));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (ticksActive == SLASH_DELAY) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.5f, 1.8f);
            dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK);

            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, user.blockPosition());
            Vec3 groundTarget = Vec3.atBottomCenterOf(groundPos);
            spawnHeartShape(level, user, groundTarget, 4.0, 100);
        }
    }
}
