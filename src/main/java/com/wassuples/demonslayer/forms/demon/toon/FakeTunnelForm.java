package com.wassuples.demonslayer.forms.demon.toon;

import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FakeTunnelForm extends ActiveForm {
    private static final double PAINT_RANGE = 12.0;

    public FakeTunnelForm(LivingEntity user, IForm formType) {
        super(user, formType);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!level.isClientSide) {
            FormAnimations.play(user, FormAnimations.TOON_TUNNEL);
            Vec3 look = user.getLookAngle().normalize();
            Vec3 textPos = user.getEyePosition().add(look.scale(1.5));

            HitResult hit = user.pick(PAINT_RANGE, 0.0f, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                    || !blockHit.getDirection().getAxis().isHorizontal()) {
                ToonSfx.spawn(level, textPos, ToonSfx.Kind.PAINT);
                ActiveFormManager.stop(user);
                return;
            }

            Direction into = blockHit.getDirection().getOpposite();
            Direction lateral = into.getClockWise();
            BlockPos origin = pickOrigin(blockHit, lateral);
            int depth = measureDepth(origin, into, lateral);

            if (depth <= 0) {
                ToonSfx.spawn(level, textPos, depth == 0 ? ToonSfx.Kind.PAINT : ToonSfx.Kind.TOO_THICK);
                level.playSound(null, user.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.6f);
                ActiveFormManager.stop(user);
                return;
            }

            ToonFakeTunnelEntity.collapseTunnelsOwnedBy(user); // only one painted tunnel at a time
            ToonFakeTunnelEntity tunnel = new ToonFakeTunnelEntity(level, user, origin, into, lateral, depth);
            level.addFreshEntity(tunnel);

            Vec3 face = tunnel.position().add(0, 1.0, 0);
            level.sendParticles(ParticleTypes.SQUID_INK, face.x, face.y, face.z, 30, 0.6, 0.6, 0.6, 0.02);
            level.playSound(null, tunnel.blockPosition(), SoundEvents.PAINTING_PLACE, SoundSource.PLAYERS, 1.0f, 1.3f);
            ToonSfx.spawn(level, face.add(0, 1.4, 0), ToonSfx.Kind.CUSTOM, "SHLAP!", -1, 20, 1.0f);
        }
        ActiveFormManager.stop(user);
    }

    /**
     * Bottom-left block of the 2x2 opening (looking into the wall). Lateral pairing follows which half of the
     * block face was clicked; vertically, a wall clicked at foot or eye level opens at the caster's feet so
     * the tunnel can be walked into.
     */
    private BlockPos pickOrigin(BlockHitResult hit, Direction lateral) {
        BlockPos hitPos = hit.getBlockPos();
        Vec3 local = hit.getLocation().subtract(Vec3.atCenterOf(hitPos));
        double lateralOffset = local.dot(Vec3.atLowerCornerOf(lateral.getNormal()));
        BlockPos column = lateralOffset >= 0.0 ? hitPos : hitPos.relative(lateral.getOpposite());

        int feet = Mth.floor(user.getY() + 0.01);
        int bottom;
        if (hitPos.getY() == feet || hitPos.getY() == feet + 1) {
            bottom = feet;
        } else {
            bottom = local.y >= 0.0 ? hitPos.getY() : hitPos.getY() - 1;
        }
        return new BlockPos(column.getX(), bottom, column.getZ());
    }

    /**
     * Number of solid layers to "tunnel" through until a fully open 2x2 layer is found behind the wall.
     *
     * @return depth >= 1, 0 if the painted spot isn't actually blocked, -1 if it's too thick or protected.
     */
    private int measureDepth(BlockPos origin, Direction into, Direction lateral) {
        for (int layer = 0; layer <= ToonFakeTunnelEntity.MAX_DEPTH; layer++) {
            List<BlockPos> cells = ToonFakeTunnelEntity.layerCells(origin, into, lateral, layer);
            boolean open = true;
            for (BlockPos pos : cells) {
                if (!level.isLoaded(pos)) return -1;
                BlockState state = level.getBlockState(pos);
                if (!state.getCollisionShape(level, pos).isEmpty()) {
                    open = false;
                    // Bedrock/barriers/end portal frames etc. and block entities (chests, spawners...) are never faked.
                    if (state.getDestroySpeed(level, pos) < 0.0f || state.hasBlockEntity()) return -1;
                }
            }
            if (open) {
                return layer; // 0 == nothing to tunnel through
            }
        }
        return -1;
    }

    @Override
    public void onTick() {
        // no-op
    }
}
