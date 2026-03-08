package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RimeBodhisattvaForm extends ActiveForm {
    public RimeBodhisattvaForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 1.0f);

        BodhisattvaEntity bodhisattva = ModEntityTypes.BODHISATTVA.get().create(level);
        if (bodhisattva != null) {
            // Raycast to find the furthest valid horizontal position (up to 8 blocks)
            Vec3 start = user.getEyePosition();
            Vec3 look = user.getLookAngle();
            Vec3 end = start.add(look.scale(8));
            BlockHitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));

            Vec3 targetPos = hitResult.getLocation();

            // Now find the ground directly below the target point (scanning down 10 blocks)
            // This prevents spawning in the air or inside walls, and handles indoors correctly (unlike Heightmap)
            BlockPos.MutableBlockPos searchPos = new BlockPos(targetPos).mutable();
            boolean foundGround = false;

            // Start search slightly above the hit point in case we hit the floor
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                 searchPos.set(hitResult.getBlockPos().above());
            }

            for (int i = 0; i < 15; i++) {
                BlockState state = level.getBlockState(searchPos);
                if (!state.getCollisionShape(level, searchPos).isEmpty()) {
                    foundGround = true;
                    // Found solid ground. The entity stands ON TOP of this block.
                    break;
                }
                searchPos.move(Direction.DOWN);
            }

            if (foundGround) {
                // Determine spawn Y: Top of the collision shape of the found block
                BlockPos groundBlockPos = searchPos.immutable();
                double yOffset = level.getBlockState(groundBlockPos).getCollisionShape(level, groundBlockPos).max(Direction.Axis.Y);
                bodhisattva.setPos(targetPos.x, groundBlockPos.getY() + yOffset, targetPos.z);
            } else {
                // Fallback: spawn at hit height (might be air if over void) or user pos
                bodhisattva.setPos(targetPos.x, targetPos.y, targetPos.z);
            }

            bodhisattva.setPilot(this.user);
            level.addFreshEntity(bodhisattva);

            user.startRiding(bodhisattva, true);
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1205, 5, false, false, true));
        }
    }

    @Override
    public void onStop() {
        if (user.getVehicle() instanceof BodhisattvaEntity bodhisattva) {
            bodhisattva.shatter();
        }

        user.removeEffect(MobEffects.INVISIBILITY);
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    @Override
    public void onTick() {
    }
}
